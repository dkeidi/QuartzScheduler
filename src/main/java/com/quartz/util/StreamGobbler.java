package com.quartz.util;

import org.apache.logging.log4j.ThreadContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class StreamGobbler implements Runnable {
    private final InputStream inputStream;
    private final Consumer<String> consumer;
    private String jobId; // Add jobId

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer, String jobId) {
        this.inputStream = inputStream;
        this.consumer = consumer;
        this.jobId = jobId; // Initialize jobId
    }


    @Override
    public void run() {
        if (jobId != null) {
            ThreadContext.put("jobId", jobId); // Set jobId in ThreadContext
        }

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                consumer.accept(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}