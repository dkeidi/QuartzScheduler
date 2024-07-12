package com.quartz.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class Log4j2PropertiesGenerator {
    public static void generateLog4j2Properties(Properties jobProperties, String outputFilePath) throws IOException {
        StringBuilder log4j2Content = new StringBuilder();

        // Append the static parts of the log4j2.properties
        log4j2Content.append("status = INFO\n");
        log4j2Content.append("name = RollingBuilder\n\n");

        // Logging Properties
        log4j2Content.append("property.LOG_PATTERN = %d{dd-MM-yyyy'T'HH:mm:ss.SSSZ} %p %m%n\n");
        log4j2Content.append("property.basePath = ./quartz_logs\n\n");

        // Console Appender
        log4j2Content.append("appender.console.type = Console\n");
        log4j2Content.append("appender.console.name = LogToConsole\n");
        log4j2Content.append("appender.console.target = SYSTEM_OUT\n");
        log4j2Content.append("appender.console.layout.type = PatternLayout\n");
        log4j2Content.append("appender.console.layout.pattern = %d{dd-MM-yyyy HH:mm:ss} %-5p %c{1}:%L - %m%n\n\n");

        // RollingFile Appender (general)
        log4j2Content.append("appender.rolling.type = RollingFile\n");
        log4j2Content.append("appender.rolling.name = LogToFile\n");
        log4j2Content.append("appender.rolling.fileName = ${basePath}/${date:dd-MM-yyyy}.log\n");
        log4j2Content.append("appender.rolling.filePattern = ${basePath}/%d{dd-MM-yyyy}.log.gz\n");
        log4j2Content.append("appender.rolling.layout.type = PatternLayout\n");
        log4j2Content.append("appender.rolling.layout.pattern = [%-5level] %d{dd-MM-yyyy HH:mm:ss.SSS} [%t] %c{1} - %msg%n\n");
        log4j2Content.append("appender.rolling.policies.type = Policies\n");
        log4j2Content.append("appender.rolling.policies.time.type = TimeBasedTriggeringPolicy\n");
        log4j2Content.append("appender.rolling.policies.time.interval = 2\n");
        log4j2Content.append("appender.rolling.policies.time.modulate = true\n");
        log4j2Content.append("appender.rolling.policies.size.type = SizeBasedTriggeringPolicy\n");
        log4j2Content.append("appender.rolling.policies.size.size = 10MB\n\n");

        // Add RollingFile Appenders for each job
        for (Map.Entry<Object, Object> entry : jobProperties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("job.") && key.endsWith(".command")) {
                String jobName = key.split("\\.")[1];

                log4j2Content.append("appender.rolling").append(jobName).append(".type = RollingFile\n");
                log4j2Content.append("appender.rolling").append(jobName).append(".name = Log").append(jobName).append("ToFile\n");
                log4j2Content.append("appender.rolling").append(jobName).append(".fileName = ${basePath}/").append(jobName).append("/${date:dd-MM-yyyy}.log\n");
                log4j2Content.append("appender.rolling").append(jobName).append(".filePattern = ${basePath}/").append(jobName).append("/%d{dd-MM-yyyy}.log.gz\n");
                log4j2Content.append("appender.rolling").append(jobName).append(".layout.type = PatternLayout\n");
                log4j2Content.append("appender.rolling").append(jobName).append(".layout.pattern = [%-5level] %d{dd-MM-yyyy HH:mm:ss.SSS} [%t] %c{1} - %msg%n\n");
                log4j2Content.append("appender.rolling").append(jobName).append(".policies.type = Policies\n");
                log4j2Content.append("appender.rolling").append(jobName).append(".policies.time.type = TimeBasedTriggeringPolicy\n");
                log4j2Content.append("appender.rolling").append(jobName).append(".policies.time.interval = 2\n");
                log4j2Content.append("appender.rolling").append(jobName).append(".policies.time.modulate = true\n");
                log4j2Content.append("appender.rolling").append(jobName).append(".policies.size.type = SizeBasedTriggeringPolicy\n");
                log4j2Content.append("appender.rolling").append(jobName).append(".policies.size.size = 10MB\n\n");
            }
        }

        // Add Loggers for each job
        log4j2Content.append("logger.com.quartz.level = debug\n");
        log4j2Content.append("logger.com.quartz.additivity = false\n");
        log4j2Content.append("logger.com.quartz.appenderRef.rolling.ref = LogToFile\n");
        log4j2Content.append("logger.com.quartz.appenderRef.console.ref = LogToConsole\n\n");

        for (Map.Entry<Object, Object> entry : jobProperties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("job.") && key.endsWith(".command")) {
                String jobName = key.split("\\.")[1];

                log4j2Content.append("logger.com.quartz.jobs.").append(jobName).append(".name = com.quartz.jobs.").append(jobName).append("\n");
                log4j2Content.append("logger.com.quartz.jobs.").append(jobName).append(".level = trace\n");
                log4j2Content.append("logger.com.quartz.jobs.").append(jobName).append(".additivity = false\n");
                log4j2Content.append("logger.com.quartz.jobs.").append(jobName).append(".appenderRef.rolling").append(jobName).append(".ref = Log").append(jobName).append("ToFile\n");
                log4j2Content.append("logger.com.quartz.jobs.").append(jobName).append(".appenderRef.console.ref = LogToConsole\n\n");
            }
        }

        // Root Logger
        log4j2Content.append("rootLogger.level = error\n");
        log4j2Content.append("rootLogger.appenderRef.rolling.ref = LogToFile\n");
        log4j2Content.append("rootLogger.appenderRef.console.ref = LogToConsole\n");

        // Write to log4j2.properties file
        FileWriter writer = new FileWriter(outputFilePath);
        writer.write(log4j2Content.toString());
        writer.close();
    }
}
