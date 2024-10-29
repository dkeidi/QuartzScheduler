package com.quartz.util;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.Executors;

public class JobExecutor {
    public static void executeJob(String jobName, String scriptLocation, Logger LOG, String jobId, String logFileName) throws IOException, InterruptedException {
        LOG.info("Starting job with command: {}", jobName);

        Process process = Runtime.getRuntime().exec("cmd /c " + scriptLocation);

        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), LOG::info, jobId, jobName, logFileName);
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), LOG::error, jobId, jobName, logFileName);

        Executors.newSingleThreadExecutor().submit(outputGobbler);
        Executors.newSingleThreadExecutor().submit(errorGobbler);

        int exitCode = process.waitFor();
        if (exitCode == 0) {
//            LOG.info("Command executed successfully");
        } else {
            LOG.error("Error while executing the command. Exit code: {}", exitCode);
        }
    }
}
