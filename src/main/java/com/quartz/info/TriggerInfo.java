package com.quartz.info;

import java.io.Serializable;

public class TriggerInfo implements Serializable {
    private boolean runForever; // if true, totalFireCount dont matter
    private int remainingFireCount;
    private String cronExp;
    private String callbackData;
    private String scriptLocation;

    public String getJobName() { return jobName; }

    public void setJobName(String jobName) { this.jobName = jobName; }

    private String jobName;

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
}