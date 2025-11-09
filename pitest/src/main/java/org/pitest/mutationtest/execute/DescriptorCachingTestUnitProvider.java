package org.pitest.mutationtest.execute;

import org.pitest.classinfo.ClassName;
import org.pitest.testapi.Configuration;
import org.pitest.testapi.TestUnit;
import org.pitest.testapi.TestUnitDescriptor;
import org.pitest.testapi.TestUnitDescriptorFactory;
import org.pitest.testapi.execute.FindTestUnits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class DescriptorCachingTestUnitProvider {

    private final Map<ClassName, List<TestUnitDescriptor>> cache = new ConcurrentHashMap<>();

    private final Configuration config;

    public DescriptorCachingTestUnitProvider(Configuration config) {
        this.config = config;
    }

    public List<TestUnitDescriptor> getDescriptorsFor(
            Collection<ClassName> testClasses,
            ClassLoader loader) {

        Optional<TestUnitDescriptorFactory> factoryOpt = config.testUnitDescriptorFactory();

        if (!factoryOpt.isPresent()) {
            return Collections.emptyList();
        }

        TestUnitDescriptorFactory factory = factoryOpt.get();
        FindTestUnits finder = new FindTestUnits(config);

        List<TestUnitDescriptor> result = new ArrayList<>();

        for (ClassName cn : testClasses) {
            List<TestUnitDescriptor> descriptorList =
                    cache.computeIfAbsent(cn, name -> discoverAndCache(name, loader, finder, factory));
            result.addAll(descriptorList);
        }

        return result;
    }

    private List<TestUnitDescriptor> discoverAndCache(
            ClassName cn,
            ClassLoader loader,
            FindTestUnits finder,
            TestUnitDescriptorFactory factory) {

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(loader);

            Class<?> clazz = Class.forName(cn.asJavaName(), false, loader);

            List<TestUnit> units = finder.findTestUnitsForAllSuppliedClasses(
                    Collections.singletonList(clazz));

            List<TestUnitDescriptor> descriptorList = factory.describe(units);
            return Collections.unmodifiableList(descriptorList);

        } catch (Throwable t) {
            return Collections.emptyList();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

}
