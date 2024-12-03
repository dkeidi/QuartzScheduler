package com.quartz.controller;

import com.quartz.info.TriggerInfo;
import com.quartz.services.SchedulerService;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/home")
public class JobsController {

    private final SchedulerService service;

    @Autowired
    public JobsController(SchedulerService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TriggerInfo>> getAllScheduledJobs() throws SchedulerException {
        List<TriggerInfo> jobs = service.getScheduledJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    //// LIST JOBS ////
    @GetMapping("/running")
    public ResponseEntity<List<TriggerInfo>> getAllRunningTimers() {
        List<TriggerInfo> jobs = service.getAllRunningJobs();
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @GetMapping("/{jobId}")
    public TriggerInfo getRunningJob(@PathVariable String jobId) {
        return service.getRunningJob(jobId);
    }


    //// CREATE JOBS ////
    @PostMapping("/create/adhoc")
//    public ResponseEntity<TriggerInfo> createAdhoc(String jobKey, Date jobDate, String scriptPath, Boolean scriptOnNetwork) {

    public ResponseEntity<TriggerInfo> createAdhoc(String cronExp) {
        TriggerInfo jobs= service.createAdhocJob("CopyJob", null, cronExp, "batch_files\\copy_file.bat" , false);
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

//    @PostMapping("/create/recurring")



    //// DELETE JOBS ////
//    @DeleteMapping("/{jobId}")
//    public Boolean deleteJob(@PathVariable String jobId) {
//        return service.deleteJob(jobId);
//    }
}