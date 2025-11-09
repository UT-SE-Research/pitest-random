package org.pitest.junit.adapter;

import org.junit.runner.manipulation.Filter;
import org.pitest.testapi.Configuration;
import org.pitest.testapi.TestUnit;
import org.pitest.testapi.TestUnitDescriptor;
import org.pitest.testapi.TestUnitDescriptorFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class JUnit4TestUnitDescriptorFactory implements TestUnitDescriptorFactory {

    @Override
    public List<TestUnitDescriptor> describe(Collection<TestUnit> units) {
        List<TestUnitDescriptor> result = new ArrayList<>();

        for (TestUnit u : units) {
            if (!(u instanceof AdaptedJUnitTestUnit)) {
                return Collections.emptyList();
            }

            AdaptedJUnitTestUnit aju = (AdaptedJUnitTestUnit) u;

            if (aju.getFilter().isPresent()) {
                return Collections.emptyList();
            }

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
            Optional<Filter> filter = Optional.empty();

            return new AdaptedJUnitTestUnit(clazz, filter);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to recreate JUnit4 TestUnit for " + d.getClassName(), e);
        }
    }

}
