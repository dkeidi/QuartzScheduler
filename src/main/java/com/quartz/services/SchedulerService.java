package com.quartz.services;

import com.quartz.info.TriggerInfo;
import com.quartz.util.SchedulerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SchedulerService {
    private static final Logger LOG = LogManager.getLogger(SchedulerService.class);
    private final Scheduler scheduler;

    @Autowired
    public SchedulerService(final Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public <T extends Job> void schedule(final JobDetail jobDetail, final TriggerInfo info) {
//        final JobDetail jobDetail = SchedulerBuilder.buildJobDetail(jobClass, info);
        final CronTrigger trigger = SchedulerBuilder.buildTrigger(jobDetail.getJobClass(), info);

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

    public <T extends Job> void scheduleManual(final Class<T> jobClass, final TriggerInfo info) {
        final JobDetail jobDetail = SchedulerBuilder.buildJobDetail(jobClass, info);
        final CronTrigger trigger = SchedulerBuilder.buildTrigger(jobClass, info);

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

    public void getScheduledJobs() throws SchedulerException {

        LOG.info("getScheduledJobs");

        for (String groupName : scheduler.getJobGroupNames()) {

            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                String jobName = jobKey.getName();
                String jobGroup = jobKey.getGroup();

                //get job's trigger
                List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
                Date nextFireTime = triggers.get(0).getNextFireTime();

                LOG.info("[jobName] : " + jobName + " [groupName] : " + jobGroup + " - " + nextFireTime);

            }
        }
    }

    public List<TriggerInfo> getAllRunningTimers() {
        try {
            // jobs belong to a group
            // iterate through all the jobs
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

    public TriggerInfo getRunningTimer(String timerId) {
        try {
            final JobDetail jobDetail = scheduler.getJobDetail(new JobKey(timerId));
            if (jobDetail == null) {
                LOG.error("Failed to find timer with ID '{}'", timerId);
                return null;
            }

            return (TriggerInfo) jobDetail.getJobDataMap().get(timerId);
        } catch (final SchedulerException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public void updateTimer(final String timerId, final TriggerInfo info) {
        try {
            final JobDetail jobDetail = scheduler.getJobDetail(new JobKey(timerId));
            if (jobDetail == null) {
                LOG.error("Failed to find timer with ID '{}'", timerId);
                return;
            }

            jobDetail.getJobDataMap().put(timerId, info);

            scheduler.addJob(jobDetail, true, true);
        } catch (final SchedulerException e) {
            LOG.error(e.getMessage(), e);
        }
    }
}