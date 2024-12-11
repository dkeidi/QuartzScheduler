package com.quartz.tests;

import com.quartz.jobs.BadJob1;
import com.quartz.jobs.BadJob2;
import com.quartz.util.CustomLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;
import java.util.UUID;

import static org.quartz.DateBuilder.nextGivenSecondDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * This job demonstrates how Quartz can handle JobExecutionExceptions that are thrown by jobs.
 *
 * @author Bill Kratzer
 */
public class JobExceptionExample {
    private static final String JOB_NAME = "JobExceptionExample";
    String LOG_FILENAME = JOB_NAME + "/" + CustomLogger.getCurrentDate() + ".log";

    public void run() throws Exception {
        Logger LOG = LogManager.getLogger(JobExceptionExample.class);
        String jobId = "";
        String instanceId = "";


        LOG.info("------- Initializing ----------------------");

        // First we must get a reference to a scheduler
        SchedulerFactory sf = new StdSchedulerFactory();
        Scheduler scheduler = sf.getScheduler();

        jobId = UUID.randomUUID().toString();  // Generate a UUID for job
        instanceId = scheduler.getSchedulerInstanceId();

        CustomLogger.initializeThreadContext(jobId, JOB_NAME, instanceId, "Executing", LOG_FILENAME, null);


        LOG.info("------- Initialization Complete ------------");

        LOG.info("------- Scheduling Jobs -------------------");

        // jobs can be scheduled before start() has been called

        // get a "nice round" time a few seconds in the future...
        Date startTime = nextGivenSecondDate(null, 15);

        // badJob1 will run every 10 seconds
        // this job will throw an exception and refire
        // immediately
        JobDetail job = newJob(BadJob1.class).withIdentity("badJob1", "group1").usingJobData("denominator", "0").build();

        SimpleTrigger trigger = newTrigger().withIdentity("trigger1", "group1").startAt(startTime)
                .withSchedule(simpleSchedule().withIntervalInSeconds(10).repeatForever()).build();

        Date ft = scheduler.scheduleJob(job, trigger);
        LOG.info(job.getKey() + " will run at: " + ft + " and repeat: " + trigger.getRepeatCount() + " times, every "
                + trigger.getRepeatInterval() / 1000 + " seconds");

        // badJob2 will run every five seconds
        // this job will throw an exception and never
        // refire
        job = newJob(BadJob2.class).withIdentity("badJob2", "group1").build();

        trigger = newTrigger().withIdentity("trigger2", "group1").startAt(startTime)
                .withSchedule(simpleSchedule().withIntervalInSeconds(5).repeatForever()).build();

        ft = scheduler.scheduleJob(job, trigger);
        LOG.info(job.getKey() + " will run at: " + ft + " and repeat: " + trigger.getRepeatCount() + " times, every "
                + trigger.getRepeatInterval() / 1000 + " seconds");

        LOG.info("------- Starting Scheduler ----------------");

        // jobs don't start firing until start() has been called...
        scheduler.start();

        LOG.info("------- Started Scheduler -----------------");

        try {
            // sleep for 30 seconds
            Thread.sleep(30L * 1000L);
        } catch (Exception e) {
            //
        }

        LOG.info("------- Shutting Down ---------------------");

//    sched.shutdown(false);

        LOG.info("------- Shutdown Complete -----------------");

        SchedulerMetaData metaData = scheduler.getMetaData();
        LOG.info("Executed " + metaData.getNumberOfJobsExecuted() + " jobs.");
    }
}
