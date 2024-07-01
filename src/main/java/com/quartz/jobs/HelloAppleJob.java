package com.quartz.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HelloAppleJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(HelloAppleJob.class);

    @Override
    public void execute(JobExecutionContext context) {
//        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
//        TriggerInfo info = (TriggerInfo) jobDataMap.get(HelloAppleJob.class.getSimpleName());
        LOG.info("Hello Apple");
    }
}