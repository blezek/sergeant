package edu.mayo.qia.qin.sergeant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public abstract class JobInfo {

  public String startTime = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date());
  public long startTimeInMillis = new Date().getTime();

  public String uuid = UUID.randomUUID().toString();
  public String commandLine;
  public String endPoint;

  public abstract String getStatus();

  public abstract String getOutput() throws Exception;

  public abstract void shutdown() throws Exception;
}
