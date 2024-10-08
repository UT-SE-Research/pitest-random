/*
 * Copyright 2010 Henry Coles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.mutationtest.execute;

import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.DetectionStatus;
import org.pitest.mutationtest.MutationStatusTestPair;
import org.pitest.mutationtest.engine.gregor.GregorMutater;
import org.pitest.mutationtest.environment.ResetEnvironment;
import org.pitest.mutationtest.engine.Mutant;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.mocksupport.JavassistInterceptor;
import org.pitest.testapi.Description;
import org.pitest.testapi.TestResult;
import org.pitest.testapi.TestUnit;
import org.pitest.testapi.execute.Container;
import org.pitest.testapi.execute.ExitingResultCollector;
import org.pitest.testapi.execute.MultipleTestGroup;
import org.pitest.testapi.execute.Pitest;
import org.pitest.testapi.execute.containers.ConcreteResultCollector;
import org.pitest.testapi.execute.containers.UnContainer;
import org.pitest.util.Log;
import org.pitest.util.Verbosity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.pitest.util.Unchecked.translateCheckedException;

public class MutationTestWorker {

    private static final Logger LOG = Log
            .getLogger();

    // micro optimise debug logging
    private static final boolean DEBUG = LOG
            .isLoggable(Level.FINE);

    private final Mutater mutater;
    private final ClassLoader loader;
    private final HotSwap hotswap;
    private final boolean fullMutationMatrix;

    private final ResetEnvironment reset;

    private final boolean randomGroup;

    private ClassName lastClassName = null;

    private byte[] lastClassBytes = null;

    public MutationTestWorker(HotSwap hotswap,
                              Mutater mutater,
                              ClassLoader loader,
                              ResetEnvironment reset,
                              boolean fullMutationMatrix,
                              boolean randomGroup) {
        this.loader = loader;
        this.reset = reset;
        this.mutater = mutater;
        this.hotswap = hotswap;
        this.fullMutationMatrix = fullMutationMatrix;
        this.randomGroup = randomGroup;

    }

    protected void run(final Collection<MutationDetails> range, final Reporter r,
                       final TimeOutDecoratedTestSource testSource) throws IOException {

        for (final MutationDetails mutation : range) {
            if (DEBUG) {
                LOG.fine("Running mutation " + mutation);
            }
            final long t0 = System.nanoTime();
            processMutation(r, testSource, mutation);
            if (DEBUG) {
                LOG.fine("processed mutation in " + NANOSECONDS.toMillis(System.nanoTime() - t0)
                        + " ms.");
            }
        }

    }

    private void processMutation(final Reporter r,
                                 final TimeOutDecoratedTestSource testSource,
                                 final MutationDetails mutationDetails) {

        final MutationIdentifier mutationId = mutationDetails.getId();
        final Mutant mutatedClass = this.mutater.getMutation(mutationId);

        byte[] originalClassBytes = null;
        if (Log.verbosity() == Verbosity.RANDOM_VERBOSE) {
            if (this.mutater instanceof GregorMutater) {
                GregorMutater gregorMutater = (GregorMutater) (this.mutater);
                originalClassBytes = gregorMutater.getOriginalClassBytes(mutationId.getClassName());
            }
        }

        // For the benefit of mocking frameworks such as PowerMock
        // mess with the internals of Javassist so our mutated class
        // bytes are returned
        JavassistInterceptor.setMutant(mutatedClass);
        reset.resetFor(mutatedClass);

        if (DEBUG) {
            LOG.fine("mutating method " + mutatedClass.getDetails().getMethod());
        }
        final List<TestUnit> relevantTests = testSource
                .translateTests(mutationDetails.getTestsInOrder());

        r.describe(mutationId);
        final MutationStatusTestPair mutationDetected;
        if (!randomGroup) {
            mutationDetected = handleMutation(
                    mutationDetails, mutatedClass, relevantTests);
        } else {
            mutationDetected = handleMutationWithPotentialDifferentClass(mutationDetails, mutatedClass, relevantTests, originalClassBytes);
        }
        r.report(mutationId, mutationDetected);
        if (DEBUG) {
            LOG.fine("Mutation " + mutationId + " detected = " + mutationDetected);
        }
    }

    private MutationStatusTestPair handleMutationWithPotentialDifferentClass(
            final MutationDetails mutationId, final Mutant mutatedClass,
            final List<TestUnit> relevantTests, final byte[] originalClassBytes) {
        final MutationStatusTestPair mutationDetected;
        if ((relevantTests == null) || relevantTests.isEmpty()) {
            LOG.info(() -> "No test coverage for mutation " + mutationId + " in "
                    + mutatedClass.getDetails().getMethod());
            mutationDetected = MutationStatusTestPair.notAnalysed(0, DetectionStatus.RUN_ERROR);
        } else {
            mutationDetected = handleCoveredMutationWithPotentialDifferentClass(mutationId, mutatedClass,
                    relevantTests, originalClassBytes);

        }
        return mutationDetected;
    }

    private MutationStatusTestPair handleCoveredMutationWithPotentialDifferentClass(
            final MutationDetails mutationId, final Mutant mutatedClass,
            final List<TestUnit> relevantTests, final byte[] originalClassBytes
    ) {
        final MutationStatusTestPair mutationDetected;
        if (DEBUG) {
            LOG.fine("" + relevantTests.size() + " relevant test for "
                    + mutatedClass.getDetails().getMethod());
        }

        if (Log.verbosity() == Verbosity.RANDOM_VERBOSE) {
            LOG.info("RANDOM LOG: start running " + +relevantTests.size() + " relevant test for " + mutationId.toString());
        }

        final long t = System.nanoTime();

        if (lastClassName != null && !mutationId.getClassName().equals(lastClassName)) {
            if (!this.hotswap.insertClass(lastClassName, this.loader, lastClassBytes)) {
                LOG.warning("Mutation " + mutationId + " was not viable ");
                mutationDetected = MutationStatusTestPair.notAnalysed(0,
                        DetectionStatus.NON_VIABLE);
                return mutationDetected;
            }
            LOG.info("RANDOM LOG: Mutated Class changed, replaced last mutant to original class " + lastClassName.asInternalName() + " in " + NANOSECONDS.toMillis(System.nanoTime() - t) + " ms");
            JavassistInterceptor.setBytesAndName(lastClassBytes, lastClassName);
            reset.resetFor(mutatedClass);
        }


        final Container c = createNewContainer();
        final long t0 = System.nanoTime();


        if (this.hotswap.insertClass(mutationId.getClassName(), this.loader,
                mutatedClass.getBytes())) {
            lastClassName = mutationId.getClassName();
            lastClassBytes = originalClassBytes;
            if (DEBUG) {
                LOG.fine("replaced class with mutant in "
                        + NANOSECONDS.toMillis(System.nanoTime() - t0) + " ms");
            }
            if (Log.verbosity() == Verbosity.RANDOM_VERBOSE) {
                LOG.info("RANDOM LOG: replaced class with mutant in " + NANOSECONDS.toMillis(System.nanoTime() - t0) + " ms");
            }
            final long t1 = System.nanoTime();
            mutationDetected = doTestsDetectMutation(c, relevantTests);
            if (Log.verbosity() == Verbosity.RANDOM_VERBOSE) {
                LOG.info("RANDOM LOG: run all related tests in " + NANOSECONDS.toMillis(System.nanoTime() - t1) + " ms");
            }
        } else {
            LOG.warning("Mutation " + mutationId + " was not viable ");
            mutationDetected = MutationStatusTestPair.notAnalysed(0,
                    DetectionStatus.NON_VIABLE);
        }
        return mutationDetected;
    }

    private MutationStatusTestPair handleMutation(
            final MutationDetails mutationId, final Mutant mutatedClass,
            final List<TestUnit> relevantTests) {
        final MutationStatusTestPair mutationDetected;
        if ((relevantTests == null) || relevantTests.isEmpty()) {
            LOG.info(() -> "No test coverage for mutation " + mutationId + " in "
                    + mutatedClass.getDetails().getMethod());
            mutationDetected = MutationStatusTestPair.notAnalysed(0, DetectionStatus.RUN_ERROR);
        } else {
            mutationDetected = handleCoveredMutation(mutationId, mutatedClass,
                    relevantTests);

        }
        return mutationDetected;
    }

    private MutationStatusTestPair handleCoveredMutation(
            final MutationDetails mutationId, final Mutant mutatedClass,
            final List<TestUnit> relevantTests) {
        final MutationStatusTestPair mutationDetected;
        if (DEBUG) {
            LOG.fine("" + relevantTests.size() + " relevant test for "
                    + mutatedClass.getDetails().getMethod());
        }

        if (Log.verbosity() == Verbosity.RANDOM_VERBOSE) {
            LOG.info("RANDOM LOG: start running " + +relevantTests.size() + " relevant test for " + mutationId.toString());
        }

        final Container c = createNewContainer();
        final long t0 = System.nanoTime();

        if (this.hotswap.insertClass(mutationId.getClassName(), this.loader,
                mutatedClass.getBytes())) {
            if (DEBUG) {
                LOG.fine("replaced class with mutant in "
                        + NANOSECONDS.toMillis(System.nanoTime() - t0) + " ms");
            }
            if (Log.verbosity() == Verbosity.RANDOM_VERBOSE) {
                LOG.info("RANDOM LOG: replaced class with mutant in " + NANOSECONDS.toMillis(System.nanoTime() - t0) + " ms");
            }
            final long t1 = System.nanoTime();
            mutationDetected = doTestsDetectMutation(c, relevantTests);
            if (Log.verbosity() == Verbosity.RANDOM_VERBOSE) {
                LOG.info("RANDOM LOG: run all related tests in " + NANOSECONDS.toMillis(System.nanoTime() - t1) + " ms");
            }
        } else {
            LOG.warning("Mutation " + mutationId + " was not viable ");
            mutationDetected = MutationStatusTestPair.notAnalysed(0,
                    DetectionStatus.NON_VIABLE);
        }
        return mutationDetected;
    }

    private static Container createNewContainer() {
        return new UnContainer() {
            @Override
            public List<TestResult> execute(final TestUnit group) {
                final Collection<TestResult> results = new ConcurrentLinkedDeque<>();
                final ExitingResultCollector rc = new ExitingResultCollector(
                        new ConcreteResultCollector(results));
                group.execute(rc);
                return new ArrayList<>(results);
            }
        };
    }


    @Override
    public String toString() {
        return "MutationTestWorker [mutater=" + this.mutater + ", loader="
                + this.loader + ", hotswap=" + this.hotswap + "]";
    }

    private MutationStatusTestPair doTestsDetectMutation(final Container c,
                                                         final List<TestUnit> tests) {
        try {
            final CheckTestHasFailedResultListener listener = new CheckTestHasFailedResultListener(fullMutationMatrix);

            final Pitest pit = new Pitest(listener);

            if (this.fullMutationMatrix) {
                pit.run(c, tests);
            } else {
                pit.run(c, createEarlyExitTestGroup(tests));
            }

            return createStatusTestPair(listener);
        } catch (final Exception ex) {
            throw translateCheckedException(ex);
        }

    }

    private MutationStatusTestPair createStatusTestPair(
            final CheckTestHasFailedResultListener listener) {
        List<String> failingTests = listener.getFailingTests().stream()
                .map(Description::getQualifiedName).collect(Collectors.toList());
        List<String> succeedingTests = listener.getSucceedingTests().stream()
                .map(Description::getQualifiedName).collect(Collectors.toList());

        return new MutationStatusTestPair(listener.getNumberOfTestsRun(),
                listener.status(), failingTests, succeedingTests);
    }

    private List<TestUnit> createEarlyExitTestGroup(final List<TestUnit> tests) {
        return Collections.singletonList(new MultipleTestGroup(tests));
    }

}
