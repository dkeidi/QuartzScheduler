package com.quartz;

import com.quartz.info.TriggerInfo;
import com.quartz.jobs.BatchJob;
import com.quartz.services.SchedulerService;
import com.quartz.util.Log4j2XmlGenerator;
import com.quartz.util.PropertiesLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
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
    public static Properties jobProperties;
    public static Properties appProperties;
    public static String log4jConfigFilePath;

    @Autowired

    public QuartzSchedulerApplication(SchedulerService scheduler) {
        QuartzSchedulerApplication.scheduler = scheduler;
    }

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
//        System.out.println("loadProperties");
        // Determine the directory of the JAR file
        String jarDir = getJarDir();
        // Specify the location for log4j2.xml
        log4jConfigFilePath = jarDir + File.separator + "log4j2.xml";
//        System.out.println("log4jConfigFilePath: " + log4jConfigFilePath);
        // Generate log4j2.xml based on job.properties and app.properties
        jobProperties = PropertiesLoader.loadProperties(jarDir + File.separator + "job.properties");
//        System.out.println("jobProperties: " + jarDir + File.separator + "job.properties");
        appProperties = PropertiesLoader.loadProperties(jarDir + File.separator + "application.properties");
        SchedulerService.setAppProperties(appProperties);
        SchedulerService.setJobProperties(jobProperties);
        SchedulerService.setGeneratedLogPath(log4jConfigFilePath);
//        System.out.println("end loadProperties");

    }

    public static String getJarDir() {
//        System.out.println("getJarDir");
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
//            System.out.println(path);

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
//        System.out.println("_jobsFromExternalProperties");
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
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();
            _xmlDebug(context, config);

        } catch (IOException e) {
            LOG.debug(e);
        }
    }

    private void _scheduleJobsFromProperties(SchedulerService scheduler) {
//        System.out.println("_scheduleJobsFromProperties");
//        System.out.println(Paths.get("job.properties"));
        try (InputStream externalInput = Files.newInputStream(Paths.get("job.properties"))) {

            Properties prop = new Properties();
            prop.load(externalInput);

            String masterCommandValue = prop.getProperty("master.map_drive.command");
            Boolean isServerScript = Boolean.parseBoolean(prop.getProperty("master.is_server_script"));

            for (String jobProperty : prop.stringPropertyNames()) {
                if (jobProperty.startsWith("job.") && jobProperty.endsWith(".cron")) {
                    String jobName = jobProperty.substring(4, jobProperty.length() - 5); // Remove the ".cron" suffix
                    String cronExp = prop.getProperty(jobProperty);
                    String commandKey = "job." + jobName + ".command";
                    String commandValue = prop.getProperty(commandKey);
                    String jobGroupKey = "job." + jobName + ".jobgroup";
                    String jobGroupValue = prop.getProperty(jobGroupKey);
                    String triggerGroupKey = "job." + jobName + ".triggergroup";
                    String triggerGroupValue = prop.getProperty(triggerGroupKey);

                    LOG.info("Processing job: {}, cron: {}, command: {}", jobName, cronExp, commandValue);

                    _scheduleJob(scheduler, jobName, jobGroupValue, cronExp, commandValue, masterCommandValue, isServerScript);
                }
            }
            LOG.info("Scheduled all jobs.");
//            LOG.info("Paused all jobs.");
//            scheduler.pauseAllJobs();

            scheduler.getScheduledJobs();

        } catch (IOException | SchedulerException ex) {
            LOG.error("Error scheduling jobs", ex);
        }
    }

    private void _scheduleJob(SchedulerService scheduler, String jobName, String jobGroup, String cronExp, String commandValue, String masterCommandValue, Boolean isServerScript) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(BatchJob.class)
                .withIdentity(jobName, jobGroup)
                .storeDurably()
                .usingJobData("command", commandValue)
                .usingJobData("master_command", masterCommandValue)
                .usingJobData("is_server_script", isServerScript)
                .usingJobData("folder", jobName)
                .build();

        TriggerInfo info = new TriggerInfo();
        info.setCronExp(cronExp);
        info.setCallbackData(jobName);
        info.setJobName(jobName);
        info.setJobGroup(jobGroup);
        info.setTriggerName(jobName);
        info.setTriggerGroup(jobGroup);

        scheduler.schedule(jobDetail, info, true);
    }

    private static void _xmlDebug(LoggerContext context, Configuration config) {
        LOG.info("context");
        LOG.info(context);
        LOG.info("config");
        LOG.info(config);

        System.out.println("##########LOGGERS############");

        // Inspect loggers
        config.getLoggers().forEach((name, logger) -> {
            System.out.println("Logger Name: " + name);
            System.out.println("Logger Level: " + logger.getLevel());
            System.out.println("Appender References: ");
            for (AppenderRef ref : logger.getAppenderRefs()) {
                System.out.println("    - " + ref.getRef());
            }
        });

        System.out.println("##########APPENDERS############");

        // Inspect appenders
        config.getAppenders().forEach((name, appender) -> {
            System.out.println("Appender Name: " + name);
            System.out.println("Appender Type: " + appender.getClass().getName());
            if (appender.getLayout() != null) {
                System.out.println("Appender Layout: " + appender.getLayout().getClass().getName());
            }
        });
    }

}
