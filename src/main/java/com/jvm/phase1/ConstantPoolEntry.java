package com.jvm.phase1;

public class ConstantPoolEntry {
    public final int tag;
    public final Object value;

    public ConstantPoolEntry(int tag, Object value) {
        this.tag = tag;
        this.value = value;
    }

    public String asUtf8() {
        return (tag == 1 && value instanceof String) ? (String) value : null;
    }

    @Override
    public String toString() {
        return "CP[tag=" + tag + ", value=" + value + "]";
    }
}
