package com.example.application.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(name ="Ablaufdaten", schema="ekp")
public class Ablaufdaten {

    @Id
    private Integer ID;
    @NotEmpty
    private Integer NACHRICHTIDINTERN;

    private String NAME;

    private String NAME_NLS;
    private String VERSION;
    private String INSTANZ;
    private String VORINSTANZ;
    private String TYP;
    private String KOMMENTAR;
    private String CONVERSATIONID;
    private String START_DATUM;
    private String ENDE_DATUM;
    private String MGLWEITERBEARBEITUNGSOPT;
    private String GWLWEITERBEARBEITUNGSOPT;
    private String WSAMESSAGEID;
    private String WSAADDRESS;
    private String TIMESTAMPVERSION;

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public Integer getNACHRICHTIDINTERN() {
        return NACHRICHTIDINTERN;
    }

    public void setNACHRICHTIDINTERN(Integer NACHRICHTIDINTERN) {
        this.NACHRICHTIDINTERN = NACHRICHTIDINTERN;
    }

    public String getNAME() {
        return NAME;
    }

    public void setNAME(String NAME) {
        this.NAME = NAME;
    }

    public String getNAME_NLS() {
        return NAME_NLS;
    }

    public void setNAME_NLS(String NAME_NLS) {
        this.NAME_NLS = NAME_NLS;
    }

    public String getVERSION() {
        return VERSION;
    }

    public void setVERSION(String VERSION) {
        this.VERSION = VERSION;
    }

    public String getINSTANZ() {
        return INSTANZ;
    }

    public void setINSTANZ(String INSTANZ) {
        this.INSTANZ = INSTANZ;
    }

    public String getVORINSTANZ() {
        return VORINSTANZ;
    }

    public void setVORINSTANZ(String VORINSTANZ) {
        this.VORINSTANZ = VORINSTANZ;
    }

    public String getTYP() {
        return TYP;
    }

    public void setTYP(String TYP) {
        this.TYP = TYP;
    }

    public String getKOMMENTAR() {
        return KOMMENTAR;
    }

    public void setKOMMENTAR(String KOMMENTAR) {
        this.KOMMENTAR = KOMMENTAR;
    }

    public String getCONVERSATIONID() {
        return CONVERSATIONID;
    }

    public void setCONVERSATIONID(String CONVERSATIONID) {
        this.CONVERSATIONID = CONVERSATIONID;
    }

    public String getSTART_DATUM() {
        return START_DATUM;
    }

    public void setSTART_DATUM(String START_DATUM) {
        this.START_DATUM = START_DATUM;
    }

    public String getENDE_DATUM() {
        return ENDE_DATUM;
    }

    public void setENDE_DATUM(String ENDE_DATUM) {
        this.ENDE_DATUM = ENDE_DATUM;
    }

    public String getMGLWEITERBEARBEITUNGSOPT() {
        return MGLWEITERBEARBEITUNGSOPT;
    }

    public void setMGLWEITERBEARBEITUNGSOPT(String MGLWEITERBEARBEITUNGSOPT) {
        this.MGLWEITERBEARBEITUNGSOPT = MGLWEITERBEARBEITUNGSOPT;
    }

    public String getGWLWEITERBEARBEITUNGSOPT() {
        return GWLWEITERBEARBEITUNGSOPT;
    }

    public void setGWLWEITERBEARBEITUNGSOPT(String GWLWEITERBEARBEITUNGSOPT) {
        this.GWLWEITERBEARBEITUNGSOPT = GWLWEITERBEARBEITUNGSOPT;
    }

    public String getWSAMESSAGEID() {
        return WSAMESSAGEID;
    }

    public void setWSAMESSAGEID(String WSAMESSAGEID) {
        this.WSAMESSAGEID = WSAMESSAGEID;
    }

    public String getWSAADDRESS() {
        return WSAADDRESS;
    }

    public void setWSAADDRESS(String WSAADDRESS) {
        this.WSAADDRESS = WSAADDRESS;
    }

    public String getTIMESTAMPVERSION() {
        return TIMESTAMPVERSION;
    }

    public void setTIMESTAMPVERSION(String TIMESTAMPVERSION) {
        this.TIMESTAMPVERSION = TIMESTAMPVERSION;
    }
}
