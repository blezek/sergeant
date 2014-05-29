package edu.mayo.qia.qin.sargent;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class JobCleanup implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    for (JobInfo job : Sargent.jobs.values()) {
      long diff = new Date().getTime() - job.startTime.getTime();

      if (job.getStatus().equals("done") && TimeUnit.MILLISECONDS.toMinutes(diff) > 4) {
        Sargent.jobs.remove(job.uuid);
      }
    }
  }
}
