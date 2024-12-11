package com.quartz.controller;

import com.quartz.info.TriggerInfo;
import com.quartz.model.JobResult;
import com.quartz.services.SchedulerService;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @PostMapping("/create_adhoc_job")
    public ResponseEntity<JobResult> createAdhocJob(Boolean is_recurring, String cron_expression, String job_datetime, String job_name, String script_filepath, Boolean is_server_script, String job_group) {
        JobResult job = null;

        if (is_recurring) {
            job = service.createRecurringJob(job_name, cron_expression, script_filepath, is_server_script, job_group);
        } else {
            job = service.createOnetimeJob(job_name, job_datetime, script_filepath, is_server_script, job_group);
        }

        HttpStatus status = job.isSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(job, status);
    }

    //// PAUSE JOBS ////
    @PostMapping("/pause")
    public ResponseEntity<String> pauseJob(String job_name, String job_group) {
        Map<String, Object> response = new HashMap<>();
        boolean isPaused = service.pauseJob(job_name, job_group);

        if (isPaused) {
            response.put("status", "success");
            response.put("message", job_name + " has been paused successfully.");
        } else {
            response.put("status", "failure");
            response.put("message", job_name + " could not be paused. Check if it exists.");
        }

        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

    @PostMapping("/pause_all")
    public ResponseEntity<String> pauseAllJobs() {
        Map<String, Object> response = new HashMap<>();
        boolean isPaused = service.pauseAllJobs();

        if (isPaused) {
            response.put("status", "success");
            response.put("message", "All jobs has been paused successfully.");
        } else {
            response.put("status", "failure");
            response.put("message", "Pause all jobs error.");
        }

        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

    //// RESUME JOBS ////
    @PostMapping("/resume")
    public ResponseEntity<String> resumeJob(String job_name, String job_group) {
        Map<String, Object> response = new HashMap<>();
        boolean isResumed = service.resumeJob(job_name, job_group);

        if (isResumed) {
            response.put("status", "success");
            response.put("message", job_name + " has been resumed successfully.");
        } else {
            response.put("status", "failure");
            response.put("message", job_name + " could not be resumed. Check if it exists.");
        }

        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

    @PostMapping("/resume_all")
    public ResponseEntity<String> resumeAllJobs() {
        Map<String, Object> response = new HashMap<>();
        boolean isPaused = service.resumeAllJobs();

        if (isPaused) {
            response.put("status", "success");
            response.put("message", "All jobs has been resumed successfully.");
        } else {
            response.put("status", "failure");
            response.put("message", "Resume all jobs error.");
        }

        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }


    //// DELETE JOBS ////
    @PostMapping("/soft_delete")
    public ResponseEntity<String> softDeleteJob(String job_name, String job_group) {
        Map<String, Object> response = new HashMap<>();
        boolean isDeleted = service.softDeleteJob(job_name, job_group);

        if (isDeleted) {
            response.put("status", "success");
            response.put("message", job_name + " has been deleted successfully.");
        } else {
            response.put("status", "failure");
            response.put("message", job_name + " could not be deleted. Check if it exists.");
        }

        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public Boolean deleteJob(String job_name, String job_group) {
        Map<String, Object> response = new HashMap<>();
        boolean isDeleted = service.deleteJob(job_name, job_group);

        if (isDeleted) {
            response.put("status", "success");
            response.put("message", job_name + " has been deleted successfully.");
        } else {
            response.put("status", "failure");
            response.put("message", job_name + " could not be deleted. Check if it exists.");
        }

        return new ResponseEntity<>(response.toString(), HttpStatus.OK).hasBody();
    }

    //// SHUTDOWN APPLICATION ////
    @GetMapping("/shutdown")
    public ResponseEntity<List<TriggerInfo>> shutdown() throws SchedulerException {
        service.shutdownQuartz();
        System.exit(0);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}