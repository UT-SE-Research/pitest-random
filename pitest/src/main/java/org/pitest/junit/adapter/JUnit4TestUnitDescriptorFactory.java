package org.pitest.junit.adapter;

import org.pitest.testapi.Configuration;
import org.pitest.testapi.TestUnit;
import org.pitest.testapi.TestUnitDescriptor;
import org.pitest.testapi.TestUnitDescriptorFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.pitest.util.Log.getLogger;

public final class JUnit4TestUnitDescriptorFactory implements TestUnitDescriptorFactory {

    private static final Logger LOG = getLogger();

    @Override
    public List<TestUnitDescriptor> describe(Collection<TestUnit> units) {
        List<TestUnitDescriptor> result = new ArrayList<>();

        for (TestUnit u : units) {
            if (!(u instanceof AdaptedJUnitTestUnit)) {
                return Collections.emptyList();
            }

            AdaptedJUnitTestUnit aju = (AdaptedJUnitTestUnit) u;

            String className = aju.getTestClass().getName();
            result.add(new JUnit4TestUnitDescriptor(className));
        }

        return result;
    }

    @Override
    public TestUnit create(TestUnitDescriptor descriptor,
                           ClassLoader loader,
                           Configuration config) {
        JUnit4TestUnitDescriptor d = (JUnit4TestUnitDescriptor) descriptor;
        try {
            Class<?> clazz = Class.forName(d.getClassName(), false, loader);
            return new AdaptedJUnitTestUnit(clazz, Optional.empty());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Failed to recreate JUnit4 TestUnit for " + d.getClassName(), e
            );
        }
    }

}
