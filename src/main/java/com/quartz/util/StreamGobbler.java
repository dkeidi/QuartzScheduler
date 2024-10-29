package com.quartz.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class StreamGobbler implements Runnable {

    private static final Logger LOG = LogManager.getLogger(StreamGobbler.class);

    private final InputStream inputStream;
    private final Consumer<String> consumer;
    private final String jobId;
    private final String jobName;
    private final String logFileName;

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer, String jobId, String jobName, String logFileName) {
        this.inputStream = inputStream;
        this.consumer = consumer;
        this.jobId = jobId;
        this.jobName = jobName;
        this.logFileName = logFileName;
    }

    @Override
    public void run() {
        if (jobId != null) {
            _initializeThreadContext(jobId, null);
        }

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                consumer.accept(line);
            }
        } catch (Exception e) {
            _initializeThreadContext(jobId, e);
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }

    private void _initializeThreadContext(String jobId, Exception e) {
        ThreadContext.put("status", "Executing");
        ThreadContext.put("jobId", jobId); // Set jobId in ThreadContext
        ThreadContext.put("jobName", jobName);
        ThreadContext.put("instanceId", "child");
        ThreadContext.put("msg", String.valueOf(e));
        ThreadContext.put("logFileName", logFileName);
    }
}