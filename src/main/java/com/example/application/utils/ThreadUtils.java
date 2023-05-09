package com.example.application.utils;

public final class ThreadUtils {
    public static void dumpThreads(){
        final ThreadGroup group = Thread.currentThread().getThreadGroup();
        final int activeCount = group.activeCount();
        final Thread[] threads = new Thread[activeCount];
        group.enumerate(threads);

        System.out.println("Thread-Group " + group + " contains " + activeCount + " threads");

        for (final Thread thread : threads){
            System.out.println("Thread " + thread);
        }

    }
}
