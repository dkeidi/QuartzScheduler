package com.quartz.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HelloWorldJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorldJob.class);

    @Override
    public void execute(JobExecutionContext context) {
//        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
//        TriggerInfo info = (TriggerInfo) jobDataMap.get(HelloWorldJob.class.getSimpleName());
        LOG.info("Hello World");
    }
}