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
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

import org.apache.logging.log4j.core.layout.PatternLayout;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        addAdHocJobLogger(logFolderName, jobKey, jobKey + "/%d{dd-MM-yyyy}.log");


        return info;
    }

    public static void addAdHocJobLogger2(String logFolderName, String loggerName, String filePattern) throws IOException {

        ConfigurationBuilder<BuiltConfiguration> builder
                = ConfigurationBuilderFactory.newConfigurationBuilder();

        AppenderComponentBuilder rollingFile = builder.newAppender("LogToCopyJobFile", "RollingFile");
        rollingFile.addAttribute("fileName", logFolderName + "/logging.log");
        rollingFile.addAttribute("filePattern", "rolling-%d{MM-dd-yy}.log.gz");
        builder.add(rollingFile);

        LayoutComponentBuilder standard = builder.newLayout("PatternLayout");
        standard.addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable");
        rollingFile.add(standard);


        LoggerComponentBuilder logger2 = builder.newLogger("com.quartz.jobs.CopyJob", Level.DEBUG);
        logger2.add(builder.newAppenderRef("log"));
        logger2.addAttribute("additivity", false);

        builder.add(logger2);

        ComponentBuilder triggeringPolicies = builder.newComponent("Policies")
                .addComponent(builder.newComponent("CronTriggeringPolicy")
                        .addAttribute("schedule", "0 0 0 * * ?"))
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy")
                        .addAttribute("size", "100M"));

        rollingFile.addComponent(triggeringPolicies);

        builder.writeXmlConfiguration(System.out);

        Configurator.initialize(builder.build());
    }

    public static void addAdHocJobLogger(String logFolderName, String loggerName, String filePattern) {

        LoggerContext context = (LoggerContext) LogManager.getContext(false);

        Configuration config = context.getConfiguration();

        DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        System.out.println("Log file path: " + logFolderName + "/" + loggerName + "/" + LocalDateTime.now().format(DATE_FORMATTER) + ".log");

        // 2. Define the layout for the RollingFile appender
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("[%-5level] %d{dd-MM-yyyy HH:mm:ss.SSS} [%t] %c{1} - %msg%n")
                .build();

        // 3. Create the RollingFile appender for the ad-hoc job
        RollingFileAppender rollingFileAppender = RollingFileAppender.newBuilder()
                .setName("Log" + loggerName + "ToFile")
                .setLayout(layout)
                .withFileName(logFolderName + "/" + loggerName + "/" + LocalDateTime.now().format(DATE_FORMATTER) + ".log")
                .withFilePattern(filePattern + ".gz")
                .withPolicy(SizeBasedTriggeringPolicy.createPolicy("10MB"))
                .withPolicy(TimeBasedTriggeringPolicy.newBuilder().withInterval(1).withModulate(true).build())
                .setConfiguration(config)
                .build();

        rollingFileAppender.start();

        // 4. Add the appender to the configuration
        config.addAppender(rollingFileAppender);

        // 5. Define a logger reference for the new job
        AppenderRef rollingRef = AppenderRef.createAppenderRef(rollingFileAppender.getName(), null, null);
        AppenderRef consoleRef = AppenderRef.createAppenderRef("LogToConsole", null, null);
        AppenderRef jobToDbRef = AppenderRef.createAppenderRef("LogJobToDB", null, null);
        AppenderRef detailToDbRef = AppenderRef.createAppenderRef("LogDetailToDB", null, null);

        // Combine all appenders into a single array
        AppenderRef[] refs = new AppenderRef[]{rollingRef, consoleRef, jobToDbRef, detailToDbRef};

        // 6. Create or update the logger configuration for the ad-hoc job
        LoggerConfig loggerConfig = config.getLoggerConfig("com.quartz.jobs.CopyJob");

        LOG.info(loggerConfig);

        if (loggerConfig == null || loggerConfig.getName().equals("")) {
            // Logger doesn't exist, create a new one
            loggerConfig = LoggerConfig.createLogger(false, Level.INFO, loggerName, "true", refs, null, config, null);
            loggerConfig.addAppender(rollingFileAppender, Level.INFO, null);
            loggerConfig.addAppender(config.getAppender("LogToConsole"), Level.INFO, null); // Add existing appenders
            loggerConfig.addAppender(config.getAppender("LogJobToDB"), Level.INFO, null);
            loggerConfig.addAppender(config.getAppender("LogDetailToDB"), Level.INFO, null);

            // Add the logger configuration to the context
            config.addLogger("com.quartz.jobs.CopyJob", loggerConfig);
        } else {
            // Logger exists, update its appenders
            loggerConfig.addAppender(rollingFileAppender, Level.INFO, null);
            if (!loggerConfig.getAppenders().containsKey("LogToConsole")) {
                loggerConfig.addAppender(config.getAppender("LogToConsole"), Level.INFO, null);
            }
            if (!loggerConfig.getAppenders().containsKey("LogJobToDB")) {
                loggerConfig.addAppender(config.getAppender("LogJobToDB"), Level.INFO, null);
            }
            if (!loggerConfig.getAppenders().containsKey("LogDetailToDB")) {
                loggerConfig.addAppender(config.getAppender("LogDetailToDB"), Level.INFO, null);
            }
        }

        // 7. Update the logger context
        context.updateLoggers();

        // Reload the Log4j2 configuration
        reloadLog4jConfiguration("C:/Users/keidi.tay.chuan/Documents/MyQuartzTest/log4j2.xml");

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
            logger.getAppenderRefs().forEach(ref -> {
                System.out.println("    - " + ref.getRef());
            });
        });
    }

    public static void reloadLog4jConfiguration(String configFilePath) {
        try {
            ConfigurationSource source = new ConfigurationSource(new FileInputStream(configFilePath));
            Configurator.initialize(null, source);
        }catch (IOException e) {
            LOG.error(e);
        }
    }

}