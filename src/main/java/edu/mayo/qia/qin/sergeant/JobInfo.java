package edu.mayo.qia.qin.sergeant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.zeroturnaround.exec.StartedProcess;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class JobInfo {
  @JsonIgnore
  public StartedProcess startedProcess;

  public String startTime = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date());
  public long startTimeInMillis = new Date().getTime();

  public String uuid;
  public String commandLine;
  public String endPoint;

  public String getStatus() {
    if (startedProcess.future().isDone()) {
      return "done";
    }
    if (startedProcess.future().isCancelled()) {
      return "canceled";
    }
    return "running";
  }

  public int getExitValue() throws InterruptedException, ExecutionException {
    if (startedProcess.future().isDone()) {
      return startedProcess.future().get().exitValue();
    }
    return -1;
  }

  public String getOutput() throws Exception {
    if (startedProcess.future().isDone()) {
      return startedProcess.future().get().outputUTF8();
    } else {
      return "";
    }
  }
}
