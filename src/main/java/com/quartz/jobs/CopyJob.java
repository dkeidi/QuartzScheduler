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

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
//        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
//        TriggerInfo info = (TriggerInfo) jobDataMap.get(CopyJob.class.getSimpleName());
        Scheduler scheduler = context.getScheduler();
        String jobId = "";
        String instanceId = "";

        try {
            jobId = UUID.randomUUID().toString();  // Generate a UUID for job
            instanceId = scheduler.getSchedulerInstanceId();

            CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executing", LOG_FILENAME, null);

            _executeJob(jobId, context, instanceId);

        } catch (Exception e) {
            CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Error", LOG_FILENAME, e);
            LOG.error(e);

            throw new JobExecutionException(e);
        } finally {
            ThreadContext.clearAll();
        }
    }

    private void _executeJob(String jobId, JobExecutionContext context, String instanceId) throws IOException, InterruptedException {
        CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executing", LOG_FILENAME, null);

        JobExecutor.executeJob(COMMAND, MASTER_COMMAND, isNetworkLocation, LOG, jobId, context.getJobDetail().getKey().getName(), instanceId, LOG_FILENAME);

        CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executed", LOG_FILENAME, null);
        LOG.info("Job completed successfully");
    }
}
