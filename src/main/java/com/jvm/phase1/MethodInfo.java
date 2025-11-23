package com.jvm.phase1;

import com.jvm.phase2.CodeAttribute;
import java.util.ArrayList;
import java.util.List;

public class MethodInfo {
    public int accessFlags;
    public int nameIndex;
    public int descriptorIndex;
    // attributes can include CodeAttribute
    public final List<Object> attributes = new ArrayList<>();

    public CodeAttribute getCodeAttribute() {
        for (Object a : attributes) {
            if (a instanceof CodeAttribute) return (CodeAttribute) a;
        }
        return null;
    }
}
