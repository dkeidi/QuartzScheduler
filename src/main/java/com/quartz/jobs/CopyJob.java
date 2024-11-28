package com.quartz.jobs;

import com.quartz.util.CustomLogger;
import com.quartz.util.JobExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;


@Component
public class CopyJob implements Job {

    private static final Logger LOG = LogManager.getLogger(CopyJob.class);

    // Constants
    private static final String COMMAND = "C:\\Users\\keidi.tay.chuan\\Documents\\MyQuartzTest\\batch_files\\copy_file.bat";
    private static final String MASTER_COMMAND = "C:\\Users\\keidi.tay.chuan\\Documents\\MyQuartzTest\\batch_files\\map_drive.bat";
    private static final String JOB_NAME = "CopyJob";
    String LOG_FILENAME = JOB_NAME + "/" + CustomLogger.getCurrentDate() + ".log";
    Boolean isNetworkLocation = false;

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 5000;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Scheduler scheduler = context.getScheduler();
        String jobId = "";
        String instanceId = "";

        try {
            jobId = UUID.randomUUID().toString();  // Generate a UUID for job
            instanceId = scheduler.getSchedulerInstanceId();

            CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executing", LOG_FILENAME, null);

            int retries = 0;
            boolean success = false;

            while (retries < MAX_RETRIES && !success) {
                try {
                    _executeJob(jobId, context, instanceId);
                    success = true;  // If the job completes successfully, exit loop
                } catch (IOException | InterruptedException e) {
                    retries++;
                    LOG.error("Job failed, attempt " + retries + " of " + MAX_RETRIES, e);

                    if (retries < MAX_RETRIES) {
                        LOG.info("Retrying in " + RETRY_DELAY_MS / 1000 + " seconds...");
                        Thread.sleep(RETRY_DELAY_MS);  // Delay before retry
                    } else {
                        throw new JobExecutionException("Job failed after " + MAX_RETRIES + " attempts", e);
                    }
                }
            }

        } catch (Exception e) {
            CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Error", LOG_FILENAME, e);
            LOG.error(e);

            throw new JobExecutionException(e);
        } finally {
            ThreadContext.clearAll();
        }
    }

    private void _executeJob(String jobId, JobExecutionContext context, String instanceId) throws IOException, InterruptedException, JobExecutionException {
        CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executing", LOG_FILENAME, null);

        JobExecutor.executeJob(COMMAND, MASTER_COMMAND, isNetworkLocation, LOG, jobId, context.getJobDetail().getKey().getName(), instanceId, LOG_FILENAME);

        CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executed", LOG_FILENAME, null);
        LOG.info("Job completed successfully");
    }
}
