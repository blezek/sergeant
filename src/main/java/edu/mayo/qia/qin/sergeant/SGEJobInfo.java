package edu.mayo.qia.qin.sergeant;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.Session;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SGEJobInfo extends JobInfo {

  public String jobID;
  @JsonIgnore
  public File logPath;
  @JsonIgnore
  String contents = "";
  @JsonIgnore
  long lastChecked = 0;

  static Map<Integer, String> statusMap;
  {
    statusMap = new ConcurrentHashMap<Integer, String>();
    statusMap.put(Session.DONE, "done");
    statusMap.put(Session.FAILED, "failed");
    statusMap.put(Session.HOLD, "hold");
    statusMap.put(Session.QUEUED_ACTIVE, "running");
    statusMap.put(Session.RUNNING, "running");
    statusMap.put(Session.SYSTEM_ON_HOLD, "hold");
    statusMap.put(Session.SYSTEM_SUSPENDED, "suspended");
    statusMap.put(Session.UNDETERMINED, "undetermined");
    statusMap.put(Session.USER_ON_HOLD, "hold");
    statusMap.put(Session.USER_SYSTEM_ON_HOLD, "hold");
    statusMap.put(Session.USER_SYSTEM_SUSPENDED, "suspended");
  }

  @Override
  public String getStatus() {
    int status = -1000;
    try {
      status = SGEManaged.session.getJobProgramStatus(jobID);
    } catch (DrmaaException e) {
      // silent fail
    }
    return statusMap.containsKey(status) ? statusMap.get(status) : "unknown";

  }

  @Override
  public String getOutput() throws Exception {
    if (lastChecked < logPath.lastModified()) {
      contents = new String(Files.readAllBytes(logPath.toPath()));
    }
    return contents;
  }

  @Override
  public void shutdown() throws Exception {
    SGEManaged.session.control(jobID, Session.TERMINATE);
  }

}
