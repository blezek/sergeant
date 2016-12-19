package edu.mayo.qia.qin.sergeant;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.ggf.drmaa.JobTemplate;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Worker {
  static Logger logger = LoggerFactory.getLogger(Worker.class);
  public String endPoint;
  public List<String> commandLine;
  public Boolean synchronous = Boolean.FALSE;
  public Boolean gridSubmit = Boolean.FALSE;
  public String description = "";
  public Map<String, String> defaults = new HashMap<String, String>();
  public String curlCommand = null;
  public String gridSpecification = "-V";

  void formCurl() {
    if (curlCommand != null) {
      return;
    }
    StringBuilder buffer = new StringBuilder("curl -POST ");
    boolean matched = false;
    for (String commandLine : this.commandLine) {
      Matcher match = Pattern.compile("@(\\w*)").matcher(commandLine);
      while (match.find()) {
        matched = true;
        String key = match.group(1);
        String value = defaults.containsKey(key) ? defaults.get(key) : "VALUE";
        buffer.append("-d '" + key + "=" + value + "' ");
      }
    }
    if (!matched) {
      // Add a dummy variable to keep CURL happy
      buffer.append("-d 'dummy=keep CURL from using GET' ");
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
  public Response postMVM(MultivaluedMap<String, String> formData) throws Exception {
    FormDataMultiPart multiPart = new FormDataMultiPart();
    for (String key : formData.keySet()) {
      for (String v : formData.get(key)) {
        multiPart.field(key, v);
      }
    }
    return post(multiPart);
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response post(final FormDataMultiPart multiPart) throws Exception {

    if (gridSubmit) {
      if (!SGEManaged.isAvailable()) {
        return Response.serverError().entity("SGE is not available").build();
      }
      new File("logs").mkdir();
      SGEJobInfo job = new SGEJobInfo();
      formCommandLine(multiPart, job);

      job.logPath = new File("logs", job.uuid + ".out");
      JobTemplate template = SGEManaged.session.createJobTemplate();
      template.setWorkingDirectory(System.getProperty("user.dir"));
      template.setRemoteCommand(job.parsedCommandLine.get(0));
      template.setArgs(job.parsedCommandLine.subList(1, job.parsedCommandLine.size()));
      template.setJoinFiles(true);
      template.setOutputPath(":" + job.logPath.getAbsolutePath());
      template.setNativeSpecification(gridSpecification);
      job.jobID = SGEManaged.session.runJob(template);
      SGEManaged.session.deleteJobTemplate(template);
      Sergeant.jobs.put(job.uuid, job);
      return Response.ok(job).build();
    } else {
      ProcessJobInfo job = new ProcessJobInfo();
      formCommandLine(multiPart, job);

      StartedProcess process = new ProcessExecutor().command(job.parsedCommandLine).readOutput(true).destroyOnExit().timeout(1, TimeUnit.HOURS).start();
      if (synchronous) {
        return Response.ok(process.future().get().outputUTF8()).build();
      } else {
        job.startedProcess = process;
        job.endPoint = endPoint;
        Sergeant.jobs.put(job.uuid, job);
        return Response.ok(job).build();
      }
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
    StringBuilder buffer = new StringBuilder();
    String sep = "";
    for (String arg : convertedCommandLine) {
      buffer.append(sep).append(arg);
      sep = " ";
    }
    return convertedCommandLine;
  }

  private void formCommandLine(FormDataMultiPart multiPart, JobInfo job) throws Exception {
    List<String> convertedCommandLine = new ArrayList<String>();
    for (String originalArg : this.commandLine) {
      String arg = originalArg;
      Matcher match;
      match = Pattern.compile("@(\\w*)").matcher(originalArg);
      while (match.find()) {
        String replacementText = match.group(0);
        String key = match.group(1);
        String value = defaults.get(key);
        if (multiPart.getFields().containsKey(key)) {
          value = multiPart.getFields().get(key).get(0).getValue();
        }
        value = value == null ? "" : value;
        arg = arg.replaceAll(replacementText, value);
      }

      // Find input values
      match = Pattern.compile("<(\\w*)").matcher(originalArg);
      while (match.find()) {
        String replacementText = match.group(0);
        String key = match.group(1);
        String value = null;
        if (multiPart.getFields().containsKey(key)) {
          // Write to a temp file
          InputStream is = multiPart.getField(key).getEntityAs(InputStream.class);
          File dir = new File(Sergeant.configuration.storageDirectory, job.uuid);
          value = multiPart.getField(key).getName();
          File out = new File(dir, new File(value).getName());
          value = out.toString();
          dir.mkdirs();
          try (OutputStream output = new FileOutputStream(out)) {
            IOUtils.copy(is, output);
          }
          job.fileMap.put(key, out);
        }
        value = value == null ? "" : value;
        arg = arg.replaceAll(replacementText, value);
      }

      // Find output values
      match = Pattern.compile(">(\\w*)").matcher(originalArg);
      while (match.find()) {
        String replacementText = match.group(0);
        String key = match.group(1);
        String value = null;
        if (multiPart.getFields().containsKey(key)) {
          // Write to a temp file
          File dir = new File(Sergeant.configuration.storageDirectory, job.uuid);
          value = multiPart.getField(key).getValue();
          File out = new File(dir, new File(value).getName());
          value = out.toString();
          dir.mkdirs();
          job.fileMap.put(key, out);
        }
        value = value == null ? "" : value;
        arg = arg.replaceAll(replacementText, value);
      }

      convertedCommandLine.add(arg);
    }
    StringBuilder buffer = new StringBuilder();
    String sep = "";
    for (String arg : convertedCommandLine) {
      buffer.append(sep).append(arg);
      sep = " ";
    }
    job.parsedCommandLine = convertedCommandLine;
    job.commandLine = buffer.toString();
  }

}
