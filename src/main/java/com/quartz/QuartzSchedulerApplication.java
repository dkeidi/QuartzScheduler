package com.quartz;

import com.quartz.jobs.BatchJob;
import com.quartz.util.Log4j2XmlGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@SpringBootApplication
public class QuartzSchedulerApplication {

    private static Logger LOG;

    @Autowired

    public static String getJarDir() {
        try {
            // Get the location of the JAR file
            File jarFile = new File(QuartzSchedulerApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            // Get the parent directory of the JAR file
            return jarFile.getParentFile().getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to determine the JAR file directory.", e);
        }
    }

    public static void main(String[] args) {
        try {
            // Determine the directory of the JAR file
            String jarDir = getJarDir();

            // Specify the location for log4j2.xml
            String log4jConfigFilePath = jarDir + File.separator + "log4j2.xml";

            // Generate log4j2.xml based on job.properties
            Properties jobProperties = JobPropertiesLoader.loadJobProperties(jarDir + File.separator + "job.properties");
            Log4j2XmlGenerator.generateLog4j2Xml(jobProperties, log4jConfigFilePath);

            // Set the log4j configuration file system property
            System.setProperty("log4j.configurationFile", log4jConfigFilePath);
            System.setProperty("log4j2.debug", "true");


            ConfigurationSource source = new ConfigurationSource(new FileInputStream(log4jConfigFilePath));
            Configurator.initialize(null, source);

            LOG = LogManager.getLogger(QuartzSchedulerApplication.class);

            SpringApplication.run(QuartzSchedulerApplication.class, args).getBean(QuartzSchedulerApplication.class).scheduleJobs();

        } catch (IOException e) {
            LOG.debug(e);
        }
    }

    public void scheduleJobs() {
        try (InputStream externalInput = Files.newInputStream(Paths.get("job.properties"))) {

            Properties prop = new Properties();
            prop.load(externalInput);

            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            for (String jobName : prop.stringPropertyNames()) {
                if (jobName.endsWith(".cron")) {
                    String jobKey = jobName.substring(4, jobName.length() - 5); // Remove the ".cron" suffix
                    String cronExp = prop.getProperty(jobName);
                    String commandKey = "job." + jobKey + ".command";
                    String commandValue = prop.getProperty(commandKey);

                    LOG.info("Processing job: {}, cron: {}, command: {}", jobKey, cronExp, commandValue);

                    scheduleJob(scheduler, jobKey, cronExp, commandValue);
                }
            }

            LOG.info("Scheduled all jobs.");
        } catch (IOException | SchedulerException ex) {
            LOG.error("Error scheduling jobs", ex);
        }
    }

    private void scheduleJob(Scheduler scheduler, String jobKey, String cronExp, String commandValue) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(BatchJob.class)
                .withIdentity(jobKey)
                .usingJobData("command", commandValue)
                .usingJobData("folder", jobKey)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobKey + "_trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExp))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }
}
