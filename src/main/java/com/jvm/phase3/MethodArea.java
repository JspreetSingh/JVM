package com.jvm.phase3;

import com.jvm.phase1.ClassFile;

import java.util.HashMap;
import java.util.Map;

public class MethodArea {
    private final Map<String, ClassFile> classes = new HashMap<>();

    public void load(ClassFile cf) {
        classes.put(cf.getClassName(), cf);
    }

    public ClassFile get(String name) {
        return classes.get(name);
    }
}
