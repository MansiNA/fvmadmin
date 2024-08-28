package com.example.application.data.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
//    public MonitorAlerting(Long id, String mailEmpfaenger, String mailCCEmpfaenger,
//                           String mailBetreff, String mailText, Integer intervall) {
//        this.id = id;
//        this.mailEmpfaenger = mailEmpfaenger;
//        this.mailCCEmpfaenger = mailCCEmpfaenger;
//        this.mailBetreff = mailBetreff;
//        this.mailText = mailText;
//        this.intervall = intervall;
//    }

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
