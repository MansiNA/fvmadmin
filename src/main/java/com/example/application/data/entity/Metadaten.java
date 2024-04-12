package com.example.application.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;

@Entity
@Table(name ="Metadaten", schema="ekp")
public class Metadaten {

    @Id
    private Integer ID;

    @NotEmpty
    private Integer NACHRICHTIDINTERN;

    private String NACHRICHTIDEXTERN;

    private String STATUS;

    private String NACHRICHTTYP;
    private String TRANSPORTART;
    private String TRANSPORTVERSION;
    private String ART;
    private String SENDER;
    private String SENDERAKTENZEICHEN;
    private String SENDERGOVELLOID;
    private String SENDERPOSTFACHNAME;
    private String SENDERGESCHAEFTSZEICHEN;
    private String EMPFAENGER;
    private String EMPFAENGERAKTENZEICHEN;
    private String EMPFAENGERGOVELLOID;
    private String EMPFAENGERPOSTFACHNAME;
    private String WEITERLEITUNGGOVELLOID;
    private String WEITERLEITUNGPOSTFACHNAME;
    private String BETREFF;
    private String BEMERKUNG;
    private String ERSTELLUNGSDATUM;
    private String ABHOLDATUM;
    private String EINGANGSDATUMSERVER;
    private String VERFALLSDATUM;
    private String SIGNATURPRUEFUNGSDATUM;
    private String VALIDIERUNGSDATUM;
    private String SIGNATURSTATUS;
    private String FACHVERFAHREN;
    private String FACHBEREICH;
    private String SACHGEBIET;
    private String ABTEILUNGE1;
    private String ABTEILUNGE2;
    private String PRIO;
    private String XJUSTIZVERSION;
    private String MANUELLBEARBEITETFLAG;
    private String BEARBEITERNAME;
    private String BEARBEITERKENNUNG;
    private String FEHLERTAG;
    private String PAPIERVORGANG;
    private String VERARBEITET;
    private String LOESCHTAG;
    private String SENDERROLLEN;
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

    public String getNACHRICHTIDEXTERN() {
        return NACHRICHTIDEXTERN;
    }

    public void setNACHRICHTIDEXTERN(String NACHRICHTIDEXTERN) {
        this.NACHRICHTIDEXTERN = NACHRICHTIDEXTERN;
    }

    public String getSTATUS() {
        return STATUS;
    }

    public void setSTATUS(String STATUS) {
        this.STATUS = STATUS;
    }

    public String getNACHRICHTTYP() {
        return NACHRICHTTYP;
    }

    public void setNACHRICHTTYP(String NACHRICHTTYP) {
        this.NACHRICHTTYP = NACHRICHTTYP;
    }

    public String getTRANSPORTART() {
        return TRANSPORTART;
    }

    public void setTRANSPORTART(String TRANSPORTART) {
        this.TRANSPORTART = TRANSPORTART;
    }

    public String getTRANSPORTVERSION() {
        return TRANSPORTVERSION;
    }

    public void setTRANSPORTVERSION(String TRANSPORTVERSION) {
        this.TRANSPORTVERSION = TRANSPORTVERSION;
    }

    public String getART() {
        return ART;
    }

    public void setART(String ART) {
        this.ART = ART;
    }

    public String getSENDER() {
        return SENDER;
    }

    public void setSENDER(String SENDER) {
        this.SENDER = SENDER;
    }

    public String getSENDERAKTENZEICHEN() {
        return SENDERAKTENZEICHEN;
    }

    public void setSENDERAKTENZEICHEN(String SENDERAKTENZEICHEN) {
        this.SENDERAKTENZEICHEN = SENDERAKTENZEICHEN;
    }

    public String getSENDERGOVELLOID() {
        return SENDERGOVELLOID;
    }

    public void setSENDERGOVELLOID(String SENDERGOVELLOID) {
        this.SENDERGOVELLOID = SENDERGOVELLOID;
    }

    public String getSENDERPOSTFACHNAME() {
        return SENDERPOSTFACHNAME;
    }

