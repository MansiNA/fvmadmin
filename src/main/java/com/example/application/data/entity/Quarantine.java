package com.example.application.data.entity;

public class Quarantine {

    private String Tag;
    private String ExceptionCode;
    private Integer Anzahl;

    public Quarantine(String tag, String exceptioncode, int anzahl) {
    }

    public Quarantine() {
    }

    public String getTag() {
        return Tag;
    }

    public void setTag(String tag) {
        Tag = tag;
    }

    public String getExceptionCode() {
        return ExceptionCode;
    }

    public void setExceptionCode(String exceptionCode) {
        ExceptionCode = exceptionCode;
    }

    public Integer getAnzahl() {
        return Anzahl;
    }

    public void setAnzahl(Integer anzahl) {
        Anzahl = anzahl;
    }
}
