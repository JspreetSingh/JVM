package com.jvm;

import com.jvm.phase1.ClassFile;
import com.jvm.phase1.ClassFileParser;

public class Main {
    public static void main(String[] args) throws Exception {
        ClassFileParser parser = new ClassFileParser();
        ClassFile cf = parser.parse("src/test/java/Test.class"); // Supply correct path

        System.out.println("Class: " + cf.getClassName());
        System.out.println("Super: " + cf.getSuperName());
    }
}
