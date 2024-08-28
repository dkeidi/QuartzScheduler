package com.quartz.QuartzScheduler;

import com.quartz.QuartzScheduler.model.User;
import com.quartz.QuartzScheduler.repo.UserRepo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@SpringBootApplication
public class QuartzSchedulerApplication {

//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//    private static Logger LOG;
//
//    public static String getJarDir() {
//        try {
//            // Get the location of the JAR file
//            File jarFile = new File(QuartzSchedulerApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI());
//            // Get the parent directory of the JAR file
//            return jarFile.getParentFile().getAbsolutePath();
//        } catch (URISyntaxException e) {
//            throw new RuntimeException("Failed to determine the JAR file directory.", e);
//        }
//    }

    public static void main(String[] args) {
        // Specify the URL with integrated security
        String url = "jdbc:sqlserver://localhost:1433;databaseName=quartz_scheduler;encrypt=true;trustServerCertificate=true;integratedSecurity=true;";

        try {
            ApplicationContext context = SpringApplication.run(QuartzSchedulerApplication.class, args);

            User user1 = context.getBean(User.class);
            user1.setId(111);
            user1.setName("kei");
            user1.setGender("JAVA");

            UserRepo repo = context.getBean(UserRepo.class);
            repo.save(user1);

            System.out.println(repo.findAll());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void scheduleJobs() {
//        String sql = "select * from [user];";
//        List<User> users  = jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(User.class));
//
//        users.forEach(System.out :: println);
//
//
//        try (InputStream externalInput = Files.newInputStream(Paths.get("job.properties"))) {
//
//            Properties prop = new Properties();
//            prop.load(externalInput);
//
//            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
//            scheduler.start();
//
//            for (String jobName : prop.stringPropertyNames()) {
//                if (jobName.endsWith(".cron")) {
//                    String jobKey = jobName.substring(4, jobName.length() - 5); // Remove the ".cron" suffix
//                    String cronExp = prop.getProperty(jobName);
//                    String commandKey = "job." + jobKey + ".command";
//                    String commandValue = prop.getProperty(commandKey);
//
//                    LOG.info("Processing job: {}, cron: {}, command: {}", jobKey, cronExp, commandValue);
//
//                    scheduleJob(scheduler, jobKey, cronExp, commandValue);
//                }
//            }
//
//            LOG.info("Scheduled all jobs.");
//        } catch (IOException | SchedulerException ex) {
//            LOG.error("Error scheduling jobs", ex);
//        }
//    }
//
//    private void scheduleJob(Scheduler scheduler, String jobKey, String cronExp, String commandValue) throws SchedulerException {
//        JobDetail jobDetail = JobBuilder.newJob(BatchJob.class)
//                .withIdentity(jobKey)
//                .usingJobData("command", commandValue)
//                .usingJobData("folder", jobKey)
//                .build();
//
//        Trigger trigger = TriggerBuilder.newTrigger()
//                .withIdentity(jobKey + "_trigger")
//                .withSchedule(CronScheduleBuilder.cronSchedule(cronExp))
//                .build();
//
//        scheduler.scheduleJob(jobDetail, trigger);
//    }
//
//    private void query() {
//        String url = "jdbc:sqlserver://localhost:1433;databaseName=quartz_scheduler;encrypt=true;trustServerCertificate=true;integratedSecurity=true;";
//
//        // Specify the path to sqljdbc_auth.dll if necessary
//        System.setProperty("java.library.path", "C:\\Program Files\\Java\\jdk-17\\bin\\sqljdbc_auth.dll");
//
//        // Optionally, you might need to refresh the library path (only necessary in some environments)
//        System.setProperty("sun.boot.library.path", System.getProperty("java.library.path"));
//
//        String user="";
//        String password="";
//        try {
//            Connection conn = DriverManager.getConnection(url, user, password);
//            LOG.info("Connection successful");
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//    @Override
//    public void run(String... args) throws Exception {
//
//        String sql = "select * from [user];";
//        List<User> users  = jdbcTemplate.query(sql, BeanPropertyRowMapper.newInstance(User.class));
//
//        users.forEach(System.out :: println);
//    }
}
