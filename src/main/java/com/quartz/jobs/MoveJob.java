package com.quartz.jobs;

import com.quartz.info.TriggerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class MoveJob implements Job {
    private static final Logger LOG = LogManager.getLogger(MoveJob.class);

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        TriggerInfo info = (TriggerInfo) jobDataMap.get(MoveJob.class.getSimpleName());

//        try {
//            LOG.info("Starting job: {}, frequency: {}", this.getClass().getName(), info.getCronExp());
//            JobExecutor.executeJob("job." + this.getClass().getName(), info.getScriptLocation(), info.getCronExp(), LOG);
//        } catch (IOException | InterruptedException e) {
//            LOG.error("Exception occurred while executing the job", e);
//        }
    }
}