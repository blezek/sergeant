package edu.mayo.qia.qin.sergeant;

import java.util.concurrent.ExecutionException;

import org.zeroturnaround.exec.StartedProcess;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProcessJobInfo extends JobInfo {
  @JsonIgnore
  public StartedProcess startedProcess;

  @Override
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

  @Override
  public String getOutput() throws Exception {
    if (startedProcess.future().isDone()) {
      return startedProcess.future().get().outputUTF8();
    } else {
      return "";
    }
  }

  @Override
  public void shutdown() throws Exception {
    super.shutdown();
    if (!getStatus().equals("done")) {
      startedProcess.process().destroy();
    }
  }
}
