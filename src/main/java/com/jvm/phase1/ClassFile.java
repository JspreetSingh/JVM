package com.jvm.phase1;

import java.util.ArrayList;
import java.util.List;

public class ClassFile {
    private final String className;
    private final String superName;
    public final ConstantPoolEntry[] constantPool;

    // store parsed methods
    public final List<MethodInfo> methods = new ArrayList<>();

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

    // helper: resolve utf8 from constant pool index
    public String getUtf8(int index) {
        if (index <= 0 || index >= constantPool.length) return null;
        ConstantPoolEntry e = constantPool[index];
        if (e == null) return null;
        return e.asUtf8();
    }

    // helper: find main method (exact name "main" and descriptor "([Ljava/lang/String;)V")
    public MethodInfo findMainMethod() {
        for (MethodInfo m : methods) {
            String name = getUtf8(m.nameIndex);
            String desc = getUtf8(m.descriptorIndex);
            if ("main".equals(name) && "([Ljava/lang/String;)V".equals(desc)) return m;
        }
        return null;
    }
}
