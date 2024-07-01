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
		LOG.info("scheduled 3 jobs");
		final TriggerInfo info = new TriggerInfo();

		info.setCronExp("5 0/1 * * * ?"); // Run every min, at 5th second
		info.setCallbackData("HelloWorldJob");
		scheduler.schedule(HelloWorldJob.class, info);

		info.setCronExp("0 55 15 * * ?"); // Run at this specific time every day
		info.setCallbackData("CopyJob");
		scheduler.schedule(CopyJob.class, info);

		info.setCronExp("0 55 15 * * ?"); // Run at this specific time every day
		info.setCallbackData("MoveJob");
		scheduler.schedule(MoveJob.class, info);
	}
}
