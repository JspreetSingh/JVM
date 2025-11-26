package com.jvm.phase2; // change to your package if different

import com.jvm.phase1.ClassFile;
import com.jvm.phase1.MethodInfo;

import java.util.Stack;

public class BytecodeInterpreter {

    private final ClassFile cf;

    public BytecodeInterpreter(ClassFile cf) {
        this.cf = cf;
    }

    public void execute(MethodInfo method) {
        CodeAttribute codeAttr = method.getCodeAttribute();
        if (codeAttr == null) {
            System.out.println("No code attribute for method.");
            return;
        }

        byte[] code = codeAttr.code;
        Object[] locals = new Object[Math.max(1, codeAttr.maxLocals)];
        Stack<Object> stack = new Stack<>();
        int pc = 0;

        while (pc < code.length) {
            int opcode = Byte.toUnsignedInt(code[pc++]);

            switch (opcode) {

                case 0xB2: { // getstatic (indexbyte1, indexbyte2)
                    int index = (Byte.toUnsignedInt(code[pc++]) << 8) | Byte.toUnsignedInt(code[pc++]);
                    // index refers to a Fieldref in constant pool
                    // Resolve Fieldref -> {classIndex, nameAndTypeIndex}
                    int[] ref = cf.getRefIndexes(index);
                    if (ref != null) {
                        String className = cf.getClassNameFromCp(ref[0]);
                        String[] nt = cf.getNameAndType(ref[1]); // {name, descriptor}
                        // special-case java/lang/System.out
                        if ("java/lang/System".equals(className) && "out".equals(nt[0])) {
                            stack.push(System.out); // sentinel
                        } else {
                            // For now push null for other statics
                            stack.push(null);
                        }
                    } else {
                        stack.push(null);
                    }
                    break;
                }

                case 0x12: { // ldc (index)
                    int cindex = Byte.toUnsignedInt(code[pc++]);
                    Object constant = cf.getConstant(cindex);
                    stack.push(constant != null ? constant : "<?>");
                    break;
                }

                case 0x13: { // ldc_w (indexbyte1, indexbyte2)
                    int cindex = (Byte.toUnsignedInt(code[pc++]) << 8) | Byte.toUnsignedInt(code[pc++]);
                    Object constant = cf.getConstant(cindex);
                    stack.push(constant != null ? constant : "<?>");
                    break;
                }

                case 0xB6: { // invokevirtual (methodref index)
                    int mref = (Byte.toUnsignedInt(code[pc++]) << 8) | Byte.toUnsignedInt(code[pc++]);
                    int[] ref = cf.getRefIndexes(mref);
                    if (ref != null) {
                        String className = cf.getClassNameFromCp(ref[0]);
                        String[] nt = cf.getNameAndType(ref[1]);
                        String methodName = nt[0];
                        String methodDesc = nt[1];

                        // Pop arguments and objectref according to descriptor (we only handle println(String))
                        // For println(String) descriptor is "(Ljava/lang/String;)V" -> one argument
                        Object arg = null;
                        Object objRef = null;

                        // handle common case println(String)
                        if ("(Ljava/lang/String;)V".equals(methodDesc) || "(Ljava/lang/Object;)V".equals(methodDesc)) {
                            arg = stack.pop();
                            objRef = stack.pop();
                            // if sentinel System.out, call host println
                            if (objRef == System.out && arg instanceof String) {
                                System.out.println((String) arg);
                            } else if (objRef == System.out && arg != null) {
                                System.out.println(arg.toString());
                            } else {
                                System.out.println("invokevirtual fallback: " + className + "." + methodName + " arg=" + arg);
                            }
                        } else if ("()V".equals(methodDesc)) {
                            // no args, pop objectref
                            objRef = stack.pop();
                            System.out.println("invokevirtual for no-arg method " + methodName + " on " + className);
                        } else {
                            // generic fallback: try to pop correct number of args based on descriptor parsing (not implemented)
                            System.out.println("invokevirtual: unsupported descriptor " + methodDesc);
                        }
                    } else {
                        // cannot resolve methodref
                        System.out.println("invokevirtual: unresolved methodref at index " + mref);
                        // consume 2 bytes already consumed above
                    }
                    break;
                }

                case 0xB1: // return (void)
                    return;

                // helpful: print unhandled opcodes to debug
                default:
                    System.out.println(String.format("Unhandled opcode 0x%02X at pc=%d", opcode, pc-1));
                    return;
            }
        }
    }
}
