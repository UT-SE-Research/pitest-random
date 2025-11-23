package org.pitest.junit5;

import java.util.Optional;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.pitest.testapi.AbstractTestUnit;
import org.pitest.testapi.Description;
import org.pitest.testapi.ResultCollector;

public class JUnit5TestUnit extends AbstractTestUnit {

    private final Class<?> testClass;

    private final String uniqueId;
    private final String displayName;

    public JUnit5TestUnit(Class<?> testClass, TestIdentifier testIdentifier) {
        this(testClass, testIdentifier.getUniqueId(), testIdentifier.getDisplayName());
    }

    public JUnit5TestUnit(Class<?> testClass, String uniqueId, String displayName) {
        super(new Description(uniqueId, testClass));
        this.testClass = testClass;
        this.uniqueId = uniqueId;
        this.displayName = displayName;
    }

    public Class<?> getTestClass() {
        return testClass;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void execute(ResultCollector resultCollector) {
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder
                .request()
                .selectors(DiscoverySelectors.selectUniqueId(uniqueId))
                .build();

        launcher.registerTestExecutionListeners(new TestExecutionListener() {

            @Override
            public void executionSkipped(TestIdentifier testIdentifier, String reason) {
                testIdentifier.getSource().ifPresent(testSource -> {
                    if (testSource instanceof MethodSource) {
                        resultCollector.notifySkipped(
                                new Description(uniqueId, testClass));
                    }
                });
            }

            @Override
            public void executionStarted(TestIdentifier testIdentifier) {
                testIdentifier.getSource().ifPresent(testSource -> {
                    if (testSource instanceof MethodSource) {
                        resultCollector.notifyStart(
                                new Description(uniqueId, testClass));
                    }
                });
            }

            @Override
            public void executionFinished(TestIdentifier testIdentifier,
                                          TestExecutionResult testExecutionResult) {
                testIdentifier.getSource().ifPresent(testSource -> {
                    if (testSource instanceof MethodSource) {
                        Optional<Throwable> throwable = testExecutionResult.getThrowable();
                        Description desc = new Description(uniqueId, testClass);

                        if (TestExecutionResult.Status.ABORTED == testExecutionResult.getStatus()) {
                            resultCollector.notifyEnd(desc);
                        } else if (throwable.isPresent()) {
                            resultCollector.notifyEnd(desc, throwable.get());
                        } else {
                            resultCollector.notifyEnd(desc);
                        }
                    }
                });
            }

        });
        launcher.execute(launcherDiscoveryRequest);
    }

}
