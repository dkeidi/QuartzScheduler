package com.quartz.services;

import com.quartz.info.TriggerInfo;
import com.quartz.jobs.BatchJob;
import com.quartz.model.JobResult;
import com.quartz.util.JobUtils;
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
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
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
    private final DataSource dataSource;

    @Autowired
    public SchedulerService(final Scheduler scheduler, @QuartzDataSource DataSource dataSource) {
        this.scheduler = scheduler;
        this.dataSource = dataSource;
    }

    public <T extends Job> boolean schedule(final JobDetail jobDetail, final TriggerInfo info, final boolean isCron) {
        CronTrigger cronTrigger = null;
        Trigger simpleTrigger = null;

        if (isCron) {
            cronTrigger = SchedulerBuilder.buildCronTrigger(jobDetail.getJobClass(), info);
        } else {
            simpleTrigger = SchedulerBuilder.buildSimpleTrigger(jobDetail.getJobClass(), info);
        }

        try {

            if (scheduler.checkExists(jobDetail.getKey())) {
//                scheduler.deleteJob(jobDetail.getKey());
                LOG.error("Job: {} under Group: {} exists in the system, please create with a different name.", jobDetail.getKey().getName(), jobDetail.getKey().getGroup());
                return false;
            } else {
                LOG.info("{} job scheduled.", info.getCallbackData());
                LOG.info("Job key is {}.", jobDetail.getKey());

                if (isCron) {
                    LOG.info("Job trigger is {}.", cronTrigger);
                    scheduler.scheduleJob(jobDetail, cronTrigger);
                } else {
                    LOG.info("Job trigger is {}.", simpleTrigger);
                    scheduler.scheduleJob(jobDetail, simpleTrigger);
                }
                return true;
            }


        } catch (SchedulerException e) {
            LOG.error(e.getMessage(), e);
        }

        return false;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public static void setAppProperties(Properties appProperties) {
        SchedulerService.appProperties = appProperties;
    }

    public static void setJobProperties(Properties jobProperties) {
        SchedulerService.jobProperties = jobProperties;
    }

    public static void setGeneratedLogPath(String generatedLogPath) {
        SchedulerService.generatedLogPath = generatedLogPath;
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

    // for testing
//    public TriggerInfo createRecurringAdhocCopyJob() {
//        JobDetail jobDetail = JobBuilder.newJob(CopyJob.class)
//                .withIdentity("CopyJob", "TEST")
//                .build();
//
//        TriggerInfo info = new TriggerInfo();
//        info.setCronExp("0 51 16 * * ?");
//        info.setCallbackData("CopyJob");
//        info.setJobName("CopyJob");
//        info.setJobGroup("TEST");
//
//        schedule(jobDetail, info, true);
//        addAdHocJobLogger("CopyJob");
//
//        return info;
//    }

    // for testing
//    public TriggerInfo createOneTimeAdhocCopyJob(String jobDatetime) {
//        JobDetail jobDetail = JobBuilder.newJob(CopyJob.class)
//                .withIdentity("CopyJob", "TEST")
//                .build();
//
//        TriggerInfo info = new TriggerInfo();
//        info.setJobDatetime(jobDatetime);
//        info.setCallbackData("CopyJob");
//        info.setJobName("CopyJob");
//        info.setJobGroup("TEST");
//
//        schedule(jobDetail, info, false);
//        addAdHocJobLogger("CopyJob");
//
//        return info;
//
//    }

    public JobResult createOnetimeJob(String jobName, String jobDatetime, String commandValue, Boolean isServerScript, String jobGroup) {
        JobDetail jobDetail = _createJobDetail(jobName, commandValue, isServerScript, jobGroup);
        TriggerInfo info = _createJobTrigger(false, jobDatetime, jobName, jobGroup);
        boolean result = schedule(jobDetail, info, false);
        addAdHocJobLogger(jobName);

        return new JobResult(info, result);
    }

    public JobResult createRecurringJob(String jobName, String jobCronExp, String commandValue, Boolean isServerScript, String jobGroup) {
        JobDetail jobDetail = _createJobDetail(jobName, commandValue, isServerScript, jobGroup);
        TriggerInfo info = _createJobTrigger(true, jobCronExp, jobName, jobGroup);
        boolean result = schedule(jobDetail, info, true);
        addAdHocJobLogger(jobName);

        return new JobResult(info, result);
    }

    public boolean pauseJob(String jobName, String jobGroup) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);

        try {
            scheduler.pauseJob(jobKey);

            // Check if the job is paused
            boolean isPaused = _isJobPaused(jobKey);

            if (isPaused) {
                LOG.info("Job '{}' has been paused successfully.", jobKey);
                return true;
            } else {
                LOG.info("Job '{}' could not be paused. It may not exist or is already paused.", jobKey);
                return false;
            }
        } catch (SchedulerException e) {
            LOG.error(e);
        }

        return false;
    }

    public boolean pauseAllJobs() {
        try {
            scheduler.pauseAll();
        } catch (SchedulerException e) {
            LOG.error(e);
            return false;
        }

        return true;
    }

    public boolean resumeJob(String jobName, String jobGroup) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);

        try {
            scheduler.resumeJob(jobKey);

            // Check if the job is resumed
            boolean isResumed = _isJobResumed(jobKey);

            if (isResumed) {
                LOG.info("Job '{}' has been resumed successfully.", jobKey);
                return true;
            } else {
                LOG.info("Job '{}' could not be resumed. It may not exist or is already resumed.", jobKey);
                return false;
            }
        } catch (SchedulerException e) {
            LOG.error(e);
        }

        return false;
    }

    public boolean resumeAllJobs() {
        try {
            scheduler.resumeAll();
        } catch (SchedulerException e) {
            LOG.error(e);
            return false;
        }

        return true;
    }

    private boolean _isJobPaused(JobKey jobKey) throws SchedulerException {
        // Check if the job exists
        if (!scheduler.checkExists(jobKey)) {
            return false;
        }

        // Retrieve all triggers for the job
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

        // Check if all triggers are in the PAUSED state
        for (Trigger trigger : triggers) {
            if (!scheduler.getTriggerState(trigger.getKey()).equals(Trigger.TriggerState.PAUSED)) {
                return false; // Not paused
            }
        }
        return true; // All triggers are paused
    }

    private boolean _isJobResumed(JobKey jobKey) throws SchedulerException {
        // Check if the job exists
        if (!scheduler.checkExists(jobKey)) {
            return false;
        }

        // Retrieve all triggers for the job
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

        // Check if all triggers are in the PAUSED state
        for (Trigger trigger : triggers) {
            if (!scheduler.getTriggerState(trigger.getKey()).equals(Trigger.TriggerState.NORMAL)) {
                return false; // Not paused
            }
        }
        return true; // All triggers are paused
    }

    public boolean softDeleteJob(String jobName, String jobGroup) {
        JobUtils jobUtils = new JobUtils(dataSource);
        return jobUtils.softDeleteJob(jobName, jobGroup);
    }

    public boolean deleteJob(String jobName, String jobGroup) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);

        try {
            return scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            LOG.error(e);
        }

        return false;
    }

    public void shutdownQuartz() throws SchedulerException {
        scheduler.shutdown(true);
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

        if (appProperties.get("app.root") == "debug") {
            _xmlDebug(config);
        }
    }

    public static void reloadLog4jConfiguration(String configFilePath) {
        try {
            ConfigurationSource source = new ConfigurationSource(new FileInputStream(configFilePath));
            Configurator.initialize(null, source);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private JobDetail _createJobDetail(String jobName, String commandValue, Boolean isServerScript, String jobGroup) {
        String masterCommandValue = jobProperties.getProperty("master.map_drive.command");

        return JobBuilder.newJob(BatchJob.class)
                .withIdentity(jobName, jobGroup)
                .storeDurably()
                .usingJobData("command", commandValue)
                .usingJobData("master_command", masterCommandValue)
                .usingJobData("is_server_script", isServerScript)
                .usingJobData("folder", jobName)
                .build();
    }

    private TriggerInfo _createJobTrigger(boolean isRecurring, String triggerValue, String jobName, String jobGroup) {
        TriggerInfo info = new TriggerInfo();
        if (isRecurring) {
            info.setCronExp(triggerValue);
        } else {
            info.setJobDatetime(triggerValue);
        }

        info.setCallbackData(jobName);
        info.setJobName(jobName);
        info.setJobGroup(jobGroup);
        info.setTriggerName(jobName);
        info.setTriggerGroup(jobGroup);

        return info;
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