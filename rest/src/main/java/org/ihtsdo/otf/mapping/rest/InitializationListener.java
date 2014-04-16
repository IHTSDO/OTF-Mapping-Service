package org.ihtsdo.otf.mapping.rest;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapProjectList;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.MappingService;

import com.sun.jersey.api.model.AbstractResourceModelContext;
import com.sun.jersey.api.model.AbstractResourceModelListener;

/**
 * The listener interface for receiving initialization events. The class that is
 * interested in processing a initialization event implements this interface,
 * and the object created with that class is registered with a component using
 * the component's <code>addInitializationListener<code> method. When
 * the initialization event occurs, that object's appropriate
 * method is invoked.
 */
@Provider
public class InitializationListener implements AbstractResourceModelListener {

  /** The timer. */
  Timer timer;

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.jersey.api.model.AbstractResourceModelListener#onLoaded(com.sun
   * .jersey.api.model.AbstractResourceModelContext)
   */
  @Override
  public void onLoaded(AbstractResourceModelContext modelContext) {
    Logger.getLogger(this.getClass()).info(
        "Computing list of conflict records.");

    TimerTask task = new ComputeCompareFinishedRecordsTask();
    timer = new Timer();

    Calendar today = Calendar.getInstance();
    today.set(Calendar.HOUR_OF_DAY, 2);
    today.set(Calendar.MINUTE, 0);
    today.set(Calendar.SECOND, 0);

    timer.scheduleAtFixedRate(task, today.getTime(), 24 * 60 * 60 * 1000);
  }

  /**
   * The Class ComputeCompareFinishedRecordsTask.
   * 
   * @author ${author}
   */
  class ComputeCompareFinishedRecordsTask extends TimerTask {

    /*
     * (non-Javadoc)
     * 
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
      try {
        System.out.format("Time's up!%n");
        timer.cancel(); // Terminate the timer thread
        MappingService mappingService = new MappingServiceJpa();
        MapProjectList mapProjects = mappingService.getMapProjects();
        for (MapProject project : mapProjects.getMapProjects()) {
          // DISABLED because we don't actually want this called in prod
          // mappingService.compareFinishedMapRecords(project);
        }
        mappingService.close();
        
      } catch (Exception e) {
        Logger.getLogger(this.getClass()).error(
            "Error running the process to compute list of finished records.");
      }
    }
  }

}
