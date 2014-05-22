package edu.mayo.qia.qin.sargent;

import java.util.HashMap;
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

@Path("/")
public class WorkerManager {

  private Map<String, Worker> workerMap = new ConcurrentHashMap<String, Worker>();

  public WorkerManager(List<Worker> workers) {
    for (Worker worker : workers) {
      workerMap.put(worker.endPoint, worker);
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
    return Response.ok(Sargent.jobs).build();
  }

  @GET
  @Path("/job/{uuid}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJobInfo(@PathParam("uuid") String uuid) {
    if (Sargent.jobs.containsKey(uuid)) {
      return Response.ok(Sargent.jobs.get(uuid)).build();
    } else {
      return Response.status(Status.NOT_FOUND).build();
    }
  }
}
