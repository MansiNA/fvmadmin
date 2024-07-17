package com.example.application.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "JOB_DEFINITION")
public class JobManager implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer pid;
    private String name;
    private String namespace;
    private String command;
    private String cron;
    private String typ;
    private String parameter;

    @Transient  // Indicates that this field is not persistent
    private Integer exitCode;
    @Override
    public String toString() {
        return "JobDefinition{" +
                "id=" + id +
                ", pid=" + pid +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", command='" + command + '\'' +
                ", cron='" + cron + '\'' +
                ", typ='" + typ + '\'' +
                '}';
    }
}
