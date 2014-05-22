package edu.mayo.qia.qin.sargent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;

public class Worker {
  public String endPoint;
  public String commandLine;
  public Map<String, String> defaults = new HashMap<String, String>();

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response get() {
    return Response.ok(this).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response post(final MultivaluedMap<String, String> formData) throws Exception {
    String uuid = UUID.randomUUID().toString();
    String commandLine = formCommandLine(formData);
    StartedProcess process = new ProcessExecutor().command(parse(commandLine)).readOutput(true).destroyOnExit().timeout(1, TimeUnit.HOURS).start();
    JobInfo job = new JobInfo();
    job.commandLine = commandLine;
    job.uuid = uuid;
    job.startedProcess = process;
    Sargent.jobs.put(uuid, job);
    return Response.ok(job).build();
  }

  private String formCommandLine(MultivaluedMap<String, String> formData) {
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

    return cl;
  }

  private static String[] parse(final String toProcess) {
    if (toProcess == null || toProcess.length() == 0) {
      // no command? no string
      return new String[0];
    }

    // parse with a simple finite state machine

    final int normal = 0;
    final int inQuote = 1;
    final int inDoubleQuote = 2;
    int state = normal;
    StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
    Vector v = new Vector();
    StringBuffer current = new StringBuffer();
    boolean lastTokenHasBeenQuoted = false;

    while (tok.hasMoreTokens()) {
      String nextTok = tok.nextToken();
      switch (state) {
      case inQuote:
        if ("\'".equals(nextTok)) {
          lastTokenHasBeenQuoted = true;
          state = normal;
        } else {
          current.append(nextTok);
        }
        break;
      case inDoubleQuote:
        if ("\"".equals(nextTok)) {
          lastTokenHasBeenQuoted = true;
          state = normal;
        } else {
          current.append(nextTok);
        }
        break;
      default:
        if ("\'".equals(nextTok)) {
          state = inQuote;
        } else if ("\"".equals(nextTok)) {
          state = inDoubleQuote;
        } else if (" ".equals(nextTok)) {
          if (lastTokenHasBeenQuoted || current.length() != 0) {
            v.addElement(current.toString());
            current = new StringBuffer();
          }
        } else {
          current.append(nextTok);
        }
        lastTokenHasBeenQuoted = false;
        break;
      }
    }

    if (lastTokenHasBeenQuoted || current.length() != 0) {
      v.addElement(current.toString());
    }

    if (state == inQuote || state == inDoubleQuote) {
      throw new IllegalArgumentException("Unbalanced quotes in " + toProcess);
    }

    String[] args = new String[v.size()];
    v.copyInto(args);
    return args;
  }
}
