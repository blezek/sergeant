package edu.mayo.qia.qin.sargent;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.commons.dbcp.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class Sargent extends Application<SargentConfiguration> {

  public static ExecutorService executor;
  public static BasicDataSource dataSource;
  public static JdbcTemplate template;
  public static Map<String, JobInfo> jobs = new ConcurrentHashMap<String, JobInfo>();

  @Override
  public void initialize(Bootstrap<SargentConfiguration> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
  }

  @Override
  public void run(SargentConfiguration configuration, Environment environment) throws Exception {
    // Add our resources to
    // the REST API will hang off of /rest, giving the AssetsBundle access to
    // '/'
    environment.jersey().setUrlPattern("/rest/*");

    environment.jersey().register(new WorkerManager(configuration.services));
    executor = environment.lifecycle().executorService("Worker-").build();
    dataSource = new BasicDataSource();
    dataSource.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
    dataSource.setUrl("jdbc:derby:memory:testDB;create=true");
    dataSource.setDefaultAutoCommit(true);
    template = new JdbcTemplate(dataSource);
    Flyway flyway = new Flyway();
    flyway.setDataSource(dataSource);
    flyway.migrate();

    QuartzManager manager = new QuartzManager(StdSchedulerFactory.getDefaultScheduler());
    environment.lifecycle().manage(manager);

    Scheduler scheduler = manager.scheduler;

    // define the job and tie it to our HelloJob class
    JobDetail job = newJob(JobCleanup.class).withIdentity("cleanup", "alpha").build();

    // Trigger the job to run now, and then repeat every 40 seconds
    Trigger trigger = newTrigger().withIdentity("trigger1", "group1").startNow().withSchedule(simpleSchedule().withIntervalInSeconds(60).repeatForever()).build();

    // Tell quartz to schedule the job using our trigger
    scheduler.scheduleJob(job, trigger);

  }

  public static void main(String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    new Sargent().run(args);
  }

}
