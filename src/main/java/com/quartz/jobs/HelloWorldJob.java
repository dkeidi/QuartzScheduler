package com.quartz.jobs;

import com.quartz.info.TriggerInfo;
import com.quartz.util.CustomLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class HelloWorldJob implements Job {
    private static final Logger LOG = LogManager.getLogger(HelloWorldJob.class);
    private static final String JOB_NAME = "HelloWorldJob";
    String LOG_FILENAME = JOB_NAME + "/" + CustomLogger.getCurrentDate() + ".log";

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        TriggerInfo info = (TriggerInfo) jobDataMap.get(HelloWorldJob.class.getSimpleName());
        Scheduler scheduler = context.getScheduler();
        String jobId = "";
        String instanceId = "";

        try {
            jobId = UUID.randomUUID().toString();  // Generate a UUID for job
            instanceId = scheduler.getSchedulerInstanceId();
            CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executing", LOG_FILENAME, null);

            _executeJob(jobId, context, info.getCronExp(), instanceId);
        } catch (Exception e) {
            CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Error", LOG_FILENAME, e);
            LOG.error(e);
            throw new RuntimeException(e);
        } finally {
            ThreadContext.clearAll();
        }
    }

    // Method to execute the job
    private void _executeJob(String jobId, JobExecutionContext context, String cronExp, String instanceId) {
        CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executing", LOG_FILENAME, null);

//        LOG.info("Starting job: copy_file script, frequency: {}", cronExp);

        // Execute the job
        LOG.info("Every min, at the 5th second, I will say Hello World!");
        // Update context and log success
        CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executed", LOG_FILENAME, null);
        LOG.info("Job completed successfully");
    }
}