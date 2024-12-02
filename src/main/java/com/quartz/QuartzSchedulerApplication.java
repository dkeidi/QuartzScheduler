package com.quartz;

import com.quartz.info.TriggerInfo;
import com.quartz.jobs.BatchJob;
import com.quartz.jobs.CopyJob;
import com.quartz.jobs.HelloWorldJob;
import com.quartz.services.SchedulerService;
import com.quartz.util.PropertiesLoader;
import com.quartz.util.Log4j2XmlGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

@SpringBootApplication
@PropertySource("classpath:application.properties")
public class QuartzSchedulerApplication {

    private static Logger LOG;
    private static SchedulerService scheduler;
    public static Properties jobProperties;
    public static Properties appProperties;
    public static String log4jConfigFilePath;

    @Autowired

    public QuartzSchedulerApplication(SchedulerService scheduler) {QuartzSchedulerApplication.scheduler = scheduler;}

    public static void main(String[] args) throws SchedulerException, IOException {
        loadProperties();

        if (Boolean.parseBoolean(appProperties.getProperty("app.readFromExternalProperties"))) {
            _jobsFromExternalProperties(args);
        } else {
            ApplicationContext context = SpringApplication.run(QuartzSchedulerApplication.class, args);
            QuartzSchedulerApplication app = context.getBean(QuartzSchedulerApplication.class);
            app._scheduleFixedJobs();
            scheduler.getScheduledJobs();
        }

    }

    public static void loadProperties() throws IOException {
        // Determine the directory of the JAR file
        String jarDir = getJarDir();
        // Specify the location for log4j2.xml
        log4jConfigFilePath = jarDir + File.separator + "log4j2.xml";

        // Generate log4j2.xml based on job.properties and app.properties
        jobProperties = PropertiesLoader.loadProperties(jarDir + File.separator + "job.properties");
        appProperties = PropertiesLoader.loadProperties(jarDir + File.separator + "application.properties");
    }

    public static String getJarDir() {
        try {
            // Retrieve the path as a URL and convert it to a URI
            URI uri = QuartzSchedulerApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI();

            // Check if it's a file URI
            if (uri.getScheme().equals("file")) {
                File jarFile = new File(uri);
                return jarFile.getParentFile().getAbsolutePath();
            }

            // Convert to a String and remove the prefix if it starts with "jar:"
            String path = uri.toString();

            if (path.startsWith("jar:")) {
                path = path.substring(4); // Strip off "jar:"
            }

            // Locate the .jar segment and trim everything after it
            int jarEndIndex = path.indexOf(".jar");
            if (jarEndIndex != -1) {
                path = path.substring(0, jarEndIndex + 4); // ".jar" has 4 characters
            }

            // Convert back to URI, then to Path, and get the absolute path
            int lastIndex = Paths.get(new URI(path)).toAbsolutePath().toString().lastIndexOf('\\');

            if (lastIndex == -1) {
                return Paths.get(new URI(path)).toAbsolutePath().toString();
            }

            return Paths.get(new URI(path)).toAbsolutePath().toString().substring(0, lastIndex);

        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to determine the JAR file path.", e);
        }
    }

    private void _scheduleFixedJobs() {
//        info.setCronExp("0/5 38 15 * * ?"); // Run every min, at 5th second
//        info.setCallbackData("HelloWorldJob");
//        scheduler.schedule(HelloWorldJob.class, info);
//
//        info.setCronExp("0 34 10 * * ?"); // Run at this specific time every day
//        info.setCallbackData("CopyJob");
//        scheduler.schedule(CopyJob.class, info);

//        JobDetail jobDetail = JobBuilder.newJob(HelloWorldJob.class)
//                .withIdentity("HelloWorldJob")
//                .build();
//
//        TriggerInfo info1 = new TriggerInfo();
//        info1.setCronExp("0/5 8 19 * * ?");
//        info1.setCallbackData("HelloWorldJob");
//        info1.setJobName("HelloWorldJob");
//
//        scheduler.schedule(jobDetail, info1);


//        JobDetail jobDetail = JobBuilder.newJob(CopyJob.class)
//                .withIdentity("CopyJob")
//                .build();
//
//        TriggerInfo info2 = new TriggerInfo();
//        info2.setCronExp("0 51 16 * * ?");
//        info2.setCallbackData("CopyJob");
//        info2.setJobName("CopyJob");
//
//        scheduler.schedule(jobDetail, info2);
    }

    private static void _jobsFromExternalProperties(String[] args) {
        try {

            Log4j2XmlGenerator.generateLog4j2Xml(jobProperties, appProperties, log4jConfigFilePath, Boolean.parseBoolean(appProperties.getProperty("app.isJDBC")));

            // Set the log4j configuration file system property
            System.setProperty("log4j.configurationFile", log4jConfigFilePath);
            System.setProperty("log4j2.debug", "true");

            ConfigurationSource source = new ConfigurationSource(new FileInputStream(log4jConfigFilePath));
            Configurator.initialize(null, source);

            LOG = LogManager.getLogger(QuartzSchedulerApplication.class);

            // .run has to come after Configurator for LOG4J2 to work
            SpringApplication.run(QuartzSchedulerApplication.class, args).getBean(QuartzSchedulerApplication.class)._scheduleJobsFromProperties(scheduler);

        } catch (IOException e) {
            LOG.debug(e);
        }
    }

    private void _scheduleJobsFromProperties(SchedulerService scheduler) {
        try (InputStream externalInput = Files.newInputStream(Paths.get("job.properties"))) {

            Properties prop = new Properties();
            prop.load(externalInput);

            String masterCommandValue = prop.getProperty("master.map_drive.command");
            Boolean isNetworkLocation = Boolean.parseBoolean(prop.getProperty("master.is_network_location"));


            for (String jobName : prop.stringPropertyNames()) {
                if (jobName.startsWith("job.") && jobName.endsWith(".cron")) {
                    String jobKey = jobName.substring(4, jobName.length() - 5); // Remove the ".cron" suffix
                    String cronExp = prop.getProperty(jobName);
                    String commandKey = "job." + jobKey + ".command";
                    String commandValue = prop.getProperty(commandKey);

                    LOG.info("Processing job: {}, cron: {}, command: {}", jobKey, cronExp, commandValue);

                    _scheduleJob(scheduler, jobKey, cronExp, commandValue, masterCommandValue, isNetworkLocation);
                }
            }

            scheduler.getScheduledJobs();
            LOG.info("Scheduled all jobs.");
        } catch (IOException | SchedulerException ex) {
            LOG.error("Error scheduling jobs", ex);
        }
    }

    private void _scheduleJob(SchedulerService scheduler, String jobKey, String cronExp, String commandValue, String masterCommandValue, Boolean isNetworkLocation) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(BatchJob.class)
                .withIdentity(jobKey)
                .usingJobData("command", commandValue)
                .usingJobData("folder", jobKey)
                .build();

        TriggerInfo info = new TriggerInfo();
        info.setCronExp(cronExp);
        info.setCallbackData(jobKey);
        info.setJobName(jobKey);

        scheduler.schedule(jobDetail, info, true);
    }
}
