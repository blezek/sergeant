package edu.mayo.qia.qin.sergeant;

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

import net.sourceforge.argparse4j.inf.Namespace;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sergeant extends Application<SergeantConfiguration> {

  public static ExecutorService executor;
  public static Map<String, JobInfo> jobs = new ConcurrentHashMap<String, JobInfo>();
  public static Namespace namespaceArguments;
  public static Logger logger = LoggerFactory.getLogger(Sergeant.class);
  public static String configFile;
  public static WorkerManager workerManager;
  public static Environment environment;

  @Override
  public void initialize(Bootstrap<SergeantConfiguration> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html"));
  }

  @Override
  public void run(SergeantConfiguration configuration, Environment environment) throws Exception {
    Sergeant.environment = environment;
    // Add our resources to
    // the REST API will hang off of /rest, giving the AssetsBundle access to
    // '/'
    environment.jersey().setUrlPattern("/rest/*");
    workerManager = new WorkerManager(configuration.services);
    environment.jersey().register(workerManager);
    executor = environment.lifecycle().executorService("Worker-").build();

    QuartzManager manager = new QuartzManager(StdSchedulerFactory.getDefaultScheduler());
    environment.lifecycle().manage(manager);

    Scheduler scheduler = manager.scheduler;

    // Trigger the job to run now, and then repeat every 60 seconds
    JobDetail job = newJob(JobCleanup.class).withIdentity("cleanup", "alpha").build();
    Trigger trigger = newTrigger().withIdentity("trigger1", "group1").startNow().withSchedule(simpleSchedule().withIntervalInSeconds(60).repeatForever()).build();

    // Tell quartz to schedule the job using our trigger
    scheduler.scheduleJob(job, trigger);

    // Reload the config file every 30 seconds
    trigger = newTrigger().withIdentity("loadConfig", "group1").startNow().withSchedule(simpleSchedule().withIntervalInSeconds(configuration.reloadTimeInSeconds).repeatForever()).build();
    job = newJob(WorkerManager.class).withIdentity("parse config", "alpha").build();
    scheduler.scheduleJob(job, trigger);
    environment.healthChecks().register("workerManager", workerManager);
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    // Capture the last argument...
    if (args.length > 0) {
      configFile = args[args.length - 1];
    }
    new Sergeant().run(args);
  }

}
