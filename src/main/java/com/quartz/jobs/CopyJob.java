package com.quartz.jobs;

import com.quartz.info.TriggerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CopyJob implements Job {
    private static final Logger LOG = LogManager.getLogger(CopyJob.class);

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        TriggerInfo info = (TriggerInfo) jobDataMap.get(CopyJob.class.getSimpleName());

        try {
            // directory part needs to be modified
            LOG.info("Starting job: copy_file.bat, frequency: " +  info.getCronExp());
            Runtime.getRuntime().exec("cmd /c start \"\" C:\\Users\\keidi.tay.chuan\\Documents\\batch_files\\copy_file.bat");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("Copied file from source_folder to dest_folder complete");
    }
}