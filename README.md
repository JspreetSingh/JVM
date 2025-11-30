🧵 Mini JVM in Java – A Fully Functional Java Virtual Machine (WIP)

A lightweight, educational Java Virtual Machine implemented from scratch in pure Java, capable of parsing .class files, interpreting bytecode, handling method invocations, stack frames, objects, fields, and control flow.

This project follows the internal architecture of a real JVM but in a simplified and clean way—making it perfect for learning JVM internals and bytecode execution.

📌 Current Status (Working Features)

Your Mini JVM currently supports:

✔ Phase 1 — Class File Parser

Reads & decodes .class files (Java 17+ / version 61)

Parses:

1. Constant Pool
2. Methods
3. Fields
4. Attributes (Code, Exceptions, LineNumberTable)

Retrieves UTF-8 strings, Class names, NameAndType, MethodRef, FieldRef info

Extracts max_stack, max_locals, and raw bytecode

✔ Phase 2 — Bytecode Interpreter

Fully working bytecode engine supporting:

Arithmetic

✔ iadd, isub, imul, idiv
✔ iconst_*, bipush, sipush
✔ iload, istore

Control Flow

✔ if_icmpgt, if_icmplt, if_icmpeq, etc.
✔ goto

Literals

✔ ldc (String, int)

✔ Phase 3 — Method Invocation Engine

Supports:

1. invokestatic
2. invokespecial
3. invokestatic
4. Native host linking for System.out.println()

✔ Phase 4 — Object Model

Your JVM now supports:

1. Object allocation (new)
2. Field storage (instance fields)
3. getfield
4. putfield
5. invokevirtual (dynamic dispatch)
6. HashMap-based object heap

✔ Example Programs Successfully Running

These programs run correctly inside your JVM:

🔹 Simple Math
```java
int a = 5, b = 7;
System.out.println(a + b);
```
🔹 If / Else
```java
if (a > b) System.out.println(100);
```

🔹 Methods
```java
static int add(int a, int b) { return a + b; }
```

🔹 Two Sum (HashMap)
```java
HashMap<Integer,Integer> map = new HashMap<>();
```

Steps to run the program is below:

**Step 1: Make a java class Test.java in src/test/java/**

**Step 2: Paste your code in that Test.java class**

**Step 3: Open terminal in your IDE and change your directory to src/test/java/**

**Step 4: compile the code using below command:**
```text
javac Test.java
```
**Step 5: Execute the code using below command:**
```text
java Test
```
<p align="center">
  OR
</p>

**Run the main method from your IDE**

➡️ Working HashMap means:
✔ objects
✔ fields
✔ invokevirtual
✔ arrays
✔ method calls
✔ branching
✔ load/store
➡️ Your JVM is already very powerful.


Expected Output

You’ll see:
```text
Java Class Version: 61.0
Methods Count: 2
Running main...
=== Finished ===
```

**Plus whatever your program prints.**


