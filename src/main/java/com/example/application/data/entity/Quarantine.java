package com.example.application.data.entity;

public class Quarantine {

    private String ID;

    private Integer NACHRICHTIDINTERN;

    private String ENTRANCEDATE;

    private String CREATIONDATE;

    private String POBOX;

    private String EXCEPTIONCODE;
    private String RECEIVERID;

    private String RECEIVERNAME;

    private String SENDERID;

    private String SENDERNAME;

    private String ART;

    private String FEHLERTAG;

    private String VERARBEITET;

    private String LOESCHTAG;

    public Quarantine() {
    }

    public Quarantine(String ID, String ENTRANCEDATE, String CREATIONDATE, String POBOX, String EXCEPTIONCODE, String RECEIVERID, String RECEIVERNAME, String SENDERID, String SENDERNAME, String ART, String FEHLERTAG, String VERARBEITET, String LOESCHTAG) {
        this.ID = ID;
        this.ENTRANCEDATE = ENTRANCEDATE;
        this.CREATIONDATE = CREATIONDATE;
        this.POBOX = POBOX;
        this.EXCEPTIONCODE = EXCEPTIONCODE;
        this.RECEIVERID = RECEIVERID;
        this.RECEIVERNAME = RECEIVERNAME;
        this.SENDERID = SENDERID;
        this.SENDERNAME = SENDERNAME;
        this.ART = ART;
        this.FEHLERTAG = FEHLERTAG;
        this.VERARBEITET = VERARBEITET;
        this.LOESCHTAG = LOESCHTAG;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public Integer getNACHRICHTIDINTERN() {
        return NACHRICHTIDINTERN;
    }

    public void setNACHRICHTIDINTERN(Integer NACHRICHTIDINTERN) {
        this.NACHRICHTIDINTERN = NACHRICHTIDINTERN;
    }

    public String getENTRANCEDATE() {
        return ENTRANCEDATE;
    }

    public void setENTRANCEDATE(String ENTRANCEDATE) {
        this.ENTRANCEDATE = ENTRANCEDATE;
    }

    public String getCREATIONDATE() {
        return CREATIONDATE;
    }

    public void setCREATIONDATE(String CREATIONDATE) {
        this.CREATIONDATE = CREATIONDATE;
    }

    public String getPOBOX() {
        return POBOX;
    }

    public void setPOBOX(String POBOX) {
        this.POBOX = POBOX;
    }

    public String getEXCEPTIONCODE() {
        return EXCEPTIONCODE;
    }

    public void setEXCEPTIONCODE(String EXCEPTIONCODE) {
        this.EXCEPTIONCODE = EXCEPTIONCODE;
    }

    public String getRECEIVERID() {
        return RECEIVERID;
    }

    public void setRECEIVERID(String RECEIVERID) {
        this.RECEIVERID = RECEIVERID;
    }

    public String getRECEIVERNAME() {
        return RECEIVERNAME;
    }

    public void setRECEIVERNAME(String RECEIVERNAME) {
        this.RECEIVERNAME = RECEIVERNAME;
    }

    public String getSENDERID() {
        return SENDERID;
    }

    public void setSENDERID(String SENDERID) {
        this.SENDERID = SENDERID;
    }

    public String getSENDERNAME() {
        return SENDERNAME;
    }

    public void setSENDERNAME(String SENDERNAME) {
        this.SENDERNAME = SENDERNAME;
    }

    public String getART() {
        return ART;
    }

    public void setART(String ART) {
        this.ART = ART;
    }

    public String getFEHLERTAG() {
        return FEHLERTAG;
    }

    public void setFEHLERTAG(String FEHLERTAG) {
        this.FEHLERTAG = FEHLERTAG;
    }

    public String getVERARBEITET() {
        return VERARBEITET;
    }

    public void setVERARBEITET(String VERARBEITET) {
        this.VERARBEITET = VERARBEITET;
    }

    public String getLOESCHTAG() {
        return LOESCHTAG;
    }

    public void setLOESCHTAG(String LOESCHTAG) {
        this.LOESCHTAG = LOESCHTAG;
    }
}
