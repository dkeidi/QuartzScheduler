package com.quartz;

import com.quartz.info.TriggerInfo;
import com.quartz.jobs.BatchJob;
import com.quartz.jobs.CopyJob;
import com.quartz.jobs.HelloWorldJob;
import com.quartz.services.SchedulerService;
import com.quartz.tests.MisfireExample;
import com.quartz.tests.JobExceptionExample;
import com.quartz.util.JobPropertiesLoader;
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
import java.util.Properties;

@SpringBootApplication
@PropertySource("classpath:application.properties")
public class QuartzSchedulerApplication {

    private static Logger LOG;
    private static SchedulerService scheduler;

    @Autowired

    public QuartzSchedulerApplication(SchedulerService scheduler) {
        QuartzSchedulerApplication.scheduler = scheduler;
    }

    //for RAMJobStore jobstore
//    public static String getJarDir() {
//        try {
//            // Get the location of the JAR file
//            File jarFile = new File(QuartzSchedulerApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
//            // Get the parent directory of the JAR file
//            return jarFile.getParentFile().getAbsolutePath();
//        } catch (URISyntaxException e) {
//            throw new RuntimeException("Failed to determine the JAR file directory.", e);
//        }
//    }
//
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

    public static void main(String[] args) throws Exception {

//        if (false) {
//            _jobsFromExternalProperties(args);
//        } else {
//            ApplicationContext context = SpringApplication.run(QuartzSchedulerApplication.class, args);
//
//            // Retrieve the QuartzSchedulerApplication bean and call scheduleFixedJobs
//            QuartzSchedulerApplication app = context.getBean(QuartzSchedulerApplication.class);
//            app._scheduleFixedJobs();
//
//            scheduler.getScheduledJobs();
//        }


        //misfire test
//        MisfireExample example = new MisfireExample();
//        example.run();

        //exception test
        JobExceptionExample example = new JobExceptionExample();
        example.run();
    }

    private void _scheduleFixedJobs() {
        final TriggerInfo info = new TriggerInfo();

        info.setCronExp("0/10 3,4 18 * * ?"); // Run every min, at 5th second
        info.setCallbackData("HelloWorldJob");
        scheduler.schedule(HelloWorldJob.class, info);

        info.setCronExp("0 30 18 * * ?"); // Run at this specific time every day
        info.setCallbackData("CopyJob");
        scheduler.schedule(CopyJob.class, info);
    }

    private static void _jobsFromExternalProperties(String[] args) {
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

            SpringApplication.run(QuartzSchedulerApplication.class, args).getBean(QuartzSchedulerApplication.class)._scheduleJobsFromProperties();

        } catch (IOException e) {
            System.out.println(e);
//            LOG.debug(e);
        }
    }

    private void _scheduleJobsFromProperties() {
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

                    _scheduleJob(scheduler, jobKey, cronExp, commandValue);
                }
            }

            LOG.info("Scheduled all jobs.");
        } catch (IOException | SchedulerException ex) {
            LOG.error("Error scheduling jobs", ex);
        }
    }

    private void _scheduleJob(Scheduler scheduler, String jobKey, String cronExp, String commandValue) throws SchedulerException {
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
