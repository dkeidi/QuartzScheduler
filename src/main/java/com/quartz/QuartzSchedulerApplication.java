package com.quartz;

import com.quartz.info.TriggerInfo;
import com.quartz.jobs.HelloWorldJob;
import com.quartz.jobs.CopyJob;
import com.quartz.jobs.MoveJob;
import com.quartz.timerservice.SchedulerService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@SpringBootApplication
public class QuartzSchedulerApplication {

    private static final Logger LOG = LogManager.getLogger(QuartzSchedulerApplication.class);

    private final SchedulerService scheduler;

    @Autowired
    public QuartzSchedulerApplication(SchedulerService scheduler) {
        this.scheduler = scheduler;
    }

    public static void main(String[] args) {
        SpringApplication.run(QuartzSchedulerApplication.class, args).getBean(QuartzSchedulerApplication.class).scheduleJobs();
    }

    public void scheduleJobs() {
        try (InputStream input = QuartzSchedulerApplication.class.getClassLoader().getResourceAsStream("job.properties")) {

            Properties prop = new Properties();
            prop.load(input);

            int jobCount = 0;

            // Iterate over job configurations
            while (true) {
                String cronKey = "job." + jobCount + ".cron";
                String classKey = "job." + jobCount + ".class";

                String cronExp = prop.getProperty(cronKey);
                String className = prop.getProperty(classKey);

                if (cronExp == null || className == null) {
                    break; // No more jobs defined
                }

                // Schedule job based on properties
                scheduleJob(className, cronExp);

                jobCount++;
            }

            System.out.println("Scheduled " + jobCount + " jobs.");

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void scheduleJob(String className, String cronExp) {
        TriggerInfo info = new TriggerInfo();
        info.setCronExp(cronExp);
        info.setCallbackData(className);

        // Replace this with your actual scheduler logic
        switch (className) {
            case "HelloWorldJob":
                scheduler.schedule(HelloWorldJob.class, info);
                break;
            case "CopyJob":
                scheduler.schedule(CopyJob.class, info);
                break;
            case "MoveJob":
                scheduler.schedule(MoveJob.class, info);
                break;
            default:
                System.out.println("Unknown job class: " + className);
        }
    }
}
