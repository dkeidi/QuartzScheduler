package com.quartz.util;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JobExecutor {
    public static void executeJob(String scriptLocation, String masterScriptLocation, Logger LOG, String jobId, String jobName, String instanceId, String logFileName) throws IOException, InterruptedException {
        LOG.info("Starting job with command: {}", jobName);

        // Run master script if the job script is on a network location
        if (scriptLocation.startsWith("Z:")) {
            executeScript(masterScriptLocation, LOG, jobId, jobName, instanceId, logFileName);
        }

        // Run the main job script
        executeScript(scriptLocation, LOG, jobId, jobName, instanceId, logFileName);
    }

    private static void executeScript(String scriptLocation, Logger LOG, String jobId, String jobName, String instanceId, String logFileName) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("cmd /c " + scriptLocation);

        // Set up StreamGobblers for output and error streams
        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), LOG::info, jobId, jobName, instanceId, logFileName);
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), LOG::error, jobId, jobName, instanceId, logFileName);

        // Use a fixed executor for concurrent execution of gobblers
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(outputGobbler);
        executor.submit(errorGobbler);

        // Wait for the process to complete and log the result
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            LOG.info("Command executed successfully: {}", scriptLocation);
        } else {
            LOG.error("Error while executing the command. Exit code: {}", exitCode);
        }

        // Ensure executor is properly shut down after use
        executor.shutdown();
    }
}
