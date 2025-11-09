package org.pitest.junit5.descriptors;

import org.pitest.testapi.TestUnitDescriptor;

public class JUnit5TestUnitDescriptor implements TestUnitDescriptor {

    private static final long serialVersionUID = 1L;

    private final String className;
    private final String uniqueId;
    private final String displayName;

    public JUnit5TestUnitDescriptor(String className, String uniqueId, String displayName) {
        this.className = className;
        this.uniqueId = uniqueId;
        this.displayName = displayName;
    }

    public String getClassName() {
        return className;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return "JUnit5TestUnitDescriptor[" + className + "," + displayName + "," + uniqueId + "]";
    }
}
