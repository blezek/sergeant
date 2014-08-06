package edu.mayo.qia.qin.sergeant;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

public abstract class JobInfo {

  public String startTime = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date());
  public long startTimeInMillis = new Date().getTime();

  public String uuid = UUID.randomUUID().toString();
  public String commandLine;
  public String endPoint;
  public Map<String, File> fileMap = new HashMap<String, File>();
  public List<String> parsedCommandLine;

  public abstract String getStatus();

  public abstract String getOutput() throws Exception;

  public void shutdown() throws Exception {
    FileUtils.deleteDirectory(new File(Sergeant.configuration.storageDirectory, uuid));
  };
}
