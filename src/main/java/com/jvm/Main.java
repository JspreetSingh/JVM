package com.jvm;//package com.jvm;
//
//import com.jvm.phase1.ClassFile;
//import com.jvm.phase1.ClassFileParser;
//import com.jvm.phase1.MethodInfo;
//import com.jvm.phase2.BytecodeInterpreter;
//
//public class Main {
//    public static void main(String[] args) throws Exception {
//        String classFilePath;
//        if (args.length > 0) classFilePath = args[0];
//        else classFilePath = "src/test/java/Test.class"; // adjust path if different
//
//        ClassFileParser parser = new ClassFileParser();
//        ClassFile cf = parser.parse(classFilePath);
//
//        MethodInfo mainMethod = cf.findMainMethod();
//        if (mainMethod == null) {
//            System.out.println("No main found.");
//            return;
//        }
//
//        System.out.println("=== Running main ===");
//        BytecodeInterpreter interp = new BytecodeInterpreter(cf);
//        interp.execute(mainMethod);
//        System.out.println("=== Finished ===");
//    }
//}


import com.jvm.phase1.ClassFileParser;
import com.jvm.phase2.JVM;

public class Main {
    public static void main(String[] args) throws Exception {
        ClassFileParser parser = new ClassFileParser();
        var cf = parser.parse("src/test/java/Test.class");

        JVM jvm = new JVM(cf);
        jvm.start();
    }
}
