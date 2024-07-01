package com.quartz;

import com.quartz.info.TriggerInfo;
import com.quartz.jobs.HelloAppleJob;
import com.quartz.jobs.HelloWorldJob;
import com.quartz.timerservice.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuartzSchedulerApplication {

	private static final Logger LOG = LoggerFactory.getLogger(QuartzSchedulerApplication.class);

	private final SchedulerService scheduler;

	@Autowired
	public QuartzSchedulerApplication(SchedulerService scheduler) {
		this.scheduler = scheduler;
	}

	public static void main(String[] args) {
		SpringApplication.run(QuartzSchedulerApplication.class, args).getBean(QuartzSchedulerApplication.class).runHelloWorldJob();
	}

	public void runHelloWorldJob() {
		LOG.info("schedule job");
		final TriggerInfo info = new TriggerInfo();
		info.setCronExp("2 0/1 * * * ?");
		scheduler.schedule(HelloWorldJob.class, info);
		info.setCronExp("5 0/2 * * * ?");
		scheduler.schedule(HelloAppleJob.class, info);
	}

}
