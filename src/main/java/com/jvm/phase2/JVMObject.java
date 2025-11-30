package com.jvm.phase2;

import java.util.HashMap;
import java.util.Map;

public class JVMObject {
    public final String className;
    public final Map<String, Object> fields = new HashMap<>();

    public JVMObject(String className) {
        this.className = className;
    }

    public Object get(String fieldName) {
        return fields.get(fieldName);
    }

    public void set(String fieldName, Object value) {
        fields.put(fieldName, value);
    }

    @Override
    public String toString() {
        return "Object(" + className + ") " + fields;
    }
}
