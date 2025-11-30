package com.jvm.phase3;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal object representation: className + field map.
 * Fields are stored by their simple name (as string). Values are either Integer or String or object-ref Integer.
 */
public class JavaObject {
    public final String className;
    public final Map<String, Object> fields = new HashMap<>();

    public JavaObject(String className) {
        this.className = className;
    }

    public Object getField(String name) {
        return fields.get(name);
    }

    public void setField(String name, Object value) {
        fields.put(name, value);
    }
}
