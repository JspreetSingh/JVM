package com.jvm.phase1;

public class ConstantPoolEntry {
    public final int tag;
    public final Object value;

    public ConstantPoolEntry(int tag, Object value) {
        this.tag = tag;
        this.value = value;
    }

    // Helper to return UTF8 string
    public String asUtf8() {
        if (tag == 1) { // UTF-8
            return (String) value;
        }
        return null;
    }

    // ---- Static factory methods for readability ----
    public static ConstantPoolEntry utf8(String s) {
        return new ConstantPoolEntry(1, s);
    }

    public static ConstantPoolEntry integer(int v) {
        return new ConstantPoolEntry(3, v);
    }

    public static ConstantPoolEntry string(int utf8Index) {
        return new ConstantPoolEntry(8, utf8Index);
    }

    public static ConstantPoolEntry classRef(int nameIndex) {
        return new ConstantPoolEntry(7, nameIndex);
    }

    public static ConstantPoolEntry nameAndType(int nameIndex, int descIndex) {
        return new ConstantPoolEntry(12, new int[]{nameIndex, descIndex});
    }

    public static ConstantPoolEntry fieldRef(int classIndex, int ntIndex) {
        return new ConstantPoolEntry(9, new int[]{classIndex, ntIndex});
    }

    public static ConstantPoolEntry methodRef(int classIndex, int ntIndex) {
        return new ConstantPoolEntry(10, new int[]{classIndex, ntIndex});
    }
}
