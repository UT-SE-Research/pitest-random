package org.pitest.junit.adapter;

import org.pitest.testapi.TestUnitDescriptor;

public final class JUnit4TestUnitDescriptor implements TestUnitDescriptor {

    private static final long serialVersionUID = 1L;

    private final String className;

    public JUnit4TestUnitDescriptor(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return "JUnit4TestUnitDescriptor[" + className + "]";
    }
}
