package com.example.application.utils;

import com.example.application.data.entity.fvm_monitoring;

public interface myCallback {
    void cancel();
    void save(fvm_monitoring mon);
    void delete(fvm_monitoring mon);
}