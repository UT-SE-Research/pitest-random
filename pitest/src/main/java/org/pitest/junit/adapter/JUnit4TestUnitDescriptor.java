package org.pitest.junit.adapter;

import org.pitest.testapi.TestUnitDescriptor;

public final class JUnit4TestUnitDescriptor implements TestUnitDescriptor {

    private static final long serialVersionUID = 1L;

    private final String className;

    private final String filterDescription;

    public JUnit4TestUnitDescriptor(String className, String filterDescription) {
        this.className = className;
        this.filterDescription = filterDescription;
    }

    public String getClassName() {
        return className;
    }

    public String getFilterDescription() {
        return filterDescription;
    }

    @Override
    public String toString() {
        return "JUnit4TestUnitDescriptor[" + className
                + (filterDescription != null ? ("," + filterDescription) : "")
                + "]";
    }
}
