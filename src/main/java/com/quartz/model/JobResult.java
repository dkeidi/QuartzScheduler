package com.quartz.model;

import com.quartz.info.TriggerInfo;

public class JobResult {
    private final TriggerInfo triggerInfo;
    private final boolean success;
    private final String message;

    public JobResult(TriggerInfo triggerInfo, boolean success) {
        this.triggerInfo = triggerInfo;
        this.success = success;
        this.message = success ? "Job scheduled successfully" : "Failed to schedule job";
    }

    public TriggerInfo getTriggerInfo() {
        return triggerInfo;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}