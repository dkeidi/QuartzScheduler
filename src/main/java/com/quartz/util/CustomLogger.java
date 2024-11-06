package com.quartz.util;

import org.apache.logging.log4j.ThreadContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomLogger {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static void initializeThreadContext(String jobId, String jobName, String instanceId, String jobStatus, String logFileName, Exception e) {
        ThreadContext.put("jobId", jobId);
        ThreadContext.put("jobName", jobName);
        ThreadContext.put("logTime", getCurrentTime());
        ThreadContext.put("instanceId", instanceId);
        ThreadContext.put("status", jobStatus);
        ThreadContext.put("logFileName", logFileName);
        ThreadContext.put("msg", String.valueOf(e));
    }

    public static String getCurrentTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    public static String getCurrentDate() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }

}
