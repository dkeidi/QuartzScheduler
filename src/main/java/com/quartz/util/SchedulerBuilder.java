package com.quartz.util;

import com.quartz.info.TriggerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;

import java.util.Date;

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
                .withIdentity(info.getJobName())
                .setJobData(jobDataMap)
                .build();
    }

    public static CronTrigger buildCronTrigger(final Class jobClass, final TriggerInfo info) {
        CronScheduleBuilder cron = cronSchedule(info.getCronExp()).withMisfireHandlingInstructionIgnoreMisfires();

        return newTrigger()
                .withIdentity(info.getJobName())
                .withSchedule(cron)
                .build();
    }

    public static Trigger buildSimpleTrigger(final Class jobClass, final TriggerInfo info) {
        SimpleScheduleBuilder builder = SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(info.getRepeatIntervalMs());

        return TriggerBuilder
                .newTrigger()
                .withIdentity(jobClass.getSimpleName())
                .withSchedule(builder)
                .startAt(new Date(System.currentTimeMillis() + info.getInitialOffsetMs()))
                .build();
    }
}