package com.jvm.phase1;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class ClassFileParser {

    public ClassFile parse(String filePath) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(filePath));

        // 1) Magic
        int magic = in.readInt();
        if (magic != 0xCAFEBABE) {
            throw new RuntimeException("Invalid .class file");
        }

        // 2) Versions
        int minor = in.readUnsignedShort();
        int major = in.readUnsignedShort();

        // 3) Constant Pool
        int cpCount = in.readUnsignedShort();
        ConstantPoolEntry[] constantPool = new ConstantPoolEntry[cpCount];

        for (int i = 1; i < cpCount; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1: // UTF8
                    constantPool[i] = new ConstantPoolEntry(tag, in.readUTF());
                    break;

                case 7: // Class reference
                case 8: // String reference
                    constantPool[i] = new ConstantPoolEntry(tag, in.readUnsignedShort());
                    break;

                case 9: // FieldRef
                case 10: // MethodRef
                case 11: // InterfaceMethodRef
                case 12: // NameAndType
                case 15: // MethodHandle
                case 16: // MethodType
                case 18: // InvokeDynamic
                    constantPool[i] = new ConstantPoolEntry(tag, new int[]{
                            in.readUnsignedShort(),
                            in.readUnsignedShort()
                    });
                    break;

                case 3: // Integer
                case 4: // Float
                    in.skipBytes(4);
                    i++;
                    break;

                case 5: // Long
                case 6: // Double
                    in.skipBytes(8);
                    i++;
                    break;

                default:
                    throw new RuntimeException("Unknown CP tag: " + tag);
            }
        }

        // 4) Access flags (skip)
        in.readUnsignedShort();

        // 5) This class
        int thisClassIndex = in.readUnsignedShort();
        int nameIndex = (Integer) constantPool[thisClassIndex].value;
        String className = constantPool[nameIndex].asUtf8();

        // 6) Super class
        int superClassIndex = in.readUnsignedShort();
        int superNameIndex = (Integer) constantPool[superClassIndex].value;
        String superName = constantPool[superNameIndex].asUtf8();

        in.close();

        return new ClassFile(className, superName, constantPool);
    }
}
