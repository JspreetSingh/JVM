package com.jvm.phase2;

import com.jvm.phase1.ClassFile;
import com.jvm.phase1.ConstantPoolEntry;
import com.jvm.phase1.MethodInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

/**
 * Minimal but capable bytecode interpreter:
 * - Frames (operand stack + locals)
 * - int ops: bipush/sipush/iconst_*
 * - iload/istore_* (0..3)
 * - arithmetic: iadd/isub/imul/idiv
 * - branching: if_icmp*, goto
 * - ldc, ldc_w
 * - getstatic (supports java/lang/System.out)
 * - invokevirtual (supports java/io/PrintStream.println for int/String)
 * - invokestatic (supports static methods in same class)
 * - return and ireturn
 *
 * Limitations:
 * - Only int and String types handled (no objects except PrintStream sentinel)
 * - invokestatic passes args using simple stack convention based on descriptor scanning for ints and object refs (basic)
 * - No exceptions, no class loading across files, no heap for user objects (phase for later)
 */
public class BytecodeInterpreter {

    private final ClassFile cf;

    public BytecodeInterpreter(ClassFile cf) {
        this.cf = cf;
    }

    // A Frame stores locals and operand stack for a method invocation
    private static class Frame {
        final int[] locals;        // local variables (ints or special markers for refs)
        final Deque<Object> stack; // operand stack (Integer or String or sentinel)
        final byte[] code;
        int pc;

        Frame(int maxLocals, int maxStack, byte[] code) {
            this.locals = new int[Math.max(1, maxLocals)];
            this.stack = new ArrayDeque<>(Math.max(10, maxStack));
            this.code = code;
            this.pc = 0;
        }
    }

    // sentinel representing System.out
    private static final Object PRINT_STREAM = new Object();

    // Entry point: execute a MethodInfo (assume it's static main or any method)
    public Object execute(MethodInfo method) {
        Frame frame = makeFrameForMethod(method);
        return runFrame(frame, method);
    }

    // Helper to create a Frame from MethodInfo (supports either code in CodeAttribute or fields on MethodInfo)
    private Frame makeFrameForMethod(MethodInfo method) {
        byte[] code;
        int maxLocals = 0, maxStack = 16;
        if (method.getCodeAttribute() != null) {
            code = method.getCodeAttribute().code;
            maxLocals = method.getCodeAttribute().maxLocals;
            maxStack = method.getCodeAttribute().maxStack;
        } else if (method.code != null) {
            code = method.code;
            maxLocals = method.maxLocals;
            maxStack = method.maxStack;
        } else {
            code = new byte[0];
        }
        return new Frame(maxLocals, maxStack, code);
    }

