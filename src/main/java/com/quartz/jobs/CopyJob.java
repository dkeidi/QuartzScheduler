package com.quartz.jobs;

import com.quartz.util.CustomLogger;
import com.quartz.util.JobExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;


@Component
public class CopyJob implements Job {

    private static final Logger LOG = LogManager.getLogger(CopyJob.class);

    // Constants
    private static final String COMMAND = "C:\\Users\\keidi.tay.chuan\\Documents\\MyQuartzTest\\batch_files\\copy_file.bat";
     private static final String JOB_NAME = "CopyJob";
    String LOG_FILENAME = JOB_NAME + "/" + CustomLogger.getCurrentDate() + ".log";

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
//        TriggerInfo info = (TriggerInfo) jobDataMap.get(CopyJob.class.getSimpleName());
        Scheduler scheduler = context.getScheduler();
        String jobId = "";
        String instanceId = "";

        try {
            jobId = UUID.randomUUID().toString();  // Generate a UUID for job
            instanceId = scheduler.getSchedulerInstanceId();

            CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId,"Executing", LOG_FILENAME,null);

            _executeJob(jobId, context, instanceId);

        } catch (Exception e) {
            CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId,"Error", LOG_FILENAME, e);
            LOG.error(e);
            throw new RuntimeException(e);
        } finally {
            ThreadContext.clearAll();
        }
    }

//    private void _initializeThreadContext(String jobId, String instanceId, String jobStatus, Exception e) {
//        ThreadContext.put("jobId", jobId);
//        ThreadContext.put("jobName", JOB_NAME);
//        ThreadContext.put("startTime", CustomLogger.getCurrentTime());
//        ThreadContext.put("endTime", CustomLogger.getCurrentTime());
//        ThreadContext.put("instanceId", instanceId);
//        ThreadContext.put("status", jobStatus);
//        ThreadContext.put("msg", String.valueOf(e));
//        ThreadContext.put("logFileName", LOG_FILENAME);
//    }

    private void _executeJob(String jobId, JobExecutionContext context, String instanceId) throws IOException, InterruptedException {
        CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId,"Executing", LOG_FILENAME, null);

        JobExecutor.executeJob(context.getJobDetail().getKey().getName(), COMMAND, LOG, jobId, LOG_FILENAME);

        CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId,"Executed", LOG_FILENAME, null);
        LOG.info("Job completed successfully");
    }
}
