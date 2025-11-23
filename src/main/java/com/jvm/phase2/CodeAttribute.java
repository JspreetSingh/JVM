package com.jvm.phase2;

import com.jvm.phase1.AttributeInfo;
import com.jvm.phase1.ConstantPoolEntry;
import java.io.DataInputStream;
import java.io.IOException;

public class CodeAttribute extends AttributeInfo {
    public int maxStack;
    public int maxLocals;
    public byte[] code;

    public CodeAttribute(String name, int length) {
        super(name, length);
    }

    // read code attribute (skips nested attributes)
    public static CodeAttribute read(DataInputStream in, ConstantPoolEntry[] pool, int attributeLength) throws IOException {
        int maxStack = in.readUnsignedShort();
        int maxLocals = in.readUnsignedShort();
        int codeLength = in.readInt();
        byte[] code = new byte[codeLength];
        in.readFully(code);

        int exceptionTableLength = in.readUnsignedShort();
        for (int i = 0; i < exceptionTableLength; i++) {
            in.readUnsignedShort(); // start_pc
            in.readUnsignedShort(); // end_pc
            in.readUnsignedShort(); // handler_pc
            in.readUnsignedShort(); // catch_type
        }

        int attributesCount = in.readUnsignedShort();
        for (int i = 0; i < attributesCount; i++) {
            // skip nested attributes
            int nameIndex = in.readUnsignedShort();
            int len = in.readInt();
            in.skipBytes(len);
        }

        CodeAttribute ca = new CodeAttribute("Code", attributeLength);
        ca.maxStack = maxStack;
        ca.maxLocals = maxLocals;
        ca.code = code;
        return ca;
    }
}