    // Run a frame until it returns (returns null for void return, Integer for ireturn, String for object)
    private Object runFrame(Frame frame, MethodInfo method) {
        byte[] code = frame.code;

        while (frame.pc < code.length) {
            int opcode = Byte.toUnsignedInt(code[frame.pc++]);

            switch (opcode) {

                // ---- constants ----
                case 0x03: // iconst_0
                case 0x04: // iconst_1
                case 0x05: // iconst_2
                case 0x06: // iconst_3
                case 0x07: // iconst_4
                case 0x08: { // iconst_5
                    int val = opcode - 0x03;
                    frame.stack.push(val);
                    break;
                }

                case 0x10: { // bipush (byte)
                    int b = code[frame.pc++];
                    frame.stack.push((int) b);
                    break;
                }

                case 0x11: { // sipush (two bytes)
                    int hi = code[frame.pc++] & 0xFF;
                    int lo = code[frame.pc++] & 0xFF;
                    int v = (short) ((hi << 8) | lo); // signed
                    frame.stack.push(v);
                    break;
                }

                // ldc (index is one byte) and ldc_w (two bytes)
                case 0x12: { // ldc
                    int index = code[frame.pc++] & 0xFF;
                    pushConstant(frame, index);
                    break;
                }
                case 0x13: { // ldc_w
                    int index = ((code[frame.pc++] & 0xFF) << 8) | (code[frame.pc++] & 0xFF);
                    pushConstant(frame, index);
                    break;
                }

                // ---- loads/stores (0..3 common) ----
                case 0x1A: // iload_0
                    frame.stack.push(frame.locals[0]);
                    break;
                case 0x1B: // iload_1
                    frame.stack.push(frame.locals[1]);
                    break;
                case 0x1C:
                    frame.stack.push(frame.locals[2]);
                    break;
                case 0x1D:
                    frame.stack.push(frame.locals[3]);
                    break;

                case 0x3B: // istore_0
                    frame.locals[0] = (int) frame.stack.pop();
                    break;
                case 0x3C: // istore_1
                    frame.locals[1] = (int) frame.stack.pop();
                    break;
                case 0x3D:
                    frame.locals[2] = (int) frame.stack.pop();
                    break;
                case 0x3E:
                    frame.locals[3] = (int) frame.stack.pop();
                    break;

                // ---- arithmetic ----
                case 0x60: { // iadd
                    int v2 = (int) frame.stack.pop();
                    int v1 = (int) frame.stack.pop();
                    frame.stack.push(v1 + v2);
                    break;
                }
                case 0x64: { // isub
                    int v2 = (int) frame.stack.pop();
                    int v1 = (int) frame.stack.pop();
                    frame.stack.push(v1 - v2);
                    break;
                }
                case 0x68: { // imul
                    int v2 = (int) frame.stack.pop();
                    int v1 = (int) frame.stack.pop();
                    frame.stack.push(v1 * v2);
                    break;
                }
                case 0x6C: { // idiv
                    int v2 = (int) frame.stack.pop();
                    int v1 = (int) frame.stack.pop();
                    frame.stack.push(v1 / v2);
                    break;
                }

                // ---- branching ----
                case 0xA7: { // goto (branch signed short)
                    int hi = code[frame.pc++] & 0xFF;
                    int lo = code[frame.pc++] & 0xFF;
                    int offset = (short) ((hi << 8) | lo);
                    frame.pc = frame.pc - 3 + offset + 3; // adjust since frame.pc already advanced by 0 at top
                    // simpler: set to (current pc before reading offset) + offset; we used earlier increment, so this arithmetic ensures alignment
                    // but we can instead compute as:
                    // frame.pc = frame.pc - 3 + offset + 3;  (keeps flow)
                    break;
                }

                case 0x9F: // if_icmpeq
                case 0xA0: // if_icmpne
                case 0xA1: // if_icmplt
                case 0xA2: // if_icmpge
                case 0xA3: // if_icmpgt
                case 0xA4: { // if_icmple
                    // we already consumed opcode, next two bytes are branch offset
                    int hi = code[frame.pc++] & 0xFF;
                    int lo = code[frame.pc++] & 0xFF;
                    int offset = (short) ((hi << 8) | lo);
                    int v2 = (int) frame.stack.pop();
                    int v1 = (int) frame.stack.pop();
                    boolean take = false;
                    switch (opcode) {
                        case 0x9F: take = (v1 == v2); break;
                        case 0xA0: take = (v1 != v2); break;
                        case 0xA1: take = (v1 < v2); break;
                        case 0xA2: take = (v1 >= v2); break;
                        case 0xA3: take = (v1 > v2); break;
                        case 0xA4: take = (v1 <= v2); break;
                    }
                    if (take) {
                        // branch relative to the opcode start: we already advanced pc past offset; compute new pc:
                        frame.pc = frame.pc - 3 + offset + 3;
                    }
                    break;
                }

                // ---- field access (only getstatic System.out) ----
                case 0xB2: { // getstatic
                    int idx = ((code[frame.pc++] & 0xFF) << 8) | (code[frame.pc++] & 0xFF);
                    ConstantPoolEntry ref = cf.constantPool[idx];
                    if (ref.tag != 9) throw new RuntimeException("getstatic: not a Fieldref at cp#" + idx);
                    int[] pair = (int[]) ref.value; // {classIndex, nameAndTypeIndex}
                    int classIndex = pair[0];
                    int ntIndex = pair[1];

                    // resolve class name
                    ConstantPoolEntry classEntry = cf.constantPool[classIndex];
                    int nameIdx = (int) classEntry.value;
                    String className = cf.getUtf8(nameIdx);

                    // resolve name
                    ConstantPoolEntry nt = cf.constantPool[ntIndex];
                    int[] ntArr = (int[]) nt.value;
                    String fieldName = cf.getUtf8(ntArr[0]);

                    if ("java/lang/System".equals(className) && "out".equals(fieldName)) {
                        frame.stack.push(PRINT_STREAM);
                    } else {
                        throw new RuntimeException("getstatic unsupported field: " + className + "." + fieldName);
                    }
                    break;
                }

                // ---- method invocation ----
                case 0xB6: { // invokevirtual (methodref)
                    int idx = ((code[frame.pc++] & 0xFF) << 8) | (code[frame.pc++] & 0xFF);
                    ConstantPoolEntry cref = cf.constantPool[idx];
                    if (cref.tag != 10) throw new RuntimeException("invokevirtual: not a Methodref at cp#" + idx);
                    int[] pair = (int[]) cref.value; // {classIndex, nameAndTypeIndex}
                    int classIndex = pair[0];
                    int ntIndex = pair[1];

                    // resolve class name
                    ConstantPoolEntry classEntry = cf.constantPool[classIndex];
                    int classNameIndex = (int) classEntry.value;
                    String className = cf.getUtf8(classNameIndex);

                    // resolve name+desc
                    ConstantPoolEntry nt = cf.constantPool[ntIndex];
                    int[] ntArr = (int[]) nt.value;
                    String methodName = cf.getUtf8(ntArr[0]);
                    String methodDesc = cf.getUtf8(ntArr[1]);

                    // Only support PrintStream.println for now
                    if ("java/io/PrintStream".equals(className) && methodName.equals("println")) {
                        // Determine how many args based on descriptor (simple parsing)
                        // Common descriptors: (I)V or (Ljava/lang/String;)V
                        if (methodDesc.startsWith("(I")) {
                            Object val = frame.stack.pop();
                            Object objref = frame.stack.pop(); // printstream
                            if (val instanceof Integer) System.out.println((Integer) val);
                            else System.out.println(val);
                        } else if (methodDesc.startsWith("(L")) {
                            Object val = frame.stack.pop();
                            Object objref = frame.stack.pop();
                            if (val instanceof String) System.out.println((String) val);
                            else System.out.println(val);
                        } else {
                            // fallback: try pop one arg
                            Object val = frame.stack.pop();
                            frame.stack.pop(); // pop objref
                            System.out.println(val);
                        }
                        break;
                    }

                    // if not PrintStream, unsupported in this phase
                    throw new RuntimeException("invokevirtual unsupported: " + className + "." + methodName + methodDesc);
                }

                case 0xB8: { // invokestatic (call static method in same class possibly)
                    int idx = ((code[frame.pc++] & 0xFF) << 8) | (code[frame.pc++] & 0xFF);
                    ConstantPoolEntry cref = cf.constantPool[idx];
                    if (cref.tag != 10) throw new RuntimeException("invokestatic: not a Methodref at cp#" + idx);
                    int[] pair = (int[]) cref.value;
                    int classIndex = pair[0];
                    int ntIndex = pair[1];

                    ConstantPoolEntry classEntry = cf.constantPool[classIndex];
                    int cnameIdx = (int) classEntry.value;
                    String className = cf.getUtf8(cnameIdx);

                    ConstantPoolEntry nt = cf.constantPool[ntIndex];
                    int[] ntArr = (int[]) nt.value;
                    String methodName = cf.getUtf8(ntArr[0]);
                    String methodDesc = cf.getUtf8(ntArr[1]);

                    // For this minimal VM: only invoke static methods that exist in the same ClassFile (cf)
                    MethodInfo target = cf.findMethod(methodName, methodDesc);
                    if (target == null) throw new RuntimeException("invokestatic: method not found " + methodName + methodDesc);

                    // prepare args based on descriptor (simple: count ints and refs)
                    Object[] argsForTarget = popArgsForDescriptor(frame, methodDesc);

                    // create a new frame and push args into locals (0..n-1)
                    Frame newFrame = makeFrameForMethod(target);
                    for (int i = 0; i < argsForTarget.length && i < newFrame.locals.length; i++) {
                        Object a = argsForTarget[i];
                        if (a instanceof Integer) newFrame.locals[i] = (int) a;
                        else if (a == null) newFrame.locals[i] = 0;
                        else {
                            // we do not support object references except strings: store special marker index in locals as 0
                            newFrame.locals[i] = 0;
                        }
                    }

                    Object ret = runFrame(newFrame, target);
                    // if target returned an int, push back to caller
                    if (ret instanceof Integer) frame.stack.push((Integer) ret);
                    // otherwise ignore (void)
                    break;
                }

                // ---- returns ----
                case 0xAC: { // ireturn
                    Object ret = frame.stack.pop();
                    return ret;
                }

                case 0xB1: { // return void
                    return null;
                }

                default:
                    throw new RuntimeException(String.format("Unsupported opcode: 0x%02X at pc=%d", opcode, frame.pc-1));
            }
        }
        return null;
    }

