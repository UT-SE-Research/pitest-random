package org.pitest.mutationtest.engine.gregor.mutators;

import org.objectweb.asm.MethodVisitor;
import org.pitest.bytecode.ASMVersion;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;

public enum DummyMutator implements MethodMutatorFactory {
    DUMMY;

    @Override
    public MethodVisitor create(MutationContext context,
                                MethodInfo methodInfo, MethodVisitor methodVisitor) {
        return new MethodVisitor(ASMVersion.ASM_VERSION, methodVisitor) {
            @Override
            public MethodVisitor getDelegate() {
                return super.getDelegate();
            }
        };
    }

    @Override
    public String getGloballyUniqueId() {
        return this.getClass().getName();
    }

    @Override
    public String getName() {
        return name();
    }

}
