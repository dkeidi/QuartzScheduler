package com.quartz.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private final String instanceId;

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer, String jobId, String jobName, String instanceId, String logFileName) {
        this.inputStream = inputStream;
        this.consumer = consumer;
        this.jobId = jobId;
        this.jobName = jobName;
        this.instanceId = instanceId;
        this.logFileName = logFileName;
    }

    @Override
    public void run() {
        if (jobId != null) {
            CustomLogger.initializeThreadContext(jobId, jobName, "child", "Executing", logFileName, null);
        }

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                consumer.accept(line);
            }
        } catch (Exception e) {
            CustomLogger.initializeThreadContext(jobId, jobName, "child", "Error", logFileName, e);
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }
}