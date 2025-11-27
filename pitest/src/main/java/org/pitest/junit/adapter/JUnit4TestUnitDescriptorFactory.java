package org.pitest.junit.adapter;

import org.junit.runner.manipulation.Filter;
import org.pitest.junit.DescriptionFilter;
import org.pitest.junit.ParameterisedTestFilter;
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

            String filterDesc = null;
            if (aju.getFilter().isPresent()) {
                Filter f = aju.getFilter().get();

                if (f instanceof DescriptionFilter || f instanceof ParameterisedTestFilter) {
                    filterDesc = f.describe();
                } else {
                    LOG.fine("JUnit4 descriptor factory: unknown Filter type "
                            + f.getClass() + ", falling back to default behaviour.");
                    return Collections.emptyList();
                }
            }

            result.add(new JUnit4TestUnitDescriptor(className, filterDesc));
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

            Optional<Filter> filter;
            String fd = d.getFilterDescription();
            if (fd == null) {
                filter = Optional.empty();
            } else {
                filter = Optional.of(new ReconstructedFilter(fd));
            }

            return new AdaptedJUnitTestUnit(clazz, filter);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Failed to recreate JUnit4 TestUnit for " + d.getClassName(), e
            );
        }
    }

}
