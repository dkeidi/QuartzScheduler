package com.quartz.jobs;

import com.quartz.info.TriggerInfo;
import com.quartz.util.StreamGobbler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Executors;

@Component
public class CopyJob implements Job {
    private static final Logger LOG = LogManager.getLogger(CopyJob.class);

        public void execute(JobExecutionContext context) {
            JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
            TriggerInfo info = (TriggerInfo) jobDataMap.get(CopyJob.class.getSimpleName());

            Process process = null;
            try {
                LOG.info("Starting job: copy_file.bat, frequency: {}", info.getCronExp());

                process = Runtime.getRuntime().exec("cmd /c batch_files\\copy_file.bat");

                StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), LOG::info);
                StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), LOG::error);

                Executors.newSingleThreadExecutor().submit(outputGobbler);
                Executors.newSingleThreadExecutor().submit(errorGobbler);

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    LOG.info("Copied file from source_folder to dest_folder complete");
                } else {
                    LOG.error("Error while executing the batch file. Exit code: {}", exitCode);
                }

            } catch (IOException e) {
                LOG.error("IO exception occurred while executing the batch file", e);
            } catch (InterruptedException e) {
                LOG.error("Interrupted exception occurred while executing the batch file", e);
                Thread.currentThread().interrupt();
            }
        }
    }
