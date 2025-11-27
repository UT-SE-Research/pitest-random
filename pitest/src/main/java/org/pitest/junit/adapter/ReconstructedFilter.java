package org.pitest.junit.adapter;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * 根据一个 desc 字符串重建出等价语义的 Filter：
 * - 不含 [ ] ：行为等价于 org.pitest.junit.DescriptionFilter
 * - 含 [ ]   ：行为等价于 org.pitest.junit.ParameterisedTestFilter
 */
final class ReconstructedFilter extends Filter {

    private final String desc;
    private final String parent;

    ReconstructedFilter(String desc) {
        this.desc = desc;
        if (desc.contains("[") && desc.contains("]")) {
            this.parent = desc.substring(desc.indexOf('['), desc.indexOf(']') + 1);
        } else {
            this.parent = null;
        }
    }

    @Override
    public String describe() {
        return this.desc;
    }

    @Override
    public boolean shouldRun(Description description) {
        String s = description.toString();
        if (s.equals(this.desc)) {
            return true;
        }
        return s.equals(this.parent);
    }

    @Override
    public String toString() {
        return describe();
    }
}
