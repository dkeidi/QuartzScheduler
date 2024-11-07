package com.quartz.util;

import com.quartz.info.TriggerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public final class SchedulerBuilder {
    private static final Logger LOG = LogManager.getLogger(SchedulerBuilder.class);

    private SchedulerBuilder() {}

    public static JobDetail buildJobDetail(final Class jobClass, final TriggerInfo info) {
        final JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(jobClass.getSimpleName(), info);

        return JobBuilder
                .newJob(jobClass)
                .withIdentity(jobClass.getSimpleName())
                .setJobData(jobDataMap)
                .build();
    }

    public static CronTrigger buildTrigger(final Class jobClass, final TriggerInfo info) {
        CronScheduleBuilder cron = cronSchedule(info.getCronExp()).withMisfireHandlingInstructionDoNothing();

        return newTrigger()
                .withIdentity(jobClass.getSimpleName())
                .withSchedule(cron)
                .build();
    }
}