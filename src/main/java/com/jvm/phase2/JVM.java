package com.jvm.phase2;

import com.jvm.phase1.ClassFile;
import com.jvm.phase1.ConstantPoolEntry;
import com.jvm.phase1.MethodInfo;
import com.jvm.phase3.Heap;
import com.jvm.phase3.JavaObject;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase-3 JVM: adds a tiny heap + object support:
 * - new (0xBB), dup (0x59)
 * - getfield (0xB4), putfield (0xB5)
 * - invokespecial (0xB7) for constructors (basic)
 * - retains invokestatic, invokevirtual (println), ireturn/return, branching, arithmetic
 *
 * NOTE: This is a minimal educational runtime; it uses integer object refs (Integer) to identify heap objects.
 */
public class JVM {

    private final ClassFile cf;
    private final Heap heap = new Heap();

    // sentinel for System.out (printstream)
    private static final Object PRINT_STREAM = new Object();

    public JVM(ClassFile classFile) {
        this.cf = classFile;
    }

    // Frame for execution
    private static class Frame {
        final Object[] locals;      // ints as Integer, object refs as Integer too, strings as String
        final Deque<Object> stack;  // operand stack
        final byte[] code;
        int pc = 0;
        final MethodInfo method;

        Frame(MethodInfo method, int maxLocals, int maxStack, byte[] code) {
            this.method = method;
            this.locals = new Object[Math.max(1, maxLocals)];
            this.stack = new ArrayDeque<>();
            this.code = code == null ? new byte[0] : code;
            this.pc = 0;
        }
    }

    // Entry point
    public void start() {
        MethodInfo main = cf.findMainMethod();
        if (main == null) {
            System.out.println("No main found.");
            return;
        }
        invokeMethod(main, null);
    }

    // invoke a method (static or instance helper). args placed in locals[0..]
    private Object invokeMethod(MethodInfo method, Object[] args) {
        byte[] code;
        int maxLocals = Math.max(1, method.maxLocals);
        int maxStack = Math.max(8, method.maxStack);
        if (method.getCodeAttribute() != null) {
            code = method.getCodeAttribute().code;
            maxLocals = method.getCodeAttribute().maxLocals;
            maxStack = method.getCodeAttribute().maxStack;
        } else {
            code = method.code == null ? new byte[0] : method.code;
        }

        Frame frame = new Frame(method, maxLocals, maxStack, code);
        if (args != null) {
            for (int i = 0; i < args.length && i < frame.locals.length; i++) {
                frame.locals[i] = args[i];
            }
        }
        return runFrame(frame);
    }