    public void setSENDERPOSTFACHNAME(String SENDERPOSTFACHNAME) {
        this.SENDERPOSTFACHNAME = SENDERPOSTFACHNAME;
    }

    public String getSENDERGESCHAEFTSZEICHEN() {
        return SENDERGESCHAEFTSZEICHEN;
    }

    public void setSENDERGESCHAEFTSZEICHEN(String SENDERGESCHAEFTSZEICHEN) {
        this.SENDERGESCHAEFTSZEICHEN = SENDERGESCHAEFTSZEICHEN;
    }

    public String getEMPFAENGER() {
        return EMPFAENGER;
    }

    public void setEMPFAENGER(String EMPFAENGER) {
        this.EMPFAENGER = EMPFAENGER;
    }

    public String getEMPFAENGERAKTENZEICHEN() {
        return EMPFAENGERAKTENZEICHEN;
    }

    public void setEMPFAENGERAKTENZEICHEN(String EMPFAENGERAKTENZEICHEN) {
        this.EMPFAENGERAKTENZEICHEN = EMPFAENGERAKTENZEICHEN;
    }

    public String getEMPFAENGERGOVELLOID() {
        return EMPFAENGERGOVELLOID;
    }

    public void setEMPFAENGERGOVELLOID(String EMPFAENGERGOVELLOID) {
        this.EMPFAENGERGOVELLOID = EMPFAENGERGOVELLOID;
    }

    public String getEMPFAENGERPOSTFACHNAME() {
        return EMPFAENGERPOSTFACHNAME;
    }

    public void setEMPFAENGERPOSTFACHNAME(String EMPFAENGERPOSTFACHNAME) {
        this.EMPFAENGERPOSTFACHNAME = EMPFAENGERPOSTFACHNAME;
    }

    public String getWEITERLEITUNGGOVELLOID() {
        return WEITERLEITUNGGOVELLOID;
    }

    public void setWEITERLEITUNGGOVELLOID(String WEITERLEITUNGGOVELLOID) {
        this.WEITERLEITUNGGOVELLOID = WEITERLEITUNGGOVELLOID;
    }

    public String getWEITERLEITUNGPOSTFACHNAME() {
        return WEITERLEITUNGPOSTFACHNAME;
    }

    public void setWEITERLEITUNGPOSTFACHNAME(String WEITERLEITUNGPOSTFACHNAME) {
        this.WEITERLEITUNGPOSTFACHNAME = WEITERLEITUNGPOSTFACHNAME;
    }

    public String getBETREFF() {
        return BETREFF;
    }

    public void setBETREFF(String BETREFF) {
        this.BETREFF = BETREFF;
    }

    public String getBEMERKUNG() {
        return BEMERKUNG;
    }

    public void setBEMERKUNG(String BEMERKUNG) {
        this.BEMERKUNG = BEMERKUNG;
    }

    public String getERSTELLUNGSDATUM() {
        return ERSTELLUNGSDATUM;
    }

    public void setERSTELLUNGSDATUM(String ERSTELLUNGSDATUM) {
        this.ERSTELLUNGSDATUM = ERSTELLUNGSDATUM;
    }

    public String getABHOLDATUM() {
        return ABHOLDATUM;
    }

    public void setABHOLDATUM(String ABHOLDATUM) {
        this.ABHOLDATUM = ABHOLDATUM;
    }

    public String getEINGANGSDATUMSERVER() {
        return EINGANGSDATUMSERVER;
    }

    public void setEINGANGSDATUMSERVER(String EINGANGSDATUMSERVER) {
        this.EINGANGSDATUMSERVER = EINGANGSDATUMSERVER;
    }

    public String getVERFALLSDATUM() {
        return VERFALLSDATUM;
    }

    public void setVERFALLSDATUM(String VERFALLSDATUM) {
        this.VERFALLSDATUM = VERFALLSDATUM;
    }

    public String getSIGNATURPRUEFUNGSDATUM() {
        return SIGNATURPRUEFUNGSDATUM;
    }

