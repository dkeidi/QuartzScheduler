package com.quartz.jobs;

import com.quartz.util.JobExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.io.IOException;

public class BatchJob extends QuartzJobBean {
    private static final Logger LOG = LogManager.getLogger(BatchJob.class);

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String command = context.getJobDetail().getJobDataMap().getString("command");
        String jobKey = context.getJobDetail().getKey().getName();
        Logger LOG = LogManager.getLogger("com.quartz.jobs." + jobKey);

        try {
            LOG.info("Executing command: {}", command);
            JobExecutor.executeJob(context.getJobDetail().getKey().getName(), command, LOG);
        } catch (IOException | InterruptedException e) {
            LOG.error("Exception occurred while executing the job", e);
            throw new JobExecutionException(e);
        }
    }
}
