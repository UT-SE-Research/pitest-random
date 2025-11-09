package org.pitest.junit5;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.pitest.testapi.TestUnit;
import org.pitest.testapi.TestUnitExecutionListener;
import org.pitest.testapi.TestUnitFinder;

public class JUnit5TestUnitFinder implements TestUnitFinder {

    private final Launcher launcher;

    public JUnit5TestUnitFinder() {
        this.launcher = LauncherFactory.create();
    }

    @Override
    public List<TestUnit> findTestUnits(Class<?> clazz,
                                        TestUnitExecutionListener listener) {
        if (clazz.getEnclosingClass() != null) {
            return emptyList();
        }

        TestPlan testPlan = launcher.discover(
                LauncherDiscoveryRequestBuilder
                        .request()
                        .selectors(DiscoverySelectors.selectClass(clazz))
                        .build()
        );

        return testPlan.getRoots()
                .stream()
                .map(testPlan::getDescendants)
                .flatMap(Set::stream)
                .filter(testIdentifier -> testIdentifier.getSource().isPresent())
                .filter(testIdentifier -> testIdentifier.getSource().get() instanceof MethodSource)
                .map(testIdentifier -> new JUnit5TestUnit(clazz, testIdentifier))
                .collect(toList());
    }

}