    public void setSIGNATURPRUEFUNGSDATUM(String SIGNATURPRUEFUNGSDATUM) {
        this.SIGNATURPRUEFUNGSDATUM = SIGNATURPRUEFUNGSDATUM;
    }

    public String getVALIDIERUNGSDATUM() {
        return VALIDIERUNGSDATUM;
    }

    public void setVALIDIERUNGSDATUM(String VALIDIERUNGSDATUM) {
        this.VALIDIERUNGSDATUM = VALIDIERUNGSDATUM;
    }

    public String getSIGNATURSTATUS() {
        return SIGNATURSTATUS;
    }

    public void setSIGNATURSTATUS(String SIGNATURSTATUS) {
        this.SIGNATURSTATUS = SIGNATURSTATUS;
    }

    public String getFACHVERFAHREN() {
        return FACHVERFAHREN;
    }

    public void setFACHVERFAHREN(String FACHVERFAHREN) {
        this.FACHVERFAHREN = FACHVERFAHREN;
    }

    public String getFACHBEREICH() {
        return FACHBEREICH;
    }

    public void setFACHBEREICH(String FACHBEREICH) {
        this.FACHBEREICH = FACHBEREICH;
    }

    public String getSACHGEBIET() {
        return SACHGEBIET;
    }

    public void setSACHGEBIET(String SACHGEBIET) {
        this.SACHGEBIET = SACHGEBIET;
    }

    public String getABTEILUNGE1() {
        return ABTEILUNGE1;
    }

    public void setABTEILUNGE1(String ABTEILUNGE1) {
        this.ABTEILUNGE1 = ABTEILUNGE1;
    }

    public String getABTEILUNGE2() {
        return ABTEILUNGE2;
    }

    public void setABTEILUNGE2(String ABTEILUNGE2) {
        this.ABTEILUNGE2 = ABTEILUNGE2;
    }

    public String getPRIO() {
        return PRIO;
    }

    public void setPRIO(String PRIO) {
        this.PRIO = PRIO;
    }

    public String getXJUSTIZVERSION() {
        return XJUSTIZVERSION;
    }

    public void setXJUSTIZVERSION(String XJUSTIZVERSION) {
        this.XJUSTIZVERSION = XJUSTIZVERSION;
    }

    public String getMANUELLBEARBEITETFLAG() {
        return MANUELLBEARBEITETFLAG;
    }

    public void setMANUELLBEARBEITETFLAG(String MANUELLBEARBEITETFLAG) {
        this.MANUELLBEARBEITETFLAG = MANUELLBEARBEITETFLAG;
    }

    public String getBEARBEITERNAME() {
        return BEARBEITERNAME;
    }

    public void setBEARBEITERNAME(String BEARBEITERNAME) {
        this.BEARBEITERNAME = BEARBEITERNAME;
    }

    public String getBEARBEITERKENNUNG() {
        return BEARBEITERKENNUNG;
    }

    public void setBEARBEITERKENNUNG(String BEARBEITERKENNUNG) {
        this.BEARBEITERKENNUNG = BEARBEITERKENNUNG;
    }

    public String getFEHLERTAG() {
        return FEHLERTAG;
    }

    public void setFEHLERTAG(String FEHLERTAG) {
        this.FEHLERTAG = FEHLERTAG;
    }

    public String getPAPIERVORGANG() {
        return PAPIERVORGANG;
    }

    public void setPAPIERVORGANG(String PAPIERVORGANG) {
        this.PAPIERVORGANG = PAPIERVORGANG;
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

    public String getSENDERROLLEN() {
        return SENDERROLLEN;
    }

    public void setSENDERROLLEN(String SENDERROLLEN) {
        this.SENDERROLLEN = SENDERROLLEN;
    }

    public String getTIMESTAMPVERSION() {
        return TIMESTAMPVERSION;
    }

    public void setTIMESTAMPVERSION(String TIMESTAMPVERSION) {
        this.TIMESTAMPVERSION = TIMESTAMPVERSION;
    }
}
