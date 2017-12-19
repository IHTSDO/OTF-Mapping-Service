package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.jpa.helpers.LoggerUtility;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

/**
 * Goal which removes a terminology from a database.
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 */
public class MapsRemoverAlgorithm extends RootServiceJpa
		implements Algorithm, AutoCloseable {

	/** Listeners. */
	private List<ProgressListener> listeners = new ArrayList<>();

	/** The request cancel flag. */
	private boolean requestCancel = false;
	
	/** Ref set id to remove */
	private String refsetId;
	
	   /** The log. */
    private static Logger log;
    
    /** The log file. */
    private File logFile;

	public MapsRemoverAlgorithm(String refsetId) throws Exception {
		super();
		this.refsetId = refsetId;
			      
        //initialize logger
        String rootPath = ConfigUtility.getConfigProperties()
              .getProperty("map.principle.source.document.dir");
        if (!rootPath.endsWith("/") && !rootPath.endsWith("\\")) {
          rootPath += "/";
        }
        rootPath += "logs";
        File logDirectory = new File(rootPath);
        if (!logDirectory.exists()) {
            logDirectory.mkdir();
        }
        
        logFile = new File(logDirectory, "remove_maps_" + refsetId + ".log");
        LoggerUtility.setConfiguration("remove_maps", logFile.getAbsolutePath());
        this.log = LoggerUtility.getLogger("remove_maps");
	}

	@Override
	public void compute() throws Exception {
	    // clear log before starting process
      PrintWriter writer = new PrintWriter(logFile);
      writer.print("");
      writer.close(); 
      
		log.info("Starting removing terminology");
		log.info("  refsetId = " + refsetId);
		
		try {
			Properties config = ConfigUtility.getConfigProperties();

			// NOTE: ideally this would not use entity manager,
			// but we do not have services for all data types yet.
			EntityManagerFactory factory = Persistence
					.createEntityManagerFactory("MappingServiceDS", config);
			EntityManager manager = factory.createEntityManager();

			EntityTransaction tx = manager.getTransaction();
			try {

				// truncate all the tables that we are going to use first
				tx.begin();

				Query query = manager.createQuery(
						"DELETE From SimpleMapRefSetMemberJpa rs where refsetId = :refsetId");
				query.setParameter("refsetId", refsetId);
				int deleteRecords = query.executeUpdate();
				log.info("    simple_map_ref_set records deleted: "
								+ deleteRecords);

				query = manager.createQuery(
						"DELETE From ComplexMapRefSetMemberJpa rs where refsetId = :refsetId");
				query.setParameter("refsetId", refsetId);
				deleteRecords = query.executeUpdate();
				log.info("    complex_map_ref_set records deleted: "
								+ deleteRecords);

				tx.commit();
				log.info("Done ...");

			} catch (Exception e) {
				tx.rollback();
				throw e;
			}

			// Clean-up
			manager.close();
			factory.close();

		} catch (Exception e) {
			e.printStackTrace();
			log.info(e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
              log.info(element.toString());
            }
			throw new Exception("Unexpected exception:", e);
		}
	}

	@Override
	public void addProgressListener(ProgressListener l) {
		listeners.add(l);
	}

	@Override
	public void removeProgressListener(ProgressListener l) {
		listeners.remove(l);
	}

	@Override
	public void reset() throws Exception {
		// n/a
	}

	@Override
	public void checkPreconditions() throws Exception {
		// n/a
	}

	@Override
	public void cancel() throws Exception {
		requestCancel = true;
	}

}
