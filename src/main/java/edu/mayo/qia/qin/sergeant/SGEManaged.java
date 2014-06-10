package edu.mayo.qia.qin.sergeant;

import io.dropwizard.lifecycle.Managed;

import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SGEManaged implements Managed {
  Logger logger = LoggerFactory.getLogger(SGEManaged.class);
  {
    try {

      session = SessionFactory.getFactory().getSession();
    } catch (UnsatisfiedLinkError e) {
      logger.error("DRMAA system is not available, please include it in java.library.path, i.e. -Djava.library.path=/path/to/sge/lib/linux-x64: ", e);
    }
  }

  public static Session session;

  public static boolean isAvailable() {
    return session != null;
  }

  @Override
  public void start() throws Exception {
    if (isAvailable()) {
      session.init("");
    }
  }

  @Override
  public void stop() throws Exception {
    if (isAvailable()) {
      session.exit();
    }
  }

}
