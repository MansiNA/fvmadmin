package com.example.application.utils;

import com.example.application.data.entity.fvm_monitoring;

public interface myCallback {
    void onSuccess();
    void save(fvm_monitoring mon);
    void onError(String err);
}