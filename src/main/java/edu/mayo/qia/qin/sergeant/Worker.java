package edu.mayo.qia.qin.sergeant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

public class Worker {
  public String endPoint;
  public List<String> commandLine;
  public Boolean synchronous = Boolean.FALSE;
  public String description = "";
  public Map<String, String> defaults = new HashMap<String, String>();
  public String curlCommand = null;

  void formCurl() {
    if (curlCommand != null) {
      return;
    }
    StringBuilder buffer = new StringBuilder("curl -POST ");
    for (String commandLine : this.commandLine) {
      Matcher match = Pattern.compile("@(\\w*)").matcher(commandLine);
      boolean matched = false;
      while (match.find()) {
        matched = true;
        String key = match.group(1);
        String value = defaults.containsKey(key) ? defaults.get(key) : "VALUE";
        buffer.append("-d '" + key + "=" + value + "' ");
      }
      if (!matched) {
        // Add a dummy variable to keep CURL happy
        buffer.append("-d 'dummy=keep CURL from using GET'");
      }
    }
    buffer.append("http://localhost/rest/service/" + endPoint);
    curlCommand = buffer.toString();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response get() {
    return Response.ok(this).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response post(final MultivaluedMap<String, String> formData) throws Exception {
    String uuid = UUID.randomUUID().toString();
    List<String> commandLine = formCommandLine(formData);

    StartedProcess process = new ProcessExecutor().command(commandLine).readOutput(true).destroyOnExit().timeout(1, TimeUnit.HOURS).start();
    if (synchronous) {
      return Response.ok(process.future().get().outputUTF8()).build();
    } else {
      JobInfo job = new JobInfo();
      StringBuilder buffer = new StringBuilder();
      String sep = "";
      for (String arg : commandLine) {
        buffer.append(sep).append(arg);
        sep = " ";
      }
      job.commandLine = buffer.toString();
      job.uuid = uuid;
      job.startedProcess = process;
      job.endPoint = endPoint;
      Sergeant.jobs.put(uuid, job);

      return Response.ok(job).build();
    }
  }

  private List<String> formCommandLine(MultivaluedMap<String, String> formData) {
    List<String> convertedCommandLine = new ArrayList<String>();
    for (String commandLine : this.commandLine) {
      Matcher match = Pattern.compile("@(\\w*)").matcher(commandLine);
      String cl = commandLine;
      while (match.find()) {
        String replacementText = match.group(0);
        String key = match.group(1);
        String value = defaults.get(key);
        if (formData.containsKey(key)) {
          value = formData.getFirst(key);
        }
        value = value == null ? "" : value;
        cl = cl.replaceAll(replacementText, value);
      }
      convertedCommandLine.add(cl);
    }
    return convertedCommandLine;
  }

}
