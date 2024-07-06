package com.quartz.jobs;

import com.quartz.info.TriggerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Component
public class CopyJob implements Job {
    private static final Logger LOG = LogManager.getLogger(CopyJob.class);


        public void execute(JobExecutionContext context) {
            JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
            TriggerInfo info = (TriggerInfo) jobDataMap.get(CopyJob.class.getSimpleName());

            Process process = null;
            try {
                LOG.info("Starting job: copy_file.bat, frequency: " + info.getCronExp());

                process = Runtime.getRuntime().exec("cmd /c C:\\Users\\keidi.tay.chuan\\Documents\\batch_files\\copy_file.bat");

                StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), LOG::info);
                StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), LOG::error);

                Executors.newSingleThreadExecutor().submit(outputGobbler);
                Executors.newSingleThreadExecutor().submit(errorGobbler);

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    LOG.info("Copied file from source_folder to dest_folder complete");
                } else {
                    LOG.error("Error while executing the batch file. Exit code: " + exitCode);
                }

            } catch (IOException e) {
                LOG.error("IO exception occurred while executing the batch file", e);
            } catch (InterruptedException e) {
                LOG.error("Interrupted exception occurred while executing the batch file", e);
                Thread.currentThread().interrupt();
            }
        }

        private static class StreamGobbler implements Runnable {
            private InputStream inputStream;
            private Consumer<String> consumer;

            public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
                this.inputStream = inputStream;
                this.consumer = consumer;
            }

            @Override
            public void run() {
                new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
            }
        }
    }

