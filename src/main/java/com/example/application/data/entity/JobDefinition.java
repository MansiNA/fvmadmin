package com.example.application.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "JOB_DEFINITION")
public class JobDefinition implements Serializable {

    @Id
    private Integer id;
    private Integer pid;
    private String name;
    private String namespace;
    private String command;
    private String cron;
    private String typ;

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
