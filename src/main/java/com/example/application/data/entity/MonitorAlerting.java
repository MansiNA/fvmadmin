package com.example.application.data.entity;

public class MonitorAlerting {

    private Long id;
    private String mailEmpfaenger;
    private String mailCCEmpfaenger;
    private String mailBetreff;
    private String mailText;
    private Integer intervall;

    // Default constructor
    public MonitorAlerting() {}

    // Parameterized constructor
    public MonitorAlerting(Long id, String mailEmpfaenger, String mailCCEmpfaenger,
                           String mailBetreff, String mailText, Integer intervall) {
        this.id = id;
        this.mailEmpfaenger = mailEmpfaenger;
        this.mailCCEmpfaenger = mailCCEmpfaenger;
        this.mailBetreff = mailBetreff;
        this.mailText = mailText;
        this.intervall = intervall;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMailEmpfaenger() {
        return mailEmpfaenger;
    }

    public void setMailEmpfaenger(String mailEmpfaenger) {
        this.mailEmpfaenger = mailEmpfaenger;
    }

    public String getMailCCEmpfaenger() {
        return mailCCEmpfaenger;
    }

    public void setMailCCEmpfaenger(String mailCCEmpfaenger) {
        this.mailCCEmpfaenger = mailCCEmpfaenger;
    }

    public String getMailBetreff() {
        return mailBetreff;
    }

    public void setMailBetreff(String mailBetreff) {
        this.mailBetreff = mailBetreff;
    }

    public String getMailText() {
        return mailText;
    }

    public void setMailText(String mailText) {
        this.mailText = mailText;
    }

    public Integer getIntervall() {
        return intervall;
    }

    public void setIntervall(Integer intervall) {
        this.intervall = intervall;
    }

    @Override
    public String toString() {
        return "MonitorAlerting{" +
                "id=" + id +
                ", mailEmpfaenger='" + mailEmpfaenger + '\'' +
                ", mailCCEmpfaenger='" + mailCCEmpfaenger + '\'' +
                ", mailBetreff='" + mailBetreff + '\'' +
                ", mailText='" + mailText + '\'' +
                ", intervall=" + intervall +
                '}';
    }
}
