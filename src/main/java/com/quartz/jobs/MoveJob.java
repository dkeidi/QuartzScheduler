package com.quartz.jobs;

import com.quartz.info.TriggerInfo;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MoveJob implements Job {
    private static final Logger LOG = LogManager.getLogger(MoveJob.class);

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        TriggerInfo info = (TriggerInfo) jobDataMap.get(MoveJob.class.getSimpleName());

        try {
            // directory part needs to be modified
            LOG.info("Starting job: move_file.bat, frequency: " +  info.getCronExp());
            Runtime.getRuntime().exec("cmd /c start \"\" C:\\Users\\keidi.tay.chuan\\Documents\\batch_files\\move_file.bat");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("Moved file from folder B to folder A complete.");
    }
}