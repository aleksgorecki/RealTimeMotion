package com.example.realtimemotion;

import java.util.LinkedList;

public class EvictingQueue<E> extends LinkedList<E> {

    private final int capacity;

    public EvictingQueue(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean add(E e) {
        boolean ret = super.add(e);
        if (size() > capacity) {
            removeFirst();
        }
        return ret;
    }
}
