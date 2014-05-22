package edu.mayo.qia.qin.sargent;

import io.dropwizard.Application;
import io.dropwizard.Bundle;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.commons.dbcp.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;

public class Sargent extends Application<SargentConfiguration> {

  public static ExecutorService executor;
  public static BasicDataSource dataSource;
  public static JdbcTemplate template;
  public static Map<String, JobInfo> jobs = new ConcurrentHashMap<String, JobInfo>();

  @Override
  public void initialize(Bootstrap<SargentConfiguration> bootstrap) {
    bootstrap.addBundle(new AssetsBundle("/assets/", "/", "index.html"));
  }

  @Override
  public void run(SargentConfiguration configuration, Environment environment) throws Exception {
    // Add our resources to
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
  }

  public static void main(String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");
    new Sargent().run(args);
  }

}
