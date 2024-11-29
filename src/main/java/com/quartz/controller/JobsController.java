package com.quartz.controller;

import com.quartz.info.TriggerInfo;
import com.quartz.services.SchedulerService;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home")
public class JobsController {

    private final SchedulerService service;

    @Autowired
    public JobsController(SchedulerService service) {
        this.service = service;
    }

//    @PostMapping("/runhelloworld")
//    public void runHelloWorldJob() {
//        service.runHelloWorldJob();
//    }

    @GetMapping
    public List<TriggerInfo> getAllScheduledJobs() throws SchedulerException {
        return service.getScheduledJobs();
    }

    @GetMapping("/running")
    public List<TriggerInfo> getAllRunningTimers() {
        return service.getAllRunningJobs();
    }


    @GetMapping("/{jobId}")
    public TriggerInfo getRunningJob(@PathVariable String jobId) {
        return service.getRunningJob(jobId);
    }

//    @DeleteMapping("/{jobId}")
//    public Boolean deleteJob(@PathVariable String jobId) {
//        return service.deleteJob(jobId);
//    }
}
