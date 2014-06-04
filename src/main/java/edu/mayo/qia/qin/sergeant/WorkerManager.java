package edu.mayo.qia.qin.sergeant;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Path("/")
public class WorkerManager extends HealthCheck implements Job {

  private Map<String, Worker> workerMap = new ConcurrentHashMap<String, Worker>();

  public WorkerManager() {
  }

  public WorkerManager(List<Worker> workers) {
    updateWorkers(workers);
  }

  public void updateWorkers(List<Worker> workers) {
    List<String> workerNames = new ArrayList<String>();
    for (Worker worker : workers) {
      workerNames.add(worker.endPoint);
      workerMap.put(worker.endPoint, worker);
    }
    for (String key : workerMap.keySet()) {
      if (!workerNames.contains(key)) {
        workerMap.remove(key);
      }
    }
  }

  @GET
  @Path("/service")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getServiceInfo() {
    return Response.ok(workerMap).build();
  }

  @Path("/service/{endpoint}")
  public Worker get(@PathParam("endpoint") String endPoint) {
    return workerMap.get(endPoint);
  }

  @GET
  @Path("/job")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJob() {
    return Response.ok(Sergeant.jobs.values()).build();
  }

  @GET
  @Path("/job/{uuid}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJobInfo(@PathParam("uuid") String uuid) {
    if (Sergeant.jobs.containsKey(uuid)) {
      return Response.ok(Sergeant.jobs.get(uuid)).build();
    } else {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    // Parse the config file
    try (InputStream input = new FileInputStream(Sergeant.configFile)) {
      ObjectMapper mapper = Sergeant.environment.getObjectMapper();

      final JsonNode node = mapper.readTree(new YAMLFactory().createParser(input));
      final SergeantConfiguration config = mapper.readValue(new TreeTraversingParser(node), SergeantConfiguration.class);
      if (config != null) {
        Sergeant.workerManager.updateWorkers(config.services);
      }
    } catch (Exception e) {
      Sergeant.logger.error("Error parsing config file", e);
    }
  }

  @Override
  protected Result check() throws Exception {
    if (workerMap.isEmpty()) {
      return Result.unhealthy("No workers defined");
    } else {
      return Result.healthy();
    }
  }
}
