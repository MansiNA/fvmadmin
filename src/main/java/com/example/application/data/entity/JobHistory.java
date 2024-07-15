package com.example.application.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "JOB_HISTORY")
public class JobHistory {

    @Id
    @Column(name = "PROZESS_ID", nullable = false)
    private Long processId;

    @Column(name = "JOB_NAME", nullable = false, length = 255)
    private String jobName;

    @Column(name = "NAMESPACE", nullable = false, length = 255)
    private String namespace;

    @Column(name = "PARAMETER", nullable = false, length = 255)
    private String parameter;

    @Column(name = "START_TYPE", nullable = false, length = 255)
    private String startType;

    @Column(name = "MEMORY_USAGE", nullable = false, length = 255)
    private String memoryUsage;

    @Column(name = "START_TIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(name = "END_TIME", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    @Column(name = "RETURN_VALUE", nullable = false, length = 4000)
    private String returnValue;

    @Column(name = "EXIT_CODE", nullable = false)
    private Integer exitCode;

    @Override
    public String toString() {
        return "JobHistory{" +
                "processId=" + processId +
                ", jobName='" + jobName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", parameter='" + parameter + '\'' +
                ", startType='" + startType + '\'' +
                ", memoryUsage='" + memoryUsage + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", returnValue='" + returnValue + '\'' +
                ", exitCode=" + exitCode +
                '}';
    }
}
