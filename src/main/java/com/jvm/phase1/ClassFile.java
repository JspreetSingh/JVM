package com.jvm.phase1; // change to your package if different

import java.util.ArrayList;
import java.util.List;

public class ClassFile {
    private final String className;
    private final String superName;
    public final ConstantPoolEntry[] constantPool;
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

    public String getUtf8(int index) {
        if (index <= 0 || index >= constantPool.length) return null;
        ConstantPoolEntry e = constantPool[index];
        if (e == null) return null;
        return e.asUtf8();
    }

    /**
     * Resolve a constant pool entry used by ldc or field/method refs.
     * For String constants it returns a java.lang.String.
     * For Integer/Float constants this simplistic resolver does not return numeric values — extend later as needed.
     */
    public Object getConstant(int index) {
        if (index <= 0 || index >= constantPool.length) return null;
        ConstantPoolEntry e = constantPool[index];
        if (e == null) return null;
        int tag = e.tag;
        if (tag == 1) { // UTF8
            return e.asUtf8();
        } else if (tag == 8) { // String: value is index to utf8
            Integer utfIndex = (Integer) e.value;
            ConstantPoolEntry u = constantPool[utfIndex];
            if (u != null && u.tag == 1) return u.asUtf8();
            return null;
        } else if (tag == 3) { // Integer
            // If you stored raw int somewhere you'll need to adapt; for now return e.value
            return e.value;
        } else {
            // For other tags return the raw stored value (int[] etc.) so caller can interpret
            return e.value;
        }
    }

    /**
     * Convenience: resolve a Fieldref/Methodref entry at index to a pair:
     * returns an int[] {classIndex, nameAndTypeIndex} or null if not applicable.
     */
    public int[] getRefIndexes(int index) {
        if (index <= 0 || index >= constantPool.length) return null;
        ConstantPoolEntry e = constantPool[index];
        if (e == null) return null;
        if (e.tag == 9 || e.tag == 10 || e.tag == 11) {
            return (int[]) e.value; // {classIndex, nameAndTypeIndex}
        }
        return null;
    }

    /**
     * Resolve NameAndType at index to String[] {name, descriptor}
     */
    public String[] getNameAndType(int index) {
        if (index <= 0 || index >= constantPool.length) return null;
        ConstantPoolEntry e = constantPool[index];
        if (e == null || e.tag != 12) return null;
        int[] arr = (int[]) e.value; // {nameIndex, descriptorIndex}
        String name = getUtf8(arr[0]);
        String desc = getUtf8(arr[1]);
        return new String[] { name, desc };
    }

    /**
     * Resolve Class entry's name (internal name like java/lang/System)
     */
    public String getClassNameFromCp(int classIndex) {
        if (classIndex <= 0 || classIndex >= constantPool.length) return null;
        ConstantPoolEntry e = constantPool[classIndex];
        if (e == null || e.tag != 7) return null;
        Integer nameIndex = (Integer) e.value;
        return getUtf8(nameIndex);
    }

    // find main method by signature
    public MethodInfo findMainMethod() {
        for (MethodInfo m : methods) {
            String name = getUtf8(m.nameIndex);
            String desc = getUtf8(m.descriptorIndex);
            if ("main".equals(name) && "([Ljava/lang/String;)V".equals(desc)) return m;
        }
        return null;
    }

    // find a method by name and descriptor
    public MethodInfo findMethod(String name, String descriptor) {
        for (MethodInfo m : methods) {
            if (name.equals(m.name) && descriptor.equals(m.descriptor)) return m;
        }
        return null;
    }

    // Resolve a CONSTANT_Methodref entry → MethodInfo
    public MethodInfo resolveMethod(int cpIndex) {
        // cpIndex points to a Methodref entry (tag 10)
        ConstantPoolEntry ref = constantPool[cpIndex];
        if (ref == null || ref.tag != 10)
            throw new RuntimeException("Not a Methodref at cp index: " + cpIndex);

        int[] arr = (int[]) ref.value;  // {classIndex, nameAndTypeIndex}
        int classIndex = arr[0];
        int natIndex = arr[1];

        // class name
        String clazz = getClassNameFromCp(classIndex);

        // method name + descriptor
        String[] nat = getNameAndType(natIndex);
        String methodName = nat[0];
        String descriptor = nat[1];

        // Only supporting methods inside the same class for now
        if (!clazz.equals(this.getClassName())) {
            throw new RuntimeException("Cross-class method invocation not implemented: " + clazz);
        }

        // find the method
        MethodInfo m = findMethod(methodName, descriptor);
        if (m == null) {
            throw new RuntimeException("Method not found: " + methodName + descriptor);
        }

        return m;
    }

    public final List<FieldInfo> fields = new ArrayList<>();

    public FieldInfo findField(String name) {
        for (FieldInfo f : fields) {
            if (f.name.equals(name)) return f;
        }
        return null;
    }


}
