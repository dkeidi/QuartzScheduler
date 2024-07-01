package com.quartz.playground;

import com.quartz.info.TriggerInfo;
import com.quartz.timerservice.SchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlaygroundService {
    private final SchedulerService scheduler;

    @Autowired
    public PlaygroundService(final SchedulerService scheduler) {
        this.scheduler = scheduler;
    }

    public void runHelloWorldJob() {
        final TriggerInfo info = new TriggerInfo();
//        info.setTotalFireCount(5);
//        info.setRemainingFireCount(info.getTotalFireCount());
//        info.setRepeatIntervalMs(5000);
//        info.setInitialOffsetMs(1000);
//        info.setCallbackData("My callback data");

//        scheduler.schedule(HelloWorldJob.class, info);
    }

    public void runTransferJob() {
        final TriggerInfo info = new TriggerInfo();
//        info.setTotalFireCount(1);
//        info.setRemainingFireCount(info.getTotalFireCount());
//        info.setRepeatIntervalMs(5000);
//        info.setInitialOffsetMs(1000);
        info.setCallbackData("Done?");

//        scheduler.schedule(TransferJob.class, info);
    }

    public Boolean deleteTimer(final String timerId) {
        return scheduler.deleteTimer(timerId);
    }

    public List<TriggerInfo> getAllRunningTimers() {
        return scheduler.getAllRunningTimers();
    }

    public TriggerInfo getRunningTimer(final String timerId) {
        return scheduler.getRunningTimer(timerId);
    }
}