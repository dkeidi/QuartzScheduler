package com.quartz.jobs;

import com.quartz.info.TriggerInfo;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TransferJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(TransferJob.class);

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        TriggerInfo info = (TriggerInfo) jobDataMap.get(TransferJob.class.getSimpleName());

        try {
            // directory part needs to be modified
            Runtime.getRuntime().exec("cmd /c start \"\" C:\\Users\\keidi.tay.chuan\\Documents\\timer\\scripts\\transfer.bat");
            // include exit in batch file to close cmd
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("Done");
    }
}