package com.quartz.services;

import com.quartz.info.TriggerInfo;
import com.quartz.jobs.BatchJob;
import com.quartz.jobs.CopyJob;
import com.quartz.util.SchedulerBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchedulerService {
    private static final Logger LOG = LogManager.getLogger(SchedulerService.class);
    private final Scheduler scheduler;

    public static Properties appProperties;
    public static Properties jobProperties;
    public static String generatedLogPath;

    public static void setAppProperties(Properties appProperties) {
        SchedulerService.appProperties = appProperties;
    }

    public static void setJobProperties(Properties jobProperties) {
        SchedulerService.jobProperties = jobProperties;
    }

    public static void setGeneratedLogPath(String generatedLogPath) {
        SchedulerService.generatedLogPath = generatedLogPath;
    }

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

            if (scheduler.checkExists(jobDetail.getKey())) {
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

            if (scheduler.checkExists(jobDetail.getKey())) {
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
                    if (trigger instanceof CronTrigger cronTrigger) {
                        cronExpression = cronTrigger.getCronExpression(); // Retrieve the cron expression
                    }

                    // Log the details
                    LOG.info("[jobName] : {} [groupName] : {} [cron] : {} - {}", jobName, jobGroup, cronExpression, nextFireTime);

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

    public TriggerInfo createRecurringAdhocCopyJob() {
        JobDetail jobDetail = JobBuilder.newJob(CopyJob.class)
                .withIdentity("CopyJob")
                .build();

        TriggerInfo info = new TriggerInfo();
        info.setCronExp("0 51 16 * * ?");
        info.setCallbackData("CopyJob");
        info.setJobName("CopyJob");

        schedule(jobDetail, info, true);
        return info;
    }

    public TriggerInfo createOneTimeAdhocCopyJob(String jobDatetime) {
        JobDetail jobDetail = JobBuilder.newJob(CopyJob.class)
                .withIdentity("CopyJob")
                .build();

        TriggerInfo info = new TriggerInfo();
        info.setJobDatetime(jobDatetime);
        info.setCallbackData("CopyJob");
        info.setJobName("CopyJob");

        schedule(jobDetail, info, false);
        addAdHocJobLogger("CopyJob");

        return info;

    }

    public TriggerInfo createOnetimeJob(String jobKey, String jobDatetime, String commandValue, Boolean isServerScript, String groupName) {
        String masterCommandValue = jobProperties.getProperty("master.map_drive.command");

        JobDetail jobDetail = JobBuilder.newJob(BatchJob.class)
                .withIdentity(jobKey, groupName)
                .usingJobData("command", commandValue)
                .usingJobData("master_command", masterCommandValue)
                .usingJobData("is_server_script", isServerScript)
                .usingJobData("folder", jobKey)
                .build();

        TriggerInfo info = new TriggerInfo();
        info.setJobDatetime(jobDatetime);
        info.setCallbackData(jobKey);
        info.setJobName(jobKey);

        schedule(jobDetail, info, true);

        addAdHocJobLogger(jobKey);

        return info;
    }

    public TriggerInfo createRecurringJob(String jobKey, String jobCronExp, String commandValue, Boolean isServerScript, String groupName) {
        String masterCommandValue = jobProperties.getProperty("master.map_drive.command");

        JobDetail jobDetail = JobBuilder.newJob(BatchJob.class)
                .withIdentity(jobKey)
                .usingJobData("command", commandValue)
                .usingJobData("master_command", masterCommandValue)
                .usingJobData("is_server_script", isServerScript)
                .usingJobData("folder", jobKey)
                .build();

        TriggerInfo info = new TriggerInfo();
        info.setCronExp(jobCronExp);
        info.setCallbackData(jobKey);
        info.setJobName(jobKey);

        schedule(jobDetail, info, true);

        addAdHocJobLogger(jobKey);

        return info;
    }

    public TriggerInfo createRecurringAdhocJob(String jobKey, String jobCronExp, String commandValue, Boolean isServerScript) {
        String masterCommandValue = jobProperties.getProperty("master.map_drive.command");

        JobDetail jobDetail = JobBuilder.newJob(BatchJob.class)
                .withIdentity(jobKey)
                .usingJobData("command", commandValue)
                .usingJobData("master_command", masterCommandValue)
                .usingJobData("is_server_script", isServerScript)
                .usingJobData("folder", jobKey)
                .build();

        TriggerInfo info = new TriggerInfo();
        info.setCronExp(jobCronExp);
        info.setCallbackData(jobKey);
        info.setJobName(jobKey);

        schedule(jobDetail, info, true);

        addAdHocJobLogger(jobKey);

        return info;
    }

    public static void addAdHocJobLogger(String jobKey) {
        String logFolderName = appProperties.getProperty("app.logFolder");
        String jobLogLevel = appProperties.getProperty("app.job.logLevel");
        String jobPrefix = appProperties.getProperty("app.jobname.prefix");
        String logDir = logFolderName + "/" + jobKey + "/";

        String dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy").format(LocalDateTime.now()) + ".log";
        String archiveDateFormat = "%d{dd-MM-yyyy}.log.gz";
        String rollingFilePattern = "[%-5level] %d{dd-MM-yyyy HH:mm:ss.SSS} [%t] %c{1} - %msg%n";

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        // Define the layout for the RollingFile appender
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern(rollingFilePattern)
                .build();

        // Create the RollingFile appender for the ad-hoc job
        RollingFileAppender rollingFileAppender = RollingFileAppender.newBuilder()
                .setName("Log" + jobKey + "ToFile")
                .setLayout(layout)
                .withFileName(logDir + dateFormat)
                .withFilePattern(logDir + archiveDateFormat)
                .withPolicy(SizeBasedTriggeringPolicy.createPolicy("10MB"))
                .withPolicy(TimeBasedTriggeringPolicy.newBuilder().withInterval(1).withModulate(true).build())
                .setConfiguration(config)
                .build();

        rollingFileAppender.start();

        config.addAppender(rollingFileAppender);

        // Define a logger reference for the new job
        AppenderRef rollingRef = AppenderRef.createAppenderRef(rollingFileAppender.getName(), null, null);
        AppenderRef consoleRef = AppenderRef.createAppenderRef("LogToConsole", null, null);
        AppenderRef jobToDbRef = AppenderRef.createAppenderRef("LogJobToDB", null, null);
        AppenderRef detailToDbRef = AppenderRef.createAppenderRef("LogDetailToDB", null, null);
        AppenderRef fileToDbRef = AppenderRef.createAppenderRef("LogFileToDB", null, null);

        AppenderRef[] refs = new AppenderRef[]{rollingRef, consoleRef, jobToDbRef, detailToDbRef, fileToDbRef};

        // Create or update the logger configuration for the ad-hoc job
        LoggerConfig loggerConfig = config.getLoggerConfig(jobPrefix + "." + jobKey);

        if (loggerConfig == null || loggerConfig.getName().isEmpty()) {
            // Logger doesn't exist, create a new one
            loggerConfig = LoggerConfig.createLogger(false, Level.valueOf(jobLogLevel), jobKey, "true", refs, null, config, null);
            loggerConfig.addAppender(rollingFileAppender, Level.valueOf(jobLogLevel), null);
            loggerConfig.addAppender(config.getAppender("LogToConsole"), Level.valueOf(jobLogLevel), null); // Add existing appenders
            loggerConfig.addAppender(config.getAppender("LogJobToDB"), Level.valueOf(jobLogLevel), null);
            loggerConfig.addAppender(config.getAppender("LogDetailToDB"), Level.valueOf(jobLogLevel), null);
            loggerConfig.addAppender(config.getAppender("LogFileToDB"), Level.valueOf(jobLogLevel), null);

            // Add the logger configuration to the context
            config.addLogger(jobPrefix + "." + jobKey, loggerConfig);
        } else {
            // Logger exists, update its appenders
            loggerConfig.addAppender(rollingFileAppender, Level.valueOf(jobLogLevel), null);
            if (!loggerConfig.getAppenders().containsKey("LogToConsole")) {
                loggerConfig.addAppender(config.getAppender("LogToConsole"), Level.valueOf(jobLogLevel), null);
            }
            if (!loggerConfig.getAppenders().containsKey("LogJobToDB")) {
                loggerConfig.addAppender(config.getAppender("LogJobToDB"), Level.valueOf(jobLogLevel), null);
            }
            if (!loggerConfig.getAppenders().containsKey("LogDetailToDB")) {
                loggerConfig.addAppender(config.getAppender("LogDetailToDB"), Level.valueOf(jobLogLevel), null);
            }
            if (!loggerConfig.getAppenders().containsKey("LogFileToDB")) {
                loggerConfig.addAppender(config.getAppender("LogFileToDB"), Level.valueOf(jobLogLevel), null);
            }
        }

        context.updateLoggers();

        LOG.info("RAMLogger updated");
        // Reload the Log4j2 configuration
        reloadLog4jConfiguration(generatedLogPath);

//        _xmlDebug(config);
    }

    public static void reloadLog4jConfiguration(String configFilePath) {
        try {
            ConfigurationSource source = new ConfigurationSource(new FileInputStream(configFilePath));
            Configurator.initialize(null, source);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private static void _xmlDebug(Configuration config) {
        System.out.println("##########APPENDERS############");

        // Inspect appenders
        config.getAppenders().forEach((name, appender) -> {
            System.out.println("Appender Name: " + name);
            System.out.println("Appender Type: " + appender.getClass().getName());
            if (appender.getLayout() != null) {
                System.out.println("Appender Layout: " + appender.getLayout().getClass().getName());
            }
        });


        System.out.println("##########DYNAMIC LOGGERS (WORK IN PROGRESS) ############");

        // Inspect loggers
        config.getLoggers().forEach((name, logger) -> {
            System.out.println("Logger Name: " + name);
            System.out.println("Logger Level: " + logger.getLevel());
            System.out.println("Appender References: ");
            logger.getAppenderRefs().forEach(ref -> System.out.println("    - " + ref.getRef()));
        });
    }

}