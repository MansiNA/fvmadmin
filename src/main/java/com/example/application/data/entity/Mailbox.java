package com.example.application.data.entity;

public class Mailbox {

    private String NAME;
    private String COURT_ID;
    private Integer QUANTIFIER;
    private String USER_ID;
    private String TYP;
    private String KONVERTIERUNGSDIENSTE;


    public String getNAME() {
        return NAME;
    }

    public void setNAME(String NAME) {
        this.NAME = NAME;
    }

    public String getCOURT_ID() {
        return COURT_ID;
    }

    public void setCOURT_ID(String COURT_ID) {
        this.COURT_ID = COURT_ID;
    }

    public Integer getQUANTIFIER() {
        return QUANTIFIER;
    }

    public void setQUANTIFIER(Integer QUANTIFIER) {
        this.QUANTIFIER = QUANTIFIER;
    }

    public String getUSER_ID() {
        return USER_ID;
    }

    public void setUSER_ID(String USER_ID) {
        this.USER_ID = USER_ID;
    }

    public String getTYP() {
        return TYP;
    }

    public void setTYP(String TYP) {
        this.TYP = TYP;
    }

    public String getKONVERTIERUNGSDIENSTE() {
        return KONVERTIERUNGSDIENSTE;
    }

    public void setKONVERTIERUNGSDIENSTE(String KONVERTIERUNGSDIENSTE) {
        this.KONVERTIERUNGSDIENSTE = KONVERTIERUNGSDIENSTE;
    }
}
