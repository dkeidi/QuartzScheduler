package com.quartz.listener;

import com.quartz.util.JobUtils;
import org.quartz.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class SoftDeleteJobListener implements JobListener {

    //    private final Scheduler scheduler;
   private DataSource dataSource;

    public SoftDeleteJobListener(DataSource dataSource) {
        System.out.println("SoftDeleteJobListener");
        this.dataSource = dataSource;
    }

    @Override
    public String getName() {
        return "SoftDeleteJobListener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        System.out.println("SoftDeleteJobListener.jobToBeExecuted");
        try {
            JobKey jobKey = context.getJobDetail().getKey();
            String jobName = jobKey.getName();
            String jobGroup = jobKey.getGroup();

            // Retrieve ISDELETED status from the database
            char isDeleted = getJobDeletionStatus(jobName, jobGroup);
            System.out.println("ISDELETED status: " + isDeleted);

            if (isDeleted == 'Y') {
                // Prevent execution if job is marked as deleted
                System.out.println("Job is soft deleted and cannot be executed.");
                return;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error checking softDeleted flag for job", e);
        }
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        // Optional: Handle vetoed jobs
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        // Optional: Handle after job execution
    }

    private char getJobDeletionStatus(String jobName, String jobGroup) throws SQLException {
        JobUtils jobUtils = new JobUtils(dataSource);
        return jobUtils.getJobDeletionStatus(jobName, jobGroup);
    }
}


