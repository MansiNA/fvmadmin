package com.example.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MessageService {
    private static final List<String> messages = new ArrayList<>();
    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public static synchronized void addMessage(String message) {
        messages.add(message);
        notifyListeners(message);
    }

    public static synchronized List<String> getMessages() {
        List<String> copy = new ArrayList<>(messages);
        messages.clear();
        return copy;
    }

    public static void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public static void removeListener(Consumer<String> listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners(String message) {
        System.out.println("");
        for (Consumer<String> listener : listeners) {
            listener.accept(message);
        }
    }
}


