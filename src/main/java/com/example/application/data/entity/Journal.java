package com.example.application.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name ="Journal", schema="ekp")
public class Journal {
    @Id
    private Integer PID;

    String DDATE;

    String Dauer;

    public String getDauer() {
        return Dauer;
    }

    public void setDauer(String dauer) {
        Dauer = dauer;
    }

    String A;

    public Journal() {
    }

    public Journal(Integer PID, String DDATE, String a) {
        this.PID = PID;
        this.DDATE = DDATE;
        A = a;
    }

    public Integer getPID() {
        return PID;
    }

    public void setPID(Integer PID) {
        this.PID = PID;
    }

    public String getDDATE() {
        return DDATE;
    }

    public void setDDATE(String DDATE) {
        this.DDATE = DDATE;
    }

    public String getA() {
        return A;
    }

    public void setA(String a) {
        A = a;
    }
}
