package com.quartz;

import com.quartz.info.TriggerInfo;
import com.quartz.jobs.HelloWorldJob;
import com.quartz.jobs.CopyJob;
import com.quartz.jobs.MoveJob;
import com.quartz.timerservice.SchedulerService;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.quartz.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@SpringBootApplication
public class QuartzSchedulerApplication {

    private static final Logger LOG = LogManager.getLogger(QuartzSchedulerApplication.class);

    private final SchedulerService scheduler;

    @Autowired
    public QuartzSchedulerApplication(SchedulerService scheduler) {
        this.scheduler = scheduler;
    }

//    public static void main(String[] args) {
//        SpringApplication.run(QuartzSchedulerApplication.class, args).getBean(QuartzSchedulerApplication.class).scheduleJobs();
//    }

//    public void scheduleJobs() {
//        try (InputStream input = QuartzSchedulerApplication.class.getClassLoader().getResourceAsStream("job.properties")) {
//            Properties prop = new Properties();
//            prop.load(input);
//
//            LOG.debug(prop);
//            int jobCount = 0;
//
//            for (String jobName : prop.stringPropertyNames()) {
//                if (jobName.endsWith(".cron")) {
//                    String className = jobName.substring(0, jobName.length() - 5); // Remove the ".cron" suffix
//                    String cronExp = prop.getProperty(jobName);
//                    String classKey = "job." + className + ".class";
//                    Class<? extends Job> jobClass = (Class<? extends Job>) Class.forName(classKey);
//
//                    if (cronExp != null && jobClass != null) {
//                        scheduleJob(jobClass, cronExp);
//                        jobCount++;
//                    }
//                }
//            }
//
//            System.out.println("Scheduled " + jobCount + " jobs.");
//
//        } catch (IOException | ClassNotFoundException ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    private void scheduleJob(Class<? extends Job> jobClass, String cronExp) {
//        TriggerInfo info = new TriggerInfo();
//        info.setCronExp(cronExp);
//        info.setCallbackData(jobClass.getSimpleName());
//
//        // Schedule job using the scheduler instance
//        scheduler.schedule(jobClass, info);
//    }


    public static void main(String[] args) {
//        System.setProperty("BASE_PATH", "C:\\Users\\keidi.tay.chuan\\Documents\\quartz_logs");

//        QuartzSchedulerApplication.initializeYourLogger("app.log", "%d %p %c [%t] %m%n");

//        LOG.debug("Hello from Log4j 2");
//        LOG.debug("This is a Debug Message!");
//        LOG.info("This is an Info Message!");
//        try {
////            System.out.println(100/0);
//        } catch (Exception e) {
//            LOG.error("Error Occured", e);
//        }

        SpringApplication.run(QuartzSchedulerApplication.class, args).getBean(QuartzSchedulerApplication.class).scheduleJobs();
    }

    public void scheduleJobs() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("job.properties")) {

            if (input == null) {
                System.out.println("Sorry, unable to find job.properties");
                return;
            }

            Properties prop = new Properties();
            prop.load(input);

            int jobCount = 0;

            for (String jobName : prop.stringPropertyNames()) {
                if (jobName.endsWith(".cron")) {
                    String jobKey = jobName.substring(4, jobName.length() - 5); // Remove the ".cron" suffix
                    String cronExp = prop.getProperty(jobName);
                    String fullClassValue = "com.quartz.jobs." + jobKey;
                    String classKey = "job." + jobKey + ".class";
                    String commandKey = "job." + jobKey + ".command";
                    String commandValue = prop.getProperty(commandKey);

                    LOG.info("Processing jobKey: {}, cronExp: {}, classKey: {}, fullClassValue: {}, commandValue: {}",
                            jobKey, cronExp, classKey, fullClassValue, commandValue);

                    Class<? extends Job> jobClass = null;
                    try {
                        Class<?> clazz = Class.forName(fullClassValue);
                        if (!Job.class.isAssignableFrom(clazz)) {
                            LOG.error("Class {} does not implement Job interface", fullClassValue);
                            continue;
                        }
                        jobClass = (Class<? extends Job>) clazz;
                    } catch (ClassNotFoundException | ClassCastException e) {
                        LOG.error("Error loading job class for key: {}", classKey, e);
                        continue;
                    }

                    if (cronExp != null) {
                        scheduleJob(jobClass, cronExp, commandValue);
                        jobCount++;
                    }
                }
            }

            LOG.info("Scheduled {} jobs.", jobCount);
        } catch (IOException ex) {
            LOG.error("Error loading job properties", ex);
        }
    }

    private void scheduleJob(Class<? extends Job> jobClass, String cronExp, String command) {
        TriggerInfo info = new TriggerInfo();
        info.setCronExp(cronExp);
        info.setCallbackData(jobClass.getSimpleName());

        if (command != null) {
            info.setScriptLocation(command);
        }

        scheduler.schedule(jobClass, info);
    }

//    public void scheduleJobs() {
//        final TriggerInfo info = new TriggerInfo();
//
//        info.setCronExp("5 0/1 * * * ?"); // Run every min, at 5th second
//        info.setCallbackData("HelloWorldJob");
//        scheduler.schedule(HelloWorldJob.class, info);
//
//        info.setCronExp("0 39 15 * * ?"); // Run at this specific time every day
//        info.setCallbackData("CopyJob");
//        scheduler.schedule(CopyJob.class, info);
//
//        info.setCronExp("0 07 12 * * ?"); // Run at this specific time every day
//        info.setCallbackData("MoveJob");
//        scheduler.schedule(MoveJob.class, info);
//    }

//    public static void initializeYourLogger(String fileName, String pattern) {
//
//        ConfigurationBuilder<BuiltConfiguration> builder =
//                ConfigurationBuilderFactory.newConfigurationBuilder();
//
//        builder.setStatusLevel(Level.ERROR);
//        builder.setConfigurationName("RollingBuilder");
//
//        // create the console appender
//        AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
//        appenderBuilder.add(builder.newLayout("PatternLayout").
//                addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
//        builder.add(appenderBuilder);
//
//
//        LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
//                .addAttribute("pattern", "%d [%t] %-5level: %msg%n");
//        ComponentBuilder<?> triggeringPolicy = builder.newComponent("Policies")
//                .addComponent(builder.newComponent("CronTriggeringPolicy").addAttribute("schedule", "0 0 0 * * ?"))
//                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "100M"));
//        appenderBuilder = builder.newAppender("rolling", "RollingFile")
//                .addAttribute("fileName", "target/rolling.log")
//                .addAttribute("filePattern", "target/archive/rolling-%d{MM-dd-yy}.log.gz")
//                .add(layoutBuilder)
//                .addComponent(triggeringPolicy);
//        builder.add(appenderBuilder);
//
//        // create the new logger
//        builder.add(builder.newLogger("TestLogger", Level.DEBUG)
//                .add(builder.newAppenderRef("rolling"))
//                .addAttribute("additivity", false));
//
//        builder.add(builder.newRootLogger(Level.DEBUG)
//                .add(builder.newAppenderRef("rolling")));
//        Configurator.initialize(builder.build());
//    }
}
