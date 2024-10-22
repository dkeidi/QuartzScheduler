package com.quartz.util;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;

public class JobExecutor {
    public static void executeJob(String jobName, String scriptLocation, Logger LOG, String jobId) throws IOException, InterruptedException {
        LOG.info("Starting job with command: {}", jobName);
        LOG.info("Script Location: {}", scriptLocation);

        Process process = Runtime.getRuntime().exec("cmd /c " + scriptLocation);

        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), LOG::info, jobId);
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), LOG::error, jobId);

        Executors.newSingleThreadExecutor().submit(outputGobbler);
        Executors.newSingleThreadExecutor().submit(errorGobbler);

        int exitCode = process.waitFor();
        if (exitCode == 0) {
            LOG.info("Command executed successfully");
        } else {
            LOG.error("Error while executing the command. Exit code: {}", exitCode);
        }
    }
}
