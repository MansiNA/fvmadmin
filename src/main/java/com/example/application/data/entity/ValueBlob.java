package com.example.application.data.entity;

public class ValueBlob {
    private long ID;

    private String Name;

    private String Pfad;

    public ValueBlob(String name, String pfad, byte[] blob) {
        Name = name;
        Pfad = pfad;
        Blob = blob;
    }

    public ValueBlob(String name, String pfad) {
        Name = name;
        Pfad = pfad;
    }

    public ValueBlob() {}
    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getPfad() {
        return Pfad;
    }

    public void setPfad(String pfad) {
        Pfad = pfad;
    }

    private byte[] Blob;

    public long getID() {
        return ID;
    }

    public void setID(long ID) {
        this.ID = ID;
    }

    public byte[] getBlob() {
        return Blob;
    }

    public void setBlob(byte[] blob) {
        Blob = blob;
    }
    //private date TimeStampVersion;


}
