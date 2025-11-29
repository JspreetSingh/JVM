package com.jvm.phase1;

import com.jvm.phase2.CodeAttribute;
import java.util.ArrayList;
import java.util.List;

public class MethodInfo {

    // raw indices
    public int accessFlags;
    public int nameIndex;
    public int descriptorIndex;

    // decoded names (fill these during parsing)
    public String name;
    public String descriptor;

    // attributes list (CodeAttribute is one of them)
    public final List<Object> attributes = new ArrayList<>();

    // directly stored values from CodeAttribute:
    public int maxStack;
    public int maxLocals;
    public byte[] code;

    /** Return CodeAttribute if attached */
    public CodeAttribute getCodeAttribute() {
        for (Object a : attributes) {
            if (a instanceof CodeAttribute) return (CodeAttribute) a;
        }
        return null;
    }

    @Override
    public String toString() {
        return "MethodInfo{name=" + name + ", descriptor=" + descriptor +
                ", maxStack=" + maxStack + ", maxLocals=" + maxLocals +
                ", codeLength=" + (code == null ? 0 : code.length) + "}";
    }
}
