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
    private String jobGroup;
    private String triggerName;
    private String triggerGroup;
    private String jobDatetime;
    private long repeatIntervalMs = 0;
    private long initialOffsetMs = 0;
    private int repeatCount = 0;
    private Date nextFireTime;

    public String getJobName() { return jobName; }

    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getTriggerName() { return triggerName; }

    public void setTriggerName(String triggerName) { this.triggerName = triggerName; }

    public String getTriggerGroup() { return triggerGroup; }

    public void setTriggerGroup(String triggerGroup) { this.triggerGroup = triggerGroup; }

    public Date getNextFireTime() {
        return nextFireTime;
    }

    public void setNextFireTime(Date nextFireTime) { this.nextFireTime = nextFireTime; }

    public String getJobDatetime() { return jobDatetime; }

    public void setJobDatetime(String jobDatetime) {
//        System.out.println(jobDatetime);
        this.jobDatetime = jobDatetime; }

    public String getScriptLocation() { return scriptLocation; }

    public void setScriptLocation(String scriptLocation) { this.scriptLocation = scriptLocation; }

    public boolean isRunForever() { return runForever; }

    public void setRunForever(boolean runForever) { this.runForever = runForever; }

    public int getRemainingFireCount() { return remainingFireCount; }

    public void setRemainingFireCount(int remainingFireCount) { this.remainingFireCount = remainingFireCount; }

    public String getCronExp() { return cronExp; }

    public void setCronExp(String cronExp) { this.cronExp = cronExp; }

    public String getCallbackData() { return callbackData; }

    public void setCallbackData(String callbackData) { this.callbackData = callbackData; }

    public long getInitialOffsetMs() { return initialOffsetMs; }

    public void setInitialOffsetMs(long initialOffsetMs) { this.initialOffsetMs = initialOffsetMs; }

    public long getRepeatIntervalMs() { return repeatIntervalMs; }

    public void setRepeatIntervalMs(long repeatIntervalMs) { this.repeatIntervalMs = repeatIntervalMs; }

    public int getRepeatCount() { return repeatCount; }

    public void setRepeatCount(int repeatCount) { this.repeatCount = repeatCount; }
}