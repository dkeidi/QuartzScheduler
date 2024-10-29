package com.quartz.jobs;

import com.quartz.info.TriggerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.quartz.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class HelloWorldJob implements Job {
    private static final Logger LOG = LogManager.getLogger(HelloWorldJob.class);

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String JOB_NAME = "HelloWorldJob";

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

            _initializeThreadContext(jobId, instanceId,"Executing", null);

            _executeJob(jobId, context, info.getCronExp(), instanceId);
        } catch (Exception e) {
            _initializeThreadContext(jobId, instanceId,"Error", null);
            LOG.error(e);
            throw new RuntimeException(e);
        } finally {
            ThreadContext.clearAll();
        }
    }

    private void _initializeThreadContext(String jobId, String instanceId, String jobStatus, Exception e) {
        ThreadContext.put("jobId", jobId);
        ThreadContext.put("jobName", JOB_NAME);
        ThreadContext.put("endTime", _getCurrentTime());
        ThreadContext.put("instanceId", instanceId);
        ThreadContext.put("status", jobStatus);
        ThreadContext.put("msg", String.valueOf(e));
    }

    // Method to execute the job
    private void _executeJob(String jobId, JobExecutionContext context, String cronExp, String instanceId) {
        _initializeThreadContext(jobId, instanceId,"Executing", null);
//        LOG.info("Starting job: copy_file script, frequency: {}", cronExp);

        // Execute the job
        LOG.info("Every min, at the 5th second, I will say Hello World!");
        // Update context and log success
        _initializeThreadContext(jobId, instanceId,"Executed", null);
        LOG.info("Job completed successfully");
    }

    private String _getCurrentTime() {
        return LocalDateTime.now().format(FORMATTER);
    }
}