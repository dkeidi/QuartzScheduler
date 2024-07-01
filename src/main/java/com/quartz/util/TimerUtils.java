package com.quartz.util;

import com.quartz.info.TriggerInfo;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public final class TimerUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TimerUtils.class);

    private TimerUtils() {}

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
        CronScheduleBuilder cron = cronSchedule(info.getCronExp());

        return newTrigger()
                .withIdentity(jobClass.getSimpleName())
                .withSchedule(cron)
                .build();
    }
}