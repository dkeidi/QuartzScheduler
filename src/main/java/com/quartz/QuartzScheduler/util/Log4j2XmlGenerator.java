package com.quartz.QuartzScheduler.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class Log4j2XmlGenerator {
    public static void generateLog4j2Xml(Properties jobProperties, String filePath) throws IOException {
        StringBuilder log4jXml = new StringBuilder();
        log4jXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<Configuration status=\"DEBUG\">\n")
                .append("    <Properties>\n")
                .append("        <Property name=\"LOG_PATTERN\">%d{dd-MM-yyyy'T'HH:mm:ss.SSSZ} %p %m%n</Property>\n")
                .append("        <Property name=\"basePath\">quartz_logs</Property>\n")
                .append("        <Property name=\"log4j.Clock\">org.apache.logging.log4j.core.util.SystemMillisClock</Property>\n")
                .append("    </Properties>\n")
                .append("    <Appenders>\n")
                .append("        <Console name=\"LogToConsole\" target=\"SYSTEM_OUT\">\n")
                .append("            <PatternLayout pattern=\"%d{dd-MM-yyyy HH:mm:ss} %-5p %c{1}:%L - %m%n\" />\n")
                .append("        </Console>\n");

        log4jXml.append("        <RollingFile name=\"LogToFile\" ")
                .append("fileName=\"${basePath}/${date:dd-MM-yyyy}.log\" ")
                .append("filePattern=\"${basePath}/%d{dd-MM-yyyy}.log.gz\">\n")
                .append("            <PatternLayout>\n")
                .append("                <pattern>[%-5level] %d{dd-MM-yyyy HH:mm:ss.SSS} [%t] %c{1} - %msg%n</pattern>\n")
                .append("            </PatternLayout>\n")
                .append("            <Policies>\n")
                .append("                <TimeBasedTriggeringPolicy interval=\"2\" modulate=\"true\" />\n")
                .append("                <SizeBasedTriggeringPolicy size=\"10MB\" />\n")
                .append("            </Policies>\n")
                .append("        </RollingFile>\n");


        for (Map.Entry<Object, Object> entry : jobProperties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("job.") && key.endsWith(".cron")) {
                String jobName = key.split("\\.")[1];
                log4jXml.append("        <RollingFile name=\"Log").append(jobName).append("ToFile\" ")
                        .append("fileName=\"${basePath}/").append(jobName).append("/${date:dd-MM-yyyy}.log\" ")
                        .append("filePattern=\"${basePath}/").append(jobName).append("/%d{dd-MM-yyyy}.log.gz\">\n")
                        .append("            <PatternLayout>\n")
                        .append("                <pattern>[%-5level] %d{dd-MM-yyyy HH:mm:ss.SSS} [%t] %c{1} - %msg%n</pattern>\n")
                        .append("            </PatternLayout>\n")
                        .append("            <Policies>\n")
                        .append("                <TimeBasedTriggeringPolicy interval=\"2\" modulate=\"true\" />\n")
                        .append("                <SizeBasedTriggeringPolicy size=\"10MB\" />\n")
                        .append("            </Policies>\n")
                        .append("        </RollingFile>\n");
            }
        }

        log4jXml.append("    </Appenders>\n")
                .append("    <Loggers>\n");

        log4jXml.append("        <Logger name=\"main").append("\" level=\"debug\" additivity=\"false\">\n")
                .append("            <AppenderRef ref=\"LogToFile\"/>\n")
                .append("            <AppenderRef ref=\"LogToConsole\"/>\n")
                .append("        </Logger>\n");


        for (Map.Entry<Object, Object> entry : jobProperties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("job.") && key.endsWith(".cron")) {
                String jobName = key.split("\\.")[1];
                log4jXml.append("        <Logger name=\"com.quartz.jobs.").append(jobName).append("\" level=\"debug\" additivity=\"false\">\n")
                        .append("            <AppenderRef ref=\"Log").append(jobName).append("ToFile\"/>\n")
                        .append("            <AppenderRef ref=\"LogToConsole\"/>\n")
                        .append("        </Logger>\n");
            }
        }

        log4jXml.append("        <Root level=\"debug\">\n")
                .append("            <AppenderRef ref=\"LogToFile\"/>\n")
                .append("            <AppenderRef ref=\"LogToConsole\"/>\n")
                .append("        </Root>\n")
                .append("    </Loggers>\n")
                .append("</Configuration>\n");

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(log4jXml.toString());
        }
    }
}