    // Push constant from constant pool onto operand stack
    private void pushConstant(Frame frame, int index) {
        ConstantPoolEntry e = cf.constantPool[index];
        if (e == null) {
            frame.stack.push(null);
            return;
        }
        switch (e.tag) {
            case 1: // UTF8
                frame.stack.push(e.asUtf8());
                break;
            case 3: // Integer
                frame.stack.push((int) e.value);
                break;
            case 8: // String (index to Utf8)
                int utfIdx = (int) e.value;
                frame.stack.push(cf.getUtf8(utfIdx));
                break;
            default:
                frame.stack.push(null);
                break;
        }
    }

    // read arguments from frame.stack for invocation based on descriptor (very simple)
    // returns args in order [arg0, arg1, ...] (left-to-right)
    private Object[] popArgsForDescriptor(Frame frame, String desc) {
        // basic parser: count number of args (only handles ints and object refs)
        if (desc == null || !desc.startsWith("(")) return new Object[0];
        String inside = desc.substring(1, desc.indexOf(')'));
        ArrayDeque<Object> tmp = new ArrayDeque<>();
        // push reversed by popping from operand stack
        // count args
        int argCount = 0;
        for (int i = 0; i < inside.length(); ) {
            char c = inside.charAt(i);
            if (c == 'L') {
                // object ref, find ';'
                int semi = inside.indexOf(';', i);
                i = semi + 1;
                argCount++;
            } else if (c == '[') {
                // array type, skip char
                i++;
            } else {
                // primitive (I, etc.)
                i++;
                argCount++;
            }
        }
        Object[] args = new Object[argCount];
        for (int i = argCount - 1; i >= 0; i--) {
            args[i] = frame.stack.pop();
        }
        return args;
    }
}
