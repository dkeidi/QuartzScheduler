package com.quartz;

import com.quartz.jobs.BatchJob;
import com.quartz.jobs.CopyJob;
import com.quartz.jobs.HelloWorldJob;
import com.quartz.model.User;
import com.quartz.repo.UserRepo;
import com.quartz.services.SchedulerService;
import com.quartz.util.JobPropertiesLoader;
import com.quartz.util.Log4j2XmlGenerator;
import com.quartz.info.TriggerInfo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
public class QuartzSchedulerApplication {

    private static Logger LOG;
    private static SchedulerService scheduler;

    @Autowired
    public static String getJarDir() {
        try {
            // Retrieve the path as a URL and convert it to a URI
            URI uri = QuartzSchedulerApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI();

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
            return Paths.get(new URI(path)).toAbsolutePath().toString();

        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to determine the JAR file path.", e);
        }
    }

    public QuartzSchedulerApplication(SchedulerService scheduler) {
        QuartzSchedulerApplication.scheduler = scheduler;
    }

    public static void main(String[] args) {
        boolean readFromExternalProperties = false; //will get from properties file
        if (readFromExternalProperties) {
            jobsFromProperties(args);
        } else {
            // Run the application and get the application context
            ApplicationContext context = SpringApplication.run(QuartzSchedulerApplication.class, args);

            // Retrieve the QuartzSchedulerApplication bean and call scheduleFixedJobs
            QuartzSchedulerApplication app = context.getBean(QuartzSchedulerApplication.class);
//            app.scheduleFixedJobs(args);

//            User user1 = context.getBean(User.class);
//            user1.setId(111);
//            user1.setName("kei");
//            user1.setGender("JAVA");
//
//            UserRepo repo = context.getBean(UserRepo.class);
//            repo.save(user1);
//
//            System.out.println(repo.findAll());
        }
    }

//    private static void scheduleFixedJobs(String[] args) {
//        final TriggerInfo info = new TriggerInfo();
//
//        info.setCronExp("0/10 * * * * ?"); // Run every min, at 5th second
//        info.setCallbackData("HelloWorldJob");
//        scheduler.schedule(HelloWorldJob.class, info);
//
//        info.setCronExp("0 20 14 * * ?"); // Run at this specific time every day
//        info.setCallbackData("CopyJob");
//        scheduler.schedule(CopyJob.class, info);
//    }

    private static void jobsFromProperties(String[] args) {
        try {
            // Determine the directory of the JAR file
            String jarDir = getJarDir();
            System.out.println("here " + jarDir);

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

            SpringApplication.run(QuartzSchedulerApplication.class, args).getBean(QuartzSchedulerApplication.class).scheduleJobsFromProperties();

            //get data from DB
            ApplicationContext context = SpringApplication.run(QuartzSchedulerApplication.class, args);
//            UserRepo repo = context.getBean(UserRepo.class);
//            User user1 = context.getBean(User.class);
//            user1.setId(111);
//            user1.setName("kei");
//            user1.setGender("JAVA");
//
//            repo.save(user1);
//            System.out.println("repo: " + repo.findAll());

        } catch (IOException e) {
            LOG.debug(e);
        }
    }

    public void scheduleJobsFromProperties() {
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

//    public void scheduleJobs() {
//        String sql = "select * from [user];";
//        List<User> users  = jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(User.class));
//
//        users.forEach(System.out :: println);
//
//
//        try (InputStream externalInput = Files.newInputStream(Paths.get("job.properties"))) {
//
//            Properties prop = new Properties();
//            prop.load(externalInput);
//
//            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
//            scheduler.start();
//
//            for (String jobName : prop.stringPropertyNames()) {
//                if (jobName.endsWith(".cron")) {
//                    String jobKey = jobName.substring(4, jobName.length() - 5); // Remove the ".cron" suffix
//                    String cronExp = prop.getProperty(jobName);
//                    String commandKey = "job." + jobKey + ".command";
//                    String commandValue = prop.getProperty(commandKey);
//
//                    LOG.info("Processing job: {}, cron: {}, command: {}", jobKey, cronExp, commandValue);
//
//                    scheduleJob(scheduler, jobKey, cronExp, commandValue);
//                }
//            }
//
//            LOG.info("Scheduled all jobs.");
//        } catch (IOException | SchedulerException ex) {
//            LOG.error("Error scheduling jobs", ex);
//        }
//    }
//
//    private void scheduleJob(Scheduler scheduler, String jobKey, String cronExp, String commandValue) throws SchedulerException {
//        JobDetail jobDetail = JobBuilder.newJob(BatchJob.class)
//                .withIdentity(jobKey)
//                .usingJobData("command", commandValue)
//                .usingJobData("folder", jobKey)
//                .build();
//
//        Trigger trigger = TriggerBuilder.newTrigger()
//                .withIdentity(jobKey + "_trigger")
//                .withSchedule(CronScheduleBuilder.cronSchedule(cronExp))
//                .build();
//
//        scheduler.scheduleJob(jobDetail, trigger);
//    }
//
//    private void query() {
//        String url = "jdbc:sqlserver://localhost:1433;databaseName=quartz_scheduler;encrypt=true;trustServerCertificate=true;integratedSecurity=true;";
//
//        // Specify the path to sqljdbc_auth.dll if necessary
//        System.setProperty("java.library.path", "C:\\Program Files\\Java\\jdk-17\\bin\\sqljdbc_auth.dll");
//
//        // Optionally, you might need to refresh the library path (only necessary in some environments)
//        System.setProperty("sun.boot.library.path", System.getProperty("java.library.path"));
//
//        String user="";
//        String password="";
//        try {
//            Connection conn = DriverManager.getConnection(url, user, password);
//            LOG.info("Connection successful");
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//    @Override
//    public void run(String... args) throws Exception {
//
//        String sql = "select * from [user];";
//        List<User> users  = jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(User.class));
//
//        users.forEach(System.out :: println);
//    }
}