    // Core interpreter for a single frame (supports calling itself for nested calls)
    private Object runFrame(Frame frame) {
        byte[] code = frame.code;

        while (frame.pc < code.length) {
            int opcodePos = frame.pc;
            int opcode = Byte.toUnsignedInt(code[frame.pc++]);

            switch (opcode) {

                // --------- constants ----------
                case 0x02: frame.stack.push(-1); break;
                case 0x03: frame.stack.push(0); break;
                case 0x04: frame.stack.push(1); break;
                case 0x05: frame.stack.push(2); break;
                case 0x06: frame.stack.push(3); break;
                case 0x07: frame.stack.push(4); break;
                case 0x08: frame.stack.push(5); break;

                case 0x10: // bipush
                    frame.stack.push((int) (byte) code[frame.pc++]);
                    break;

                case 0x11: { // sipush
                    int hi = code[frame.pc++] & 0xFF;
                    int lo = code[frame.pc++] & 0xFF;
                    int v = (short) ((hi << 8) | lo);
                    frame.stack.push(v);
                    break;
                }

                case 0x12: { // ldc
                    int idx = code[frame.pc++] & 0xFF;
                    pushConstantToStack(frame, idx);
                    break;
                }
                case 0x13: { // ldc_w
                    int idx = ((code[frame.pc++] & 0xFF) << 8) | (code[frame.pc++] & 0xFF);
                    pushConstantToStack(frame, idx);
                    break;
                }

                // --------- loads ----------
                case 0x1A: frame.stack.push(asInt(frame.locals[0])); break;
                case 0x1B: frame.stack.push(asInt(frame.locals[1])); break;
                case 0x1C: frame.stack.push(asInt(frame.locals[2])); break;
                case 0x1D: frame.stack.push(asInt(frame.locals[3])); break;
                case 0x15: { int idx = code[frame.pc++] & 0xFF; frame.stack.push(asInt(frame.locals[idx])); break; }

                // --------- stores ----------
                case 0x3B: frame.locals[0] = frame.stack.pop(); break;
                case 0x3C: frame.locals[1] = frame.stack.pop(); break;
                case 0x3D: frame.locals[2] = frame.stack.pop(); break;
                case 0x3E: frame.locals[3] = frame.stack.pop(); break;
                case 0x36: { int idx = code[frame.pc++] & 0xFF; frame.locals[idx] = frame.stack.pop(); break; }

                // iinc
                case 0x84: {
                    int index = code[frame.pc++] & 0xFF;
                    int c = (byte) code[frame.pc++]; // signed
                    Integer cur = asInt(frame.locals[index]);
                    frame.locals[index] = (cur == null ? c : cur + c);
                    break;
                }

                // stack
                case 0x57: frame.stack.pop(); break; // pop
                case 0x59: { Object top = frame.stack.peek(); frame.stack.push(top); break; } // dup

                // arithmetic
                case 0x60: { int v2 = (int) frame.stack.pop(); int v1 = (int) frame.stack.pop(); frame.stack.push(v1 + v2); break; }
                case 0x64: { int v2 = (int) frame.stack.pop(); int v1 = (int) frame.stack.pop(); frame.stack.push(v1 - v2); break; }
                case 0x68: { int v2 = (int) frame.stack.pop(); int v1 = (int) frame.stack.pop(); frame.stack.push(v1 * v2); break; }
                case 0x6C: { int v2 = (int) frame.stack.pop(); int v1 = (int) frame.stack.pop(); frame.stack.push(v1 / v2); break; }
                case 0x70: { int v2 = (int) frame.stack.pop(); int v1 = (int) frame.stack.pop(); frame.stack.push(v1 % v2); break; }
                case 0x74: { int v = (int) frame.stack.pop(); frame.stack.push(-v); break; }

                // branches
                case 0x99: { // ifeq
                    int opcodeStart = opcodePos;
                    int hi = code[frame.pc++] & 0xFF;
                    int lo = code[frame.pc++] & 0xFF;
                    int offset = (short) ((hi << 8) | lo);
                    int val = (int) frame.stack.pop();
                    if (val == 0) frame.pc = opcodeStart + offset;
                    break;
                }
                case 0x9A: { // ifne
                    int opcodeStart = opcodePos;
                    int hi = code[frame.pc++] & 0xFF;
                    int lo = code[frame.pc++] & 0xFF;
                    int offset = (short) ((hi << 8) | lo);
                    int val = (int) frame.stack.pop();
                    if (val != 0) frame.pc = opcodeStart + offset;
                    break;
                }
                // if_icmp*
                case 0x9F: case 0xA0: case 0xA1: case 0xA2: case 0xA3: case 0xA4: {
                    int opcodeStart = opcodePos;
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
                    if (take) frame.pc = opcodeStart + offset;
                    break;
                }
                case 0xA7: { // goto
                    int opcodeStart = opcodePos;
                    int hi = code[frame.pc++] & 0xFF;
                    int lo = code[frame.pc++] & 0xFF;
                    int offset = (short) ((hi << 8) | lo);
                    frame.pc = opcodeStart + offset;
                    break;
                }

                // --------- object support ----------
                case 0xBB: { // new
                    int idx = readUnsignedShort(frame);
                    /* class entry at idx -> value = nameUtfIndex */
                    ConstantPoolEntry classEntry = cf.constantPool[idx];
                    if (classEntry == null || classEntry.tag != 7) {
                        throw new RuntimeException("new: not a Class entry at cp#" + idx);
                    }
                    int nameUtfIndex = (int) classEntry.value;
                    String className = cf.getUtf8(nameUtfIndex);

                    JavaObject obj = new JavaObject(className);
                    int ref = heap.allocate(obj); // integer id
                    frame.stack.push(ref); // push objectref as Integer
                    break;
                }

                case 0xB5: { // putfield
                    int idx = readUnsignedShort(frame);
                    ConstantPoolEntry ref = cf.constantPool[idx];
                    if (ref == null || ref.tag != 9) throw new RuntimeException("putfield: not a Fieldref cp#" + idx);
                    int[] pair = (int[]) ref.value;
                    int classIdx = pair[0];
                    int natIdx = pair[1];

                    ConstantPoolEntry classRef = cf.constantPool[classIdx];
                    int classNameUtf = (int) classRef.value;
                    String className = cf.getUtf8(classNameUtf);

                    ConstantPoolEntry nat = cf.constantPool[natIdx];
                    int[] ntarr = (int[]) nat.value;
                    String fieldName = cf.getUtf8(ntarr[0]);
                    String fieldDesc = cf.getUtf8(ntarr[1]);

                    Object value = frame.stack.pop(); // value (could be Integer or String or objectref Integer)
                    Object objRef = frame.stack.pop(); // objectref (Integer)
                    if (!(objRef instanceof Integer)) throw new RuntimeException("putfield: objectref expected");
                    int id = (Integer) objRef;
                    JavaObject jobj = heap.get(id);
                    if (jobj == null) throw new RuntimeException("putfield: invalid objectref " + id);

                    // store value directly (no type conversion)
                    jobj.setField(fieldName, value);
                    break;
                }

                case 0xB4: { // getfield
                    int idx = readUnsignedShort(frame);
                    ConstantPoolEntry ref = cf.constantPool[idx];
                    if (ref == null || ref.tag != 9) throw new RuntimeException("getfield: not a Fieldref cp#" + idx);
                    int[] pair = (int[]) ref.value;
                    int classIdx = pair[0];
                    int natIdx = pair[1];

                    ConstantPoolEntry classRef = cf.constantPool[classIdx];
                    int classNameUtf = (int) classRef.value;
                    String className = cf.getUtf8(classNameUtf);

                    ConstantPoolEntry nat = cf.constantPool[natIdx];
                    int[] ntarr = (int[]) nat.value;
                    String fieldName = cf.getUtf8(ntarr[0]);
                    String fieldDesc = cf.getUtf8(ntarr[1]);

                    Object objRef = frame.stack.pop();
                    if (!(objRef instanceof Integer)) throw new RuntimeException("getfield: objectref expected");
                    int id = (Integer) objRef;
                    JavaObject jobj = heap.get(id);
                    if (jobj == null) throw new RuntimeException("getfield: invalid objectref " + id);

                    Object val = jobj.getField(fieldName);
                    frame.stack.push(val);
                    break;
                }

                // --------- method invocation ----------
                case 0xB6: { // invokevirtual
                    int idx = readUnsignedShort(frame);
                    ConstantPoolEntry methodRef = cf.constantPool[idx];
                    if (methodRef == null || methodRef.tag != 10) throw new RuntimeException("invokevirtual: not Methodref cp#" + idx);
                    int[] pair = (int[]) methodRef.value;
                    int classIdx = pair[0];
                    int natIdx = pair[1];

                    String className = cf.getClassNameFromCp(classIdx);
                    String[] nameDesc = cf.getNameAndType(natIdx);
                    String methodName = nameDesc[0];
                    String methodDesc = nameDesc[1];

                    // PrintStream.println special-case
                    if ("java/io/PrintStream".equals(className) && "println".equals(methodName)) {
                        Object arg = popArgByDescriptor(frame, methodDesc);
                        Object objref = frame.stack.pop(); // printstream placeholder
                        if (arg instanceof Integer) System.out.println((Integer) arg);
                        else System.out.println(arg);
                        break;
                    }

                    // if same-class instance method: resolve and call, first arg is 'this'
                    if (className.equals(cf.getClassName())) {
                        MethodInfo target = cf.resolveMethod(idx);
                        Object[] callArgs = popArgsForDescriptor(frame, methodDesc); // returns args left-to-right
                        // 'this' is expected to have been pushed before args for invokevirtual; our popArgsForDescriptor removed args, but we still need to pop 'this'
                        Object thisRef = frame.stack.pop();
                        // prepare locals: locals[0] = thisRef, locals[1..] = args...
                        Object[] localsForCallee = new Object[callArgs.length + 1];
                        localsForCallee[0] = thisRef;
                        System.arraycopy(callArgs, 0, localsForCallee, 1, callArgs.length);
                        Object res = invokeMethod(target, localsForCallee);
                        if (res != null) frame.stack.push(res);
                        break;
                    }

                    throw new RuntimeException("invokevirtual unsupported: " + className + "." + methodName + methodDesc);
                }

                case 0xB8: { // invokestatic
                    int idx = readUnsignedShort(frame);
                    ConstantPoolEntry methodRef = cf.constantPool[idx];
                    if (methodRef == null || methodRef.tag != 10) throw new RuntimeException("invokestatic: not Methodref cp#" + idx);
                    int[] pair = (int[]) methodRef.value;
                    int classIdx = pair[0];
                    int natIdx = pair[1];

                    String className = cf.getClassNameFromCp(classIdx);
                    String[] nameDesc = cf.getNameAndType(natIdx);
                    String methodName = nameDesc[0];
                    String methodDesc = nameDesc[1];

                    if (!className.equals(cf.getClassName())) throw new RuntimeException("invokestatic supports same-class only");

                    MethodInfo target = cf.findMethod(methodName, methodDesc);
                    if (target == null) throw new RuntimeException("invokestatic: method not found " + methodName + methodDesc);

                    Object[] callArgs = popArgsForDescriptor(frame, methodDesc);
                    Object ret = invokeMethod(target, callArgs);
                    if (ret != null) frame.stack.push(ret);
                    break;
                }

                case 0xB7: { // invokespecial (constructors or private)
                    int idx = readUnsignedShort(frame);
                    // resolve MethodInfo from cp index
                    MethodInfo target = cf.resolveMethod(idx);
                    String desc = target.descriptor;
                    Object[] callArgs = popArgsForDescriptor(frame, desc);
                    // For constructors 'this' should be below args on stack (invokespecial normally pops objectref then args)
                    Object maybeThis = frame.stack.pop();
                    // if 'this' is an objectref Integer, pass it as locals[0]
                    Object[] localsForCtor = new Object[callArgs.length + 1];
                    localsForCtor[0] = maybeThis;
                    System.arraycopy(callArgs, 0, localsForCtor, 1, callArgs.length);
                    Object r = invokeMethod(target, localsForCtor);
                    if (r != null) frame.stack.push(r);
                    break;
                }

                // returns
                case 0xAC: { // ireturn
                    Object val = frame.stack.pop();
                    return val;
                }
                case 0xB0: { // areturn (object return)
                    Object val = frame.stack.pop();
                    return val;
                }
                case 0xB1: { // return void
                    return null;
                }

                default:
                    throw new RuntimeException(String.format("Unsupported opcode 0x%02X at pc=%d", opcode, opcodePos));
            }
        }
        return null;
    }

