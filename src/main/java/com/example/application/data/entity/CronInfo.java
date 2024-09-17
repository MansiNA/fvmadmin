package com.example.application.data.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class CronInfo {

    private String jobName;
    private String jobGroup;
    private Date nextFireTime;

    // Constructor
    public CronInfo(String jobName, String jobGroup, Date nextFireTime) {
        this.jobName = jobName;
        this.jobGroup = jobGroup;
        this.nextFireTime = nextFireTime;
    }

    // toString method for easy logging or debugging
    @Override
    public String toString() {
        return "CronInfo{" +
                "jobName='" + jobName + '\'' +
                ", jobGroup='" + jobGroup + '\'' +
                ", nextFireTime=" + nextFireTime +
                '}';
    }
}
