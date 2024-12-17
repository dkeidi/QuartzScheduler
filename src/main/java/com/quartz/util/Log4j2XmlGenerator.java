package com.quartz.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Log4j2XmlGenerator {

    public static void generateLog4j2Xml(Properties jobProperties, Properties appProperties, String filePath, boolean isJDBC) throws IOException {
        System.out.println("generateLog4j2Xml");
        StringBuilder log4jXml = new StringBuilder();

        String serverName = appProperties.getProperty("app.serverNameInString");
        String dbName = appProperties.getProperty("app.dbName");
        String log4j2Level = appProperties.getProperty("app.log4j2Level");
        String jobLogLevel = appProperties.getProperty("app.job.logLevel");
        String rootLogLevel = appProperties.getProperty("app.root.logLevel");
        String logFolder = appProperties.getProperty("app.logFolder");
        String jobPrefix = appProperties.getProperty("app.jobname.prefix");

        String logPattern = "%d{dd-MM-yyyy'T'HH:mm:ss.SSSZ} %p %m%n";
        String consolePattern = "%d{dd-MM-yyyy HH:mm:ss} %-5p %c{1}:%L - %m%n";
        String rollingFilePattern = "[%-5level] %d{dd-MM-yyyy HH:mm:ss.SSS} [%t] %c{1} - %msg%n";

        log4jXml.append(generateXmlHeader(log4j2Level));
        log4jXml.append(generateProperties(logPattern, logFolder, serverName, dbName, isJDBC, appProperties));

        if (isJDBC) {
            log4jXml.append(generateJDBCJobAppender(consolePattern, jobProperties, jobLogLevel, rootLogLevel, jobPrefix, rollingFilePattern));
        } else {
            log4jXml.append(generateRAMJobAppender(consolePattern, jobProperties, jobLogLevel, rootLogLevel, jobPrefix, rollingFilePattern));
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(log4jXml.toString());
        }

        System.out.println("end generateLog4j2Xml");

    }

    private static String generateXmlHeader(String log4j2Level) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Configuration status=\"" + log4j2Level + "\">\n";
    }

    private static String generateProperties(String logPattern, String logFolder, String serverName, String dbName, boolean isJDBC, Properties appProperties) {
        StringBuilder propertiesSection = new StringBuilder();

        propertiesSection.append("    <Properties>\n")
                .append("        <Property name=\"LOG_PATTERN\">").append(logPattern).append("</Property>\n")
                .append("        <Property name=\"BASE_FOLDER\">").append(logFolder).append("</Property>\n");

        if (isJDBC) {
            String connectionParam = "integratedSecurity=true;encrypt=true;trustServerCertificate=true;";
            String jdbcDriver = appProperties.getProperty("spring.datasource.driver-class-name");

            propertiesSection.append("        <Property name=\"DB_CONNECTION_STRING\">jdbc:sqlserver://")
                    .append(serverName).append(";databaseName=").append(dbName).append(";").append(connectionParam).append("</Property>\n")
                    .append("        <Property name=\"DB_DRIVER_CLASS\">").append(jdbcDriver).append("</Property>\n");
        }

        propertiesSection.append("    </Properties>\n");
        return propertiesSection.toString();
    }

    private static String generateJDBCJobAppender(String consolePattern, Properties jobProperties, String jobLogLevel, String rootLogLevel, String jobPrefix, String rollingFilePattern) {
        StringBuilder jdbcSection = new StringBuilder();

        // Appenders Section
        jdbcSection.append("    <Appenders>\n")
                .append(generateConsoleAppender(consolePattern))

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


        jdbcSection.append(generateRollingFileAppender(jobProperties, rollingFilePattern));

        jdbcSection.append("    </Appenders>\n")
                .append("    <Loggers>\n");

        for (Map.Entry<Object, Object> entry : jobProperties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("job.") && key.endsWith(".cron")) {
                String jobName = key.split("\\.")[1];
                appendLoggerBlock(jdbcSection, jobName, jobLogLevel, List.of(
                                "Log" + jobName + "ToFile",
                                "LogToConsole",
                                "LogJobToDB",
                                "LogFileToDB",
                                "LogDetailToDB"
                        ),
                        jobPrefix);
            }
        }
        appendRootSection(jdbcSection, rootLogLevel);
        return jdbcSection.toString();
    }

    private static String generateRAMJobAppender(String consolePattern, Properties jobProperties, String jobLogLevel, String rootLogLevel, String jobPrefix, String rollingFilePattern) {
        StringBuilder ramSection = new StringBuilder();

        ramSection.append("    <Appenders>\n").append(generateConsoleAppender(consolePattern));
        ramSection.append(generateRollingFileAppender(jobProperties, rollingFilePattern));

        ramSection.append("    </Appenders>\n")
                .append("    <Loggers>\n");

        ramSection.append("        <Logger name=\"main").append("\" level=\"").append(jobLogLevel).append("\" additivity=\"false\">\n")
                .append("            <AppenderRef ref=\"LogToFile\"/>\n")
                .append("            <AppenderRef ref=\"LogToConsole\"/>\n")
                .append("        </Logger>\n");


        for (Map.Entry<Object, Object> entry : jobProperties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("job.") && key.endsWith(".cron")) {
                String jobName = key.split("\\.")[1];
                appendLoggerBlock(ramSection, jobName, jobLogLevel, List.of(
                        "Log" + jobName + "ToFile",
                        "LogToConsole"
                ), jobPrefix);
            }
        }
        appendRootSection(ramSection, rootLogLevel);
        return ramSection.toString();
    }

    private static String generateConsoleAppender(String consolePattern) {
        return "        <Console name=\"LogToConsole\" target=\"SYSTEM_OUT\">\n" +
                "            <PatternLayout pattern=\"" + consolePattern + "\"/>\n" +
                "        </Console>\n";
    }

    private static String generateRollingFileAppender(Properties jobProperties, String rollingFilePattern) {
        StringBuilder rollingFileSection = new StringBuilder();

        rollingFileSection.append("        <RollingFile name=\"LogToFile\" ")
                .append("fileName=\"${BASE_FOLDER}/${date:dd-MM-yyyy}.log\" ")
                .append("filePattern=\"${BASE_FOLDER}/%d{dd-MM-yyyy}.log.gz\">\n")
                .append("            <PatternLayout>\n")
                .append("                <pattern>").append(rollingFilePattern).append("</pattern>\n")
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
                rollingFileSection.append("        <RollingFile name=\"Log").append(jobName).append("ToFile\" ")
                        .append("fileName=\"${BASE_FOLDER}/").append(jobName).append("/${date:dd-MM-yyyy}.log\" ")
                        .append("filePattern=\"${BASE_FOLDER}/").append(jobName).append("/%d{dd-MM-yyyy}.log.gz\">\n")
                        .append("            <PatternLayout>\n")
                        .append("                <pattern>").append(rollingFilePattern).append("</pattern>\n")
                        .append("            </PatternLayout>\n")
                        .append("            <Policies>\n")
                        .append("                <TimeBasedTriggeringPolicy interval=\"2\" modulate=\"true\" />\n")
                        .append("                <SizeBasedTriggeringPolicy size=\"10MB\" />\n")
                        .append("            </Policies>\n")
                        .append("        </RollingFile>\n");
            }
        }

        return rollingFileSection.toString();
    }

    private static void appendLoggerBlock(StringBuilder section, String jobName, String level, List<String> appenderRefs, String jobPrefix) {
        section.append("        <Logger name=\"").append(jobPrefix).append(".").append(jobName).append("\" level=\"").append(level).append("\" additivity=\"false\">\n");
        for (String appenderRef : appenderRefs) {
            section.append("            <AppenderRef ref=\"").append(appenderRef).append("\"/>\n");
        }
        section.append("        </Logger>\n");
    }

    private static void appendRootSection(StringBuilder section, String level) {
        section.append("        <Root level=\"").append(level).append("\">\n")
                .append("            <AppenderRef ref=\"LogToFile\"/>\n")
                .append("            <AppenderRef ref=\"LogToConsole\"/>\n")
                .append("        </Root>\n")
                .append("    </Loggers>\n")
                .append("</Configuration>\n");
    }
}