    // --- helpers ---

    private static Integer asInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Integer) return (Integer) o;
        if (o instanceof String) return Integer.parseInt((String) o);
        return Integer.parseInt(o.toString());
    }

    private int readUnsignedShort(Frame f) {
        int hi = f.code[f.pc++] & 0xFF;
        int lo = f.code[f.pc++] & 0xFF;
        return (hi << 8) | lo;
    }

    // push constant from constant pool
    private void pushConstantToStack(Frame frame, int cpIndex) {
        ConstantPoolEntry e = cf.constantPool[cpIndex];
        if (e == null) { frame.stack.push(null); return; }
        switch (e.tag) {
            case 1: frame.stack.push(e.asUtf8()); break;
            case 3: frame.stack.push((int) e.value); break;
            case 8: {
                int utfIdx = (int) e.value;
                frame.stack.push(cf.getUtf8(utfIdx));
                break;
            }
            default: frame.stack.push(null); break;
        }
    }

    // pop arguments for descriptor: returns left-to-right array
    private Object[] popArgsForDescriptor(Frame frame, String desc) {
        if (desc == null || !desc.startsWith("(")) return new Object[0];
        String inside = desc.substring(1, desc.indexOf(')'));
        // parse argument types
        List<String> types = new ArrayList<>();
        for (int i = 0; i < inside.length(); ) {
            char c = inside.charAt(i);
            if (c == 'L') {
                int semi = inside.indexOf(';', i);
                types.add(inside.substring(i, semi + 1));
                i = semi + 1;
            } else if (c == '[') {
                // treat array as object
                i++;
                if (inside.charAt(i) == 'L') {
                    int semi = inside.indexOf(';', i);
                    types.add(inside.substring(i, semi + 1));
                    i = semi + 1;
                } else {
                    types.add(String.valueOf(inside.charAt(i)));
                    i++;
                }
            } else {
                types.add(String.valueOf(c));
                i++;
            }
        }
        Object[] args = new Object[types.size()];
        // pop in reverse order
        for (int i = types.size() - 1; i >= 0; i--) {
            args[i] = frame.stack.pop();
        }
        return args;
    }

    // pop one arg for println based on descriptor
    private Object popArgByDescriptor(Frame frame, String methodDesc) {
        int start = methodDesc.indexOf('(');
        int end = methodDesc.indexOf(')');
        String inside = methodDesc.substring(start + 1, end);
        if (inside.length() == 0) return null;
        if (inside.equals("I")) return frame.stack.pop();
        if (inside.startsWith("L")) return frame.stack.pop();
        return frame.stack.pop();
    }
}
