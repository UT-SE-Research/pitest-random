package org.pitest.testapi;

import java.util.Collection;
import java.util.List;

public interface TestUnitDescriptorFactory {

    /**
     * Converts a list of {@code TestUnit} instances generated in the current JVM
     * into a list of serializable descriptors.
     */
    List<TestUnitDescriptor> describe(Collection<TestUnit> units);

    /**
     * Reconstructs an equivalent {@code TestUnit} in the target JVM
     * based on the given descriptor.
     */
    TestUnit create(TestUnitDescriptor descriptor,
                    ClassLoader loader,
                    Configuration config);
}
