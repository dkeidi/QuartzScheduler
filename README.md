Project: QuartzScheduler
Description: This is a version of QuartzScheduler to schedule jobs using JDBC Jobstore and
LOG4J2 to provide information of job executions and errors.
By: Keidi Tay

Requirements:
- JDK 17
- Microsoft JDBC DRIVER 12.6
- 

1) Run application directly in terminal
    `./gradlew bootRun`

2) Run application as a jar
- `./gradlew clean build`

Run jar with default configuration
- `java -jar  QuartzScheduler-0.0.1-SNAPSHOT.jar`

Run jar with external configuration
- `java -jar  QuartzScheduler-0.0.1-SNAPSHOT.jar --spring.config.location=file:/file_path/application.properties`


