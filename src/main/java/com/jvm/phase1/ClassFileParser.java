package com.jvm.phase1;

import com.jvm.phase2.CodeAttribute;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class ClassFileParser {

    // Main parse entry - simplified around previously working parser
    public ClassFile parse(String filePath) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(filePath))) {
            int magic = in.readInt();
            if (magic != 0xCAFEBABE) throw new IOException("Invalid class file");

            int minor = in.readUnsignedShort();
            int major = in.readUnsignedShort();
            System.out.println("Java Class Version: " + major + "." + minor);

            int cpCount = in.readUnsignedShort();
            ConstantPoolEntry[] constantPool = new ConstantPoolEntry[cpCount];

            // read simple constant pool entries (utf8, class, nameandtype, refs, string)
            for (int i = 1; i < cpCount; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1: // UTF8
                        constantPool[i] = new ConstantPoolEntry(tag, in.readUTF());
                        break;
                    case 7: // Class
                    case 8: // String
                        constantPool[i] = new ConstantPoolEntry(tag, in.readUnsignedShort());
                        break;
                    case 9: case 10: case 11: case 12:
                        // store two-shorts as int array
                        int a = in.readUnsignedShort();
                        int b = in.readUnsignedShort();
                        constantPool[i] = new ConstantPoolEntry(tag, new int[]{a, b});
                        break;
                    case 3: case 4:
                        in.readInt();
                        break;
                    case 5: case 6:
                        in.readLong();
                        i++; // long/double take two slots
                        break;
                    default:
                        throw new IOException("Unsupported CP tag: " + tag);
                }
            }

            int accessFlags = in.readUnsignedShort();
            int thisClass = in.readUnsignedShort();
            int superClass = in.readUnsignedShort();

            // resolve class names
            int nameIndex = (Integer) constantPool[thisClass].value;
            String className = ((String) constantPool[nameIndex].value);
            int superNameIndex = (Integer) constantPool[superClass].value;
            String superName = ((String) constantPool[superNameIndex].value);

            ClassFile cf = new ClassFile(className, superName, constantPool);

            // interfaces
            int interfacesCount = in.readUnsignedShort();
            for (int i = 0; i < interfacesCount; i++) in.readUnsignedShort();

            // fields - skip
            int fieldsCount = in.readUnsignedShort();
            for (int i = 0; i < fieldsCount; i++) {
                in.readUnsignedShort(); // access
                in.readUnsignedShort(); // name_index
                in.readUnsignedShort(); // descriptor_index
                int attrs = in.readUnsignedShort();
                for (int j = 0; j < attrs; j++) {
                    int ai = in.readUnsignedShort();
                    int alen = in.readInt();
                    in.skipBytes(alen);
                }
            }

            // *** METHODS: parse and attach Code attributes ***
            int methodsCount = in.readUnsignedShort();
            System.out.println("Methods Count: " + methodsCount);
            for (int i = 0; i < methodsCount; i++) {
                MethodInfo m = new MethodInfo();
                m.accessFlags = in.readUnsignedShort();
                m.nameIndex = in.readUnsignedShort();
                m.descriptorIndex = in.readUnsignedShort();
                int attrsCount = in.readUnsignedShort();
                for (int j = 0; j < attrsCount; j++) {
                    int attrNameIndex = in.readUnsignedShort();
                    String attrName = cf.getUtf8(attrNameIndex);
                    int attrLen = in.readInt();
                    if ("Code".equals(attrName)) {
                        CodeAttribute codeAttr = CodeAttribute.read(in, constantPool, attrLen);
                        m.attributes.add(codeAttr);
                    } else {
                        // skip other attributes
                        in.skipBytes(attrLen);
                    }
                }
                cf.methods.add(m);
            }

            // class attributes - skip
            int classAttrs = in.readUnsignedShort();
            for (int i = 0; i < classAttrs; i++) {
                int ai = in.readUnsignedShort();
                int alen = in.readInt();
                in.skipBytes(alen);
            }

            // debug print methods and code lengths
            for (MethodInfo mm : cf.methods) {
                String name = cf.getUtf8(mm.nameIndex);
                System.out.println("  Method: " + name);
                CodeAttribute ca = mm.getCodeAttribute();
                if (ca != null) {
                    System.out.println("    MaxStack=" + ca.maxStack + " MaxLocals=" + ca.maxLocals + " CodeLength=" + ca.code.length);
                }
            }

            return cf;
        }
    }
}
