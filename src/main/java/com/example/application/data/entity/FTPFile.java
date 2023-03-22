package com.example.application.data.entity;

import java.time.LocalDateTime;

public class FTPFile {
    String Name;
    Long Size;
    LocalDateTime Erstellungszeit;

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public Long getSize() {
        return Size;
    }

    public void setSize(Long size) {
        Size = size;
    }

    public LocalDateTime getErstellungszeit() {
        return Erstellungszeit;
    }

    public void setErstellungszeit(LocalDateTime erstellungszeit) {
        Erstellungszeit = erstellungszeit;
    }
}
