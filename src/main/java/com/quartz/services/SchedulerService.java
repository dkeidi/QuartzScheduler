package com.quartz.services;

import com.quartz.info.TriggerInfo;
import com.quartz.jobs.BatchJob;
import com.quartz.jobs.CopyJob;
import com.quartz.util.SchedulerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchedulerService {
    private static final Logger LOG = LogManager.getLogger(SchedulerService.class);
    private final Scheduler scheduler;

    @Autowired
    public SchedulerService(final Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public <T extends Job> void schedule(final JobDetail jobDetail, final TriggerInfo info, final boolean isCron) {
        CronTrigger cronTrigger = null;
        Trigger simpleTrigger = null;

        if (isCron) {
            cronTrigger = SchedulerBuilder.buildCronTrigger(jobDetail.getJobClass(), info);
        } else {
            simpleTrigger = SchedulerBuilder.buildSimpleTrigger(jobDetail.getJobClass(), info);
        }

        try {
            LOG.info("{} job scheduled.", info.getCallbackData());
            LOG.info("Job key is {}.", jobDetail.getKey());

            if (scheduler.checkExists(jobDetail.getKey())){
                scheduler.deleteJob(jobDetail.getKey());
            }

            if (isCron) {
                scheduler.scheduleJob(jobDetail, cronTrigger);
            } else {
                scheduler.scheduleJob(jobDetail, simpleTrigger);
            }

        } catch (SchedulerException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public <T extends Job> void scheduleManual(final Class<T> jobClass, final TriggerInfo info) {
        final JobDetail jobDetail = SchedulerBuilder.buildJobDetail(jobClass, info);
        final CronTrigger trigger = SchedulerBuilder.buildCronTrigger(jobClass, info);

        try {
            LOG.info("{} job scheduled.", info.getCallbackData());
            LOG.info("Job key is {}.", jobDetail.getKey());

            if (scheduler.checkExists(jobDetail.getKey())){
                scheduler.deleteJob(jobDetail.getKey());
            }

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public List<TriggerInfo> getScheduledJobs() throws SchedulerException {

        LOG.info("getScheduledJobs");
        List<TriggerInfo> triggerInfoList = new ArrayList<>();

        for (String groupName : scheduler.getJobGroupNames()) {

            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                String jobName = jobKey.getName();
                String jobGroup = jobKey.getGroup();

                //get job's trigger
                List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);

                for (Trigger trigger : triggers) {
                    Date nextFireTime = trigger.getNextFireTime();
                    String cronExpression = null;

                    // Check if the trigger is a CronTrigger
                    if (trigger instanceof CronTrigger) {
                        CronTrigger cronTrigger = (CronTrigger) trigger;
                        cronExpression = cronTrigger.getCronExpression(); // Retrieve the cron expression
                    }

                    // Log the details
                    LOG.info("[jobName] : " + jobName + " [groupName] : " + jobGroup + " [cron] : " + cronExpression + " - " + nextFireTime);

                    // Create a TriggerInfo object and add it to the list
                    TriggerInfo triggerInfo = new TriggerInfo();
                    triggerInfo.setJobGroup(jobGroup);
                    triggerInfo.setJobName(jobName);
                    triggerInfoList.add(triggerInfo);
                }
            }
        }
        return triggerInfoList;
    }

    public List<TriggerInfo> getAllRunningJobs() {
        try {
            // jobs belong to a group
            return scheduler.getJobKeys(GroupMatcher.anyGroup())
                    .stream()
                    .map(jobKey -> {
                        try {
                            final JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                            return (TriggerInfo) jobDetail.getJobDataMap().get(jobKey.getName());
                        } catch (final SchedulerException e) {
                            LOG.error(e.getMessage(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (final SchedulerException e) {
            LOG.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public TriggerInfo getRunningJob(String jobId) {
        try {
            final JobDetail jobDetail = scheduler.getJobDetail(new JobKey(jobId));
            if (jobDetail == null) {
                LOG.error("Failed to find job with ID '{}'", jobId);
                return null;
            }

            return (TriggerInfo) jobDetail.getJobDataMap().get(jobId);
        } catch (final SchedulerException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public void updateJob(final String jobId, final TriggerInfo info) {
        try {
            final JobDetail jobDetail = scheduler.getJobDetail(new JobKey(jobId));
            if (jobDetail == null) {
                LOG.error("Failed to find job with ID '{}'", jobId);
                return;
            }

            jobDetail.getJobDataMap().put(jobId, info);

            scheduler.addJob(jobDetail, true, true);
        } catch (final SchedulerException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public TriggerInfo createAdhocCopyJob() {
        JobDetail jobDetail = JobBuilder.newJob(CopyJob.class)
                .withIdentity("CopyJob")
                .build();

        TriggerInfo info2 = new TriggerInfo();
        info2.setCronExp("0 51 16 * * ?");
        info2.setCallbackData("CopyJob");
        info2.setJobName("CopyJob");

        schedule(jobDetail, info2, true);
        return info2;
    }

    public TriggerInfo createAdhocJob(String jobKey, Date jobDate, String jobCronExp, String commandValue, Boolean isNetworkLocation) {
        JobDetail jobDetail = JobBuilder.newJob(BatchJob.class)
                .withIdentity(jobKey)
                .usingJobData("command", commandValue)
                .usingJobData("folder", jobKey)
                .build();

        TriggerInfo info = new TriggerInfo();
        info.setCronExp(jobCronExp);
        info.setCallbackData(jobKey);
        info.setJobName(jobKey);

        schedule(jobDetail, info, true);

        String logFolderName = "quartz_logs";
//        addRollingFileLogger(logFolderName, jobKey, jobKey + "/%d{dd-MM-yyyy}.log");

        return info;
    }

//    private static void addRollingFileLogger(String logFolderName, String loggerName, String filePattern) {
//        LoggerContext context = (LoggerContext) LogManager.getContext(false);
//        Configuration config = context.getConfiguration();
//
//        // Define the layout for the RollingFile appender
//        PatternLayout layout = PatternLayout.newBuilder()
//                .withPattern("[%-5level] %d{dd-MM-yyyy HH:mm:ss.SSS} [%t] %c{1} - %msg%n")
//                .build();
//
//        // Create the RollingFile appender
//        RollingFileAppender rollingFileAppender = RollingFileAppender.newBuilder()
//                .setName(loggerName)
//                .setLayout(layout)
//                .withFileName(logFolderName + "/" + filePattern.replace("%d", ""))
//                .withFilePattern(filePattern + ".gz")
//                .withPolicy(SizeBasedTriggeringPolicy.createPolicy("10MB"))
//                .withPolicy(TimeBasedTriggeringPolicy.newBuilder().withInterval(1).withModulate(true).build())
//                .setConfiguration(config)
//                .build();
//
//        rollingFileAppender.start();
//
//        // Add the appender to the configuration
//        config.addAppender(rollingFileAppender);
//
//        // Define a logger reference
//        AppenderRef ref = AppenderRef.createAppenderRef(rollingFileAppender.getName(), null, null);
//        AppenderRef consoleRef = AppenderRef.createAppenderRef("LogToConsole", null, null);
//        AppenderRef jobToDbRef = AppenderRef.createAppenderRef("LogJobToDB", null, null);
//        AppenderRef detailToDbRef = AppenderRef.createAppenderRef("LogDetailToDB", null, null);
//
//        AppenderRef[] refs = new AppenderRef[]{ref, consoleRef, jobToDbRef, detailToDbRef};
//
//        // Create a new logger configuration
//        LoggerConfig loggerConfig = LoggerConfig.createLogger(false, null, loggerName, "true", refs, null, config, null);
//        loggerConfig.addAppender(rollingFileAppender, null, null);
//
//        // Add the logger configuration to the context
//        config.addLogger(loggerName, loggerConfig);
//
//        // Update the logger context
//        context.updateLoggers();
//    }
}