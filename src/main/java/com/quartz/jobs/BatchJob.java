package com.quartz.jobs;

import com.quartz.util.CustomLogger;
import com.quartz.util.JobExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.io.IOException;
import java.util.UUID;

public class BatchJob extends QuartzJobBean {
    public String JOB_NAME = "";
    String LOG_FILENAME = JOB_NAME + "/" + CustomLogger.getCurrentDate() + ".log";

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Scheduler scheduler = context.getScheduler();
        String jobId = "";
        String instanceId = "";
        JOB_NAME = context.getJobDetail().getKey().getName();

        String scriptLocation = context.getJobDetail().getJobDataMap().getString("command");
        String masterScriptLocation = context.getJobDetail().getJobDataMap().getString("master_command");
        String jobKey = context.getJobDetail().getKey().getName();
        Logger LOG = LogManager.getLogger("com.quartz.jobs." + jobKey);

        LOG.info("jobKey: ", jobKey);
        try {
            jobId = UUID.randomUUID().toString();  // Generate a UUID for job
            instanceId = scheduler.getSchedulerInstanceId();
            CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executing", LOG_FILENAME, null);

            JobExecutor.executeJob(scriptLocation, masterScriptLocation, LOG, jobId, context.getJobDetail().getKey().getName(), instanceId, LOG_FILENAME);
        } catch (IOException | InterruptedException | SchedulerException e) {
            CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Error", LOG_FILENAME, e);
            LOG.error(e);
            throw new JobExecutionException(e);
        }
    }
}
