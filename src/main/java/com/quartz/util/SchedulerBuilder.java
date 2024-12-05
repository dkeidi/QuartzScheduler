package com.quartz.util;

import com.quartz.info.TriggerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy HH:mm:ss");
        LocalDateTime dateTime;

        // Check if jobDatetime is empty
        if (info.getJobDatetime() == null || info.getJobDatetime().isEmpty()) {
            dateTime = LocalDateTime.now(); // Use current time
            System.out.println("Job datetime is empty. Using current datetime: " + dateTime);
        } else {
            dateTime = LocalDateTime.parse(info.getJobDatetime(), formatter);
            System.out.println("Parsed DateTime: " + dateTime);
        }

        // Convert LocalDateTime to Date
        Date startDate = Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());

        // Build SimpleScheduleBuilder
        SimpleScheduleBuilder builder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMilliseconds(info.getRepeatIntervalMs())
                .withRepeatCount(info.getRepeatCount()); // You can configure repeat count here

        // Create and return Trigger
        return newTrigger()
                .withIdentity(info.getJobName())
                .withSchedule(builder)
                .startAt(new Date(startDate.getTime() + info.getInitialOffsetMs())) // Apply offset to start time
                .build();
    }

}