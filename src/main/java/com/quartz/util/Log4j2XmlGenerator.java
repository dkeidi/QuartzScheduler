package com.quartz.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class Log4j2XmlGenerator {

    public static void generateLog4j2Xml(Properties jobProperties, Properties appProperties, String filePath, boolean isJDBC) throws IOException {
        StringBuilder log4jXml = new StringBuilder();
        StringBuilder rootAppenderRefs = new StringBuilder();

        String serverName = appProperties.getProperty("app.serverNameInString");
        String dbName = appProperties.getProperty("app.dbName");

        if (isJDBC) {
            log4jXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                    .append("<Configuration status=\"debug\">\n")

                    // Logging Properties Section
                    .append("    <Properties>\n")
                    .append("        <Property name=\"LOG_PATTERN\">%d{dd-MM-yyyy'T'HH:mm:ss.SSSZ} %p %m%n</Property>\n")
                    .append("        <Property name=\"BASE_FOLDER\">quartz_logs</Property>\n")
                    .append("        <Property name=\"DB_CONNECTION_STRING\">");

            log4jXml.append("jdbc:sqlserver://");
            log4jXml.append(serverName);
            log4jXml.append(";databaseName=");
            log4jXml.append(dbName);
            log4jXml.append(";integratedSecurity=true;encrypt=true;trustServerCertificate=true;\n")
                    .append("        </Property>\n")
                    .append("        <Property name=\"DB_DRIVER_CLASS\">com.microsoft.sqlserver.jdbc.SQLServerDriver</Property>\n")
                    .append("    </Properties>\n")

                    // Appenders Section
                    .append("    <Appenders>\n")

                    // Console Appender
                    .append("        <Console name=\"LogToConsole\" target=\"SYSTEM_OUT\">\n")
                    .append("            <PatternLayout pattern=\"%d{dd-MM-yyyy HH:mm:ss} %-5p %c{1}:%L - %m%n\"/>\n")
                    .append("        </Console>\n")

                    // JDBC Appenders
                    .append("        <JDBC name=\"LogJobToDB\" tableName=\"LOG_SCHEDULER\">\n")
                    .append("            <DriverManager connectionString=\"${DB_CONNECTION_STRING}\" driverClassName=\"${DB_DRIVER_CLASS}\"/>\n")
                    .append("            <Column name=\"ID\" pattern=\"%X{jobId}\"/>\n")
                    .append("            <Column name=\"JOB_NAME\" pattern=\"%X{jobName}\"/>\n")
                    .append("            <Column name=\"DATETIME_LOG\" pattern=\"%X{logTime}\"/>\n")
                    .append("            <Column name=\"NODE_ID\" pattern=\"%X{instanceId}\"/>\n")
                    .append("            <Column name=\"STATUS\" pattern=\"%X{status}\"/>\n")
                    .append("            <Column name=\"DATETIME_CREATED\" isEventTimestamp=\"true\"/>\n")
                    .append("        </JDBC>\n")

                    .append("        <JDBC name=\"LogFileToDB\" tableName=\"LOG_FILE\">\n")
                    .append("            <DriverManager connectionString=\"${DB_CONNECTION_STRING}\" driverClassName=\"${DB_DRIVER_CLASS}\"/>\n")
                    .append("            <Column name=\"ID\" pattern=\"%u\"/>\n")
                    .append("            <Column name=\"SCHEDULER_ID\" pattern=\"%X{jobId}\" />\n")
                    .append("            <Column name=\"DATETIME_LOG\" pattern=\"%X{logTime}\"/>\n")
                    .append("            <Column name=\"FILENAME\" pattern=\"%msg\"/>\n")
                    .append("            <Column name=\"FINAL_FILENAME\" pattern=\"%msg\"/>\n")
                    .append("            <Column name=\"DATETIME_CREATED\" isEventTimestamp=\"true\"/>\n")
                    .append("            <Filters>\n")
                    .append("                <RegexFilter regex=\"(?i).*\\.((txt|log|csv|json|xml|html?|pdf|docx?|xlsx?|pptx?|jpe?g|png|bmp|tiff|zip|tar|gz|7z|rar|sql|sh|bat))\\b.*\" onMatch=\"ACCEPT\" onMismatch=\"DENY\"/>\n")
                    .append("            </Filters>\n")
                    .append("        </JDBC>\n")

                    .append("        <JDBC name=\"LogDetailToDB\" tableName=\"LOG_DETAIL\">\n")
                    .append("            <DriverManager connectionString=\"${DB_CONNECTION_STRING}\" driverClassName=\"${DB_DRIVER_CLASS}\"/>\n")
                    .append("            <Column name=\"ID\" pattern=\"%u\"/>\n")
                    .append("            <Column name=\"SCHEDULER_ID\" pattern=\"%X{jobId}\" />\n")
                    .append("            <Column name=\"LOG_LEVEL\" pattern=\"%level\"/>\n")
                    .append("            <Column name=\"DESCRIPTION\" pattern=\"%msg\"/>\n")
                    .append("            <Column name=\"LOG_FILENAME\" pattern=\"%X{logFileName}\"/>\n")
                    .append("            <Column name=\"DATETIME_CREATED\" isEventTimestamp=\"true\"/>\n")
                    .append("            <Filters>\n")
                    .append("                <ThresholdFilter level=\"ERROR\" onMatch=\"ACCEPT\" onMismatch=\"DENY\"/>\n")
                    .append("            </Filters>\n")
                    .append("        </JDBC>\n");


            // RollingFile Appender for General Logs
            log4jXml.append("        <RollingFile name=\"LogToFile\" ")
                    .append("fileName=\"${BASE_FOLDER}/${date:dd-MM-yyyy}.log\" ")
                    .append("filePattern=\"${BASE_FOLDER}/%d{dd-MM-yyyy}.log.gz\">\n")
                    .append("            <PatternLayout>\n")
                    .append("                <pattern>[%-5level] %d{dd-MM-yyyy HH:mm:ss.SSS} [%t] %c{1} - %msg%n</pattern>\n")
                    .append("            </PatternLayout>\n")
                    .append("            <Policies>\n")
                    .append("                <TimeBasedTriggeringPolicy interval=\"2\" modulate=\"true\" />\n")
                    .append("                <SizeBasedTriggeringPolicy size=\"10MB\" />\n")
                    .append("            </Policies>\n")
                    .append("        </RollingFile>\n");

            // RollingFile Appenders for each job
            for (Map.Entry<Object, Object> entry : jobProperties.entrySet()) {
                String key = (String) entry.getKey();
                if (key.startsWith("job.") && key.endsWith(".cron")) {
                    String jobName = key.split("\\.")[1];
                    log4jXml.append("        <RollingFile name=\"Log").append(jobName).append("ToFile\" ")
                            .append("fileName=\"${BASE_FOLDER}/").append(jobName).append("/${date:dd-MM-yyyy}.log\" ")
                            .append("filePattern=\"${BASE_FOLDER}/").append(jobName).append("/%d{dd-MM-yyyy}.log.gz\">\n")
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

            // Closing Appenders and Adding Loggers
            log4jXml.append("    </Appenders>\n")
                    .append("    <Loggers>\n");

            for (Map.Entry<Object, Object> entry : jobProperties.entrySet()) {
                String key = (String) entry.getKey();
                if (key.startsWith("job.") && key.endsWith(".cron")) {
                    String jobName = key.split("\\.")[1];
                    log4jXml.append("        <Logger name=\"com.quartz.jobs.").append(jobName).append("\" level=\"info\" additivity=\"false\">\n")
                            .append("            <AppenderRef ref=\"Log").append(jobName).append("ToFile\"/>\n")
                            .append("            <AppenderRef ref=\"LogToConsole\"/>\n")
                            .append("            <AppenderRef ref=\"LogJobToDB\"/>\n")
                            .append("            <AppenderRef ref=\"LogFileToDB\"/>\n")
                            .append("            <AppenderRef ref=\"LogDetailToDB\"/>\n")
                            .append("        </Logger>\n");

                    // Add each job's appender to the Root tag collection
                    rootAppenderRefs.append("            <AppenderRef ref=\"Log").append(jobName).append("ToFile\"/>\n");
                }
            }

            log4jXml.append("        <Root level=\"info\">\n")
                    .append("            <AppenderRef ref=\"LogToFile\"/>\n")
                    .append(rootAppenderRefs)
                    .append("            <AppenderRef ref=\"LogToConsole\"/>\n")
                    .append("        </Root>\n")
                    .append("    </Loggers>\n")
                    .append("</Configuration>\n");

        } else { //non-JDBC
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

        }


        // Write the configuration to a file
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(log4jXml.toString());
        }
    }
}
