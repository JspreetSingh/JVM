package com.jvm.phase2;

import java.util.Stack;

public class StackFrame {
    public final Object[] locals;
    public final Stack<Object> stack = new Stack<>();
    public final byte[] code;
    public int pc = 0;

    public StackFrame(int maxLocals, int maxStack, byte[] code) {
        this.locals = new Object[maxLocals];
        this.code = code;
    }
}
