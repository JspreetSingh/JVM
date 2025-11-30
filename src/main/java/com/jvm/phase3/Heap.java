package com.jvm.phase3;

import java.util.HashMap;
import java.util.Map;

/**
 * Very small heap: allocates JavaObject and returns an integer reference id.
 */
public class Heap {
    private final Map<Integer, JavaObject> objects = new HashMap<>();
    private int nextId = 1;

    public synchronized int allocate(JavaObject obj) {
        int id = nextId++;
        objects.put(id, obj);
        return id;
    }

    public synchronized JavaObject get(int id) {
        return objects.get(id);
    }

    public synchronized void put(int id, JavaObject obj) {
        objects.put(id, obj);
    }

    public synchronized boolean exists(int id) {
        return objects.containsKey(id);
    }
}
