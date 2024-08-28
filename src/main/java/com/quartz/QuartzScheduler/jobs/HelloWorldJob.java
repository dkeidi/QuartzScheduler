package com.quartz.QuartzScheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

@Component
public class HelloWorldJob implements Job {
    private static final Logger LOG = LogManager.getLogger(HelloWorldJob.class);

    @Override
    public void execute(JobExecutionContext context) {
       LOG.info("Every min, at the 5th second, I will say Hello World!");
    }
}