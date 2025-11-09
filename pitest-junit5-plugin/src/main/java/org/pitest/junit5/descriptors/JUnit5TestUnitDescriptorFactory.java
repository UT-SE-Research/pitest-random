package org.pitest.junit5.descriptors;

import org.pitest.junit5.JUnit5TestUnit;
import org.pitest.testapi.Configuration;
import org.pitest.testapi.TestUnit;
import org.pitest.testapi.TestUnitDescriptor;
import org.pitest.testapi.TestUnitDescriptorFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JUnit5TestUnitDescriptorFactory implements TestUnitDescriptorFactory {

    @Override
    public List<TestUnitDescriptor> describe(Collection<TestUnit> units) {
        List<TestUnitDescriptor> result = new ArrayList<>();
        for (TestUnit u : units) {
            if (u instanceof JUnit5TestUnit) {
                JUnit5TestUnit j5 = (JUnit5TestUnit) u;
                result.add(new JUnit5TestUnitDescriptor(
                        j5.getTestClass().getName(),
                        j5.getUniqueId(),
                        j5.getDisplayName()
                ));
            }
        }
        return result;
    }

    @Override
    public TestUnit create(TestUnitDescriptor descriptor,
                           ClassLoader loader,
                           Configuration config) {
        if (!(descriptor instanceof JUnit5TestUnitDescriptor)) {
            throw new IllegalArgumentException(
                    "Unsupported descriptor type: " + descriptor.getClass());
        }
        JUnit5TestUnitDescriptor d = (JUnit5TestUnitDescriptor) descriptor;
        try {
            Class<?> clazz = Class.forName(d.getClassName(), false, loader);
            return new JUnit5TestUnit(clazz, d.getUniqueId(), d.getDisplayName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load test class " + d.getClassName(), e);
        }
    }
}
