package com.example.application.data.entity;

public class Mailbox {

    private String NAME;
    private String COURT_ID;
    private Integer QUANTIFIER;
    private String USER_ID;
    private String TYP;
    private String KONVERTIERUNGSDIENSTE;
    private String MAX_MESSAGE_COUNT;

    private String DAYSTOEXPIRE;

    private String ROLEID;

    private String Status;

    public String getDAYSTOEXPIRE() {
        return DAYSTOEXPIRE;
    }

    public void setDAYSTOEXPIRE(String DAYSTOEXPIRE) {
        this.DAYSTOEXPIRE = DAYSTOEXPIRE;
    }

    public String getROLEID() {
        return ROLEID;
    }

    public void setROLEID(String ROLEID) {
        this.ROLEID = ROLEID;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public String getMAX_MESSAGE_COUNT() {
        return MAX_MESSAGE_COUNT;
    }

    public void setMAX_MESSAGE_COUNT(String MAX_MESSAGE_COUNT) {
        this.MAX_MESSAGE_COUNT = MAX_MESSAGE_COUNT;
    }

    private String in_egvp_wartend;
    private String aktuell_in_eKP_verarbeitet;
    private String in_ekp_haengend;
    private String in_ekp_warteschlange;
    private String in_ekp_fehlerhospital;

    public String getIn_egvp_wartend() {
        return in_egvp_wartend;
    }

    public void setIn_egvp_wartend(String in_egvp_wartend) {
        this.in_egvp_wartend = in_egvp_wartend;
    }

    public String getAktuell_in_eKP_verarbeitet() {
        return aktuell_in_eKP_verarbeitet;
    }

    public void setAktuell_in_eKP_verarbeitet(String aktuell_in_eKP_verarbeitet) {
        this.aktuell_in_eKP_verarbeitet = aktuell_in_eKP_verarbeitet;
    }

    public String getIn_ekp_haengend() {
        return in_ekp_haengend;
    }

    public void setIn_ekp_haengend(String in_ekp_haengend) {
        this.in_ekp_haengend = in_ekp_haengend;
    }

    public String getIn_ekp_warteschlange() {
        return in_ekp_warteschlange;
    }

    public void setIn_ekp_warteschlange(String in_ekp_warteschlange) {
        this.in_ekp_warteschlange = in_ekp_warteschlange;
    }

    public String getIn_ekp_fehlerhospital() {
        return in_ekp_fehlerhospital;
    }

    public void setIn_ekp_fehlerhospital(String in_ekp_fehlerhospital) {
        this.in_ekp_fehlerhospital = in_ekp_fehlerhospital;
    }

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
