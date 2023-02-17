package com.example.application.data.entity;

public class ElaFavoriten {

    private Integer ID;
    private String BENUTZER_KENNUNG;
    private String NUTZER_ID;
    private String NAME;
    private String VORNAME;
    private String ORT;
    private String PLZ;
    private String STRASSE;
    private String HAUSNUMMER;
    private String ORGANISATION;
    private Integer VERSION;

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public String getBENUTZER_KENNUNG() {
        return BENUTZER_KENNUNG;
    }

    public void setBENUTZER_KENNUNG(String BENUTZER_KENNUNG) {
        this.BENUTZER_KENNUNG = BENUTZER_KENNUNG;
    }

    public String getNUTZER_ID() {
        return NUTZER_ID;
    }

    public void setNUTZER_ID(String NUTZER_ID) {
        this.NUTZER_ID = NUTZER_ID;
    }

    public String getNAME() {
        return NAME;
    }

    public void setNAME(String NAME) {
        this.NAME = NAME;
    }

    public String getVORNAME() {
        return VORNAME;
    }

    public void setVORNAME(String VORNAME) {
        this.VORNAME = VORNAME;
    }

    public String getORT() {
        return ORT;
    }

    public void setORT(String ORT) {
        this.ORT = ORT;
    }

    public String getPLZ() {
        return PLZ;
    }

    public void setPLZ(String PLZ) {
        this.PLZ = PLZ;
    }

    public String getSTRASSE() {
        return STRASSE;
    }

    public void setSTRASSE(String STRASSE) {
        this.STRASSE = STRASSE;
    }

    public String getHAUSNUMMER() {
        return HAUSNUMMER;
    }

    public void setHAUSNUMMER(String HAUSNUMMER) {
        this.HAUSNUMMER = HAUSNUMMER;
    }

    public String getORGANISATION() {
        return ORGANISATION;
    }

    public void setORGANISATION(String ORGANISATION) {
        this.ORGANISATION = ORGANISATION;
    }

    public Integer getVERSION() {
        return VERSION;
    }

    public void setVERSION(Integer VERSION) {
        this.VERSION = VERSION;
    }
}
