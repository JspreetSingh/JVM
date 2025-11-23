package com.jvm.phase2;

import com.jvm.phase1.ClassFile;
import com.jvm.phase1.MethodInfo;

import java.io.UnsupportedEncodingException;
import java.util.Stack;

public class BytecodeInterpreter {
    private final ClassFile classFile;

    public BytecodeInterpreter(ClassFile cf) {
        this.classFile = cf;
    }

    // Execute the supplied method's code attribute (static main, no args passed from host)
    public void execute(MethodInfo method) {
        CodeAttribute codeAttr = method.getCodeAttribute();
        if (codeAttr == null) {
            System.out.println("No code to execute for method.");
            return;
        }

        byte[] code = codeAttr.code;
        Stack<Object> stack = new Stack<>();
        int pc = 0;

        while (pc < code.length) {
            int opcode = Byte.toUnsignedInt(code[pc++]);
            switch (opcode) {
                case 0xb2: { // getstatic (indexbyte1, indexbyte2)
                    int index = ((Byte.toUnsignedInt(code[pc++]) << 8) | Byte.toUnsignedInt(code[pc++]));
                    // For demo: if it references java/lang/System.out push a sentinel
                    // We try to resolve constant pool entry to see class/name
                    // Many classfiles use GETSTATIC to reference fieldref -> class_index/nameandtype
                    // We'll push a simple sentinel "System.out" object
                    stack.push(System.out);
                    break;
                }
                case 0x12: { // ldc (index)
                    int cIndex = Byte.toUnsignedInt(code[pc++]);
                    // constant pool entry: expect UTF8 string or String constant
                    // try resolving via classFile.getUtf8(cIndex)
                    String s = classFile.getUtf8(cIndex);
                    if (s != null) stack.push(s);
                    else stack.push("<?>");
                    break;
                }
                case 0xb6: { // invokevirtual (methodref index)
                    int mref = ((Byte.toUnsignedInt(code[pc++]) << 8) | Byte.toUnsignedInt(code[pc++]));
                    // pop argument(s) then objectref
                    Object arg = stack.pop();
                    Object obj = stack.pop();
                    // For demo handle java/io/PrintStream.println(String)
                    if (obj == System.out && arg instanceof String) {
                        System.out.println((String) arg);
                    } else {
                        System.out.println("invokevirtual fallback: obj=" + obj + " arg=" + arg);
                    }
                    break;
                }
                case 0xb1: // return
                    return;
                default:
                    System.out.println(String.format("Unhandled opcode 0x%02X at pc=%d", opcode, pc-1));
                    return;
            }
        }
    }
}
