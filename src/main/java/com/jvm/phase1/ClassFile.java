package com.jvm.phase1;

public class ClassFile {
    private final String className;
    private final String superName;
    private final ConstantPoolEntry[] constantPool;

    public ClassFile(String className, String superName, ConstantPoolEntry[] constantPool) {
        this.className = className;
        this.superName = superName;
        this.constantPool = constantPool;
    }

    public String getClassName() {
        return className;
    }

    public String getSuperName() {
        return superName;
    }

    public String getUtf8(int index) {
        if (index <= 0 || index >= constantPool.length)
            return null;
        return constantPool[index].asUtf8();
    }
}
