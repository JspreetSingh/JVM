package com.jvm.phase1;

import java.io.DataInputStream;
import java.io.IOException;

public class AttributeInfo {
    public final String name;
    public final int length;

    public AttributeInfo(String name, int length) {
        this.name = name;
        this.length = length;
    }

    // convenience to skip unknown attribute bytes
    public static void skip(DataInputStream in) throws IOException {
        in.readUnsignedShort(); // attribute_name_index
        int len = in.readInt();
        in.skipBytes(len);
    }
}
