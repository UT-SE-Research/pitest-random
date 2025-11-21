package org.pitest.testapi.execute;

import org.pitest.testapi.AbstractTestUnit;
import org.pitest.testapi.Description;
import org.pitest.testapi.ResultCollector;
import org.pitest.testapi.TestUnit;

public final class NamedTestUnit extends AbstractTestUnit {

    private final TestUnit delegate;

    public NamedTestUnit(TestUnit delegate, String displayName) {
        super(new Description(
                displayName,
                delegate.getDescription().getFirstTestClass()
        ));
        this.delegate = delegate;
    }

    @Override
    public void execute(ResultCollector rc) {
        delegate.execute(rc);
    }

    @Override
    public String toString() {
        return "NamedTestUnit[" + getDescription() + ", delegate=" + delegate + "]";
    }
}
