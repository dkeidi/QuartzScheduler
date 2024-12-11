package com.quartz.jobs;

import com.quartz.util.CustomLogger;
import com.quartz.util.JobExecutor;
import com.quartz.util.JobUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.scheduling.quartz.QuartzJobBean;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.UUID;

public class BatchJob extends QuartzJobBean {
    public String JOB_NAME = "";
    String LOG_FILENAME = JOB_NAME + "/" + CustomLogger.getCurrentDate() + ".log";

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 5000;

    private final DataSource dataSource;

    @Autowired
    public BatchJob(@QuartzDataSource DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Scheduler scheduler = context.getScheduler();
        String jobId = "";
        String instanceId = "";
        JOB_NAME = context.getJobDetail().getKey().getName();

        String scriptLocation = context.getJobDetail().getJobDataMap().getString("command");
        String masterScriptLocation = context.getJobDetail().getJobDataMap().getString("master_command");
        Boolean isServerScript = context.getJobDetail().getJobDataMap().getBoolean("is_server_script");

        String jobName = context.getJobDetail().getKey().getName();
        Logger LOG = LogManager.getLogger("com.quartz.jobs." + jobName);
        LOG.info("Job Name: {}", jobName);

        char isDeleted = getJobDeletionStatus(jobName, context.getJobDetail().getKey().getGroup());
        if (isDeleted == 'Y') {
            LOG.info("Job has been deleted and will not be run.");
            return;
        }

        try {
            jobId = UUID.randomUUID().toString();  // Generate a UUID for job
            instanceId = scheduler.getSchedulerInstanceId();
            CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executing", LOG_FILENAME, null);

            int retries = 0;
            boolean success = false;

            while (retries < MAX_RETRIES && !success) {
                try {
                    _executeJob(scriptLocation, masterScriptLocation, isServerScript, LOG, jobId, context.getJobDetail().getKey().getName(), instanceId);
                    success = true;  // If the job completes successfully, exit loop
                } catch (IOException | InterruptedException e) {
                    retries++;
                    LOG.error("Job failed, attempt {} of " + MAX_RETRIES, retries, e);

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

    private void _executeJob(String scriptLocation, String masterScriptLocation, Boolean isServerScript, Logger LOG, String jobId, String jobName, String instanceId) throws IOException, InterruptedException {
        CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executing", LOG_FILENAME, null);

        JobExecutor.executeJob(scriptLocation, masterScriptLocation, isServerScript, LOG, jobId, jobName, instanceId, LOG_FILENAME);

        CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executed", LOG_FILENAME, null);
        LOG.info("Job completed successfully");
    }

    private char getJobDeletionStatus(String jobName, String jobGroup) {
        JobUtils jobUtils = new JobUtils(dataSource);
        return jobUtils.getJobDeletionStatus(jobName, jobGroup);
    }
}
