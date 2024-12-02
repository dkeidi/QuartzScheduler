package com.quartz.info;

import java.io.Serializable;
import java.util.Date;

public class TriggerInfo implements Serializable {
    private boolean runForever; // if true, totalFireCount dont matter
    private int remainingFireCount;
    private String cronExp;
    private String callbackData;
    private String scriptLocation;
    private String jobName;

    private long repeatIntervalMs;

    private long initialOffsetMs;

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public Date getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(Date nextFireTime) {
        this.nextFireTime = nextFireTime;
    }

    private String jobGroup;
    private Date nextFireTime;

    public String getJobName() { return jobName; }

    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getScriptLocation() {
        return scriptLocation;
    }

    public void setScriptLocation(String scriptLocation) {
        this.scriptLocation = scriptLocation;
    }

    public boolean isRunForever() {
        return runForever;
    }

    public void setRunForever(boolean runForever) {
        this.runForever = runForever;
    }

    public int getRemainingFireCount() {
        return remainingFireCount;
    }

    public void setRemainingFireCount(int remainingFireCount) {
        this.remainingFireCount = remainingFireCount;
    }

    public String getCronExp() {
        return cronExp;
    }

    public void setCronExp(String cronExp) {
        this.cronExp = cronExp;
    }

    public String getCallbackData() {
        return callbackData;
    }

    public void setCallbackData(String callbackData) {
        this.callbackData = callbackData;
    }

    public long getInitialOffsetMs() { return initialOffsetMs; }

    public void setInitialOffsetMs(long initialOffsetMs) { this.initialOffsetMs = initialOffsetMs; }

    public long getRepeatIntervalMs() { return repeatIntervalMs; }

    public void setRepeatIntervalMs(long repeatIntervalMs) { this.repeatIntervalMs = repeatIntervalMs; }
}