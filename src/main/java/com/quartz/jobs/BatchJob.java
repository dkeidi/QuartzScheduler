package com.quartz.jobs;

import com.quartz.util.JobExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class BatchJob extends QuartzJobBean {
    private static final String JOB_NAME = "CopyJob";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    String LOG_FILENAME = JOB_NAME + "/" + _getCurrentDate() + ".log";

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Scheduler scheduler = context.getScheduler();
        String jobId = "";
        String instanceId = "";

        String command = context.getJobDetail().getJobDataMap().getString("command");
        String jobKey = context.getJobDetail().getKey().getName();
        Logger LOG = LogManager.getLogger("com.quartz.jobs." + jobKey);

        try {
            jobId = UUID.randomUUID().toString();  // Generate a UUID for job
            instanceId = scheduler.getSchedulerInstanceId();
//            LOG.info("Executing command: {}", command);
            JobExecutor.executeJob(command, LOG, jobId, context.getJobDetail().getKey().getName(), instanceId, LOG_FILENAME);
        } catch (IOException | InterruptedException | SchedulerException e) {
            LOG.error("Exception occurred while executing the job", e);
            throw new JobExecutionException(e);
        }
    }

    private String _getCurrentDate() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }
}
