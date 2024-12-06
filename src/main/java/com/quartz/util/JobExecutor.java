package com.quartz.util;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JobExecutor {

    private static final int MAX_RETRIES = 3; // Maximum retry attempts
    private static final int RETRY_DELAY_MS = 5000; // Delay between retries in milliseconds

    public static void executeJob(String scriptLocation, String masterScriptLocation, Boolean isServerScript,
                                  Logger LOG, String jobId, String jobName, String instanceId, String logFileName)
            throws IOException, InterruptedException {
        LOG.info("Starting job with command: {}", jobName);

        int retries = 0;
        boolean success = false;

        while (retries < MAX_RETRIES && !success) {
            try {
                // Run master script if the job script is on a network location
                if (isServerScript) {
                    executeScript(masterScriptLocation, LOG, jobId, jobName, instanceId, logFileName);
                }

                // Run the main job script
                executeScript(scriptLocation, LOG, jobId, jobName, instanceId, logFileName);
                success = true; // If execution succeeds, exit loop
            } catch (IOException | InterruptedException e) {
                retries++;
                LOG.error("Execution failed, attempt {} of {}", retries, MAX_RETRIES, e);

                if (retries < MAX_RETRIES) {
                    LOG.info("Retrying in {} seconds...", RETRY_DELAY_MS / 1000);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ex) {
                        LOG.warn("Retry delay interrupted", ex);
                        Thread.currentThread().interrupt();
                        throw ex; // Rethrow if interrupted
                    }
                } else {
                    LOG.error("All retry attempts failed. Giving up.");
                    throw e; // Throw the exception if retries are exhausted
                }
            }
        }
    }

    private static void executeScript(String scriptLocation, Logger LOG, String jobId, String jobName, String instanceId, String logFileName) throws IOException, InterruptedException {
        LOG.info("Executing script: {}", scriptLocation);
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
            throw new IOException("Script execution failed with exit code " + exitCode);
        }

        // Ensure executor is properly shut down after use
        executor.shutdown();
    }
}
