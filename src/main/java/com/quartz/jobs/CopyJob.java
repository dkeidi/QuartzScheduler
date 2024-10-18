package com.quartz.jobs;

import com.quartz.info.TriggerInfo;
import com.quartz.util.JobExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class CopyJob implements Job {
    private static final Logger LOG = LogManager.getLogger(CopyJob.class);

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        TriggerInfo info = (TriggerInfo) jobDataMap.get(CopyJob.class.getSimpleName());

        String jobId = UUID.randomUUID().toString();  // Generate a UUID
        ThreadContext.put("jobId", jobId);  // Put the actual UUID into the ThreadContext

        ThreadContext.put("jobName", "CopyJob");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String now = LocalDateTime.now().format(formatter);
        ThreadContext.put("startTime", now);
        ThreadContext.put("endTime", now);

        try {
            // directory part needs to be modified
            LOG.info("Starting job: copy_file.bat, frequency: " +  info.getCronExp());
            String command = "C:\\Users\\keidi.tay.chuan\\Documents\\MyQuartzTest\\batch_files\\copy_file.bat";

            JobExecutor.executeJob(context.getJobDetail().getKey().getName(), command, LOG);

            now = LocalDateTime.now().format(formatter);
            ThreadContext.put("startTime", now);
            ThreadContext.put("endTime", now);
            ThreadContext.put("status", "Executed");
            LOG.info("Job completed");

        } catch (IOException e) {
            ThreadContext.put("status", "Error");
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            ThreadContext.clearAll();
        }

//        try {
//            LOG.info("Starting job: {}, frequency: {}", this.getClass().getName(), info.getCronExp());
//            JobExecutor.executeJob("job." + this.getClass().getName(), info.getScriptLocation(), info.getCronExp(), LOG);
//        } catch (IOException | InterruptedException e) {
//            LOG.error("Exception occurred while executing the job", e);
//        }
    }
}
