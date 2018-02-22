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
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressEvent;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;

public class RemoverAlgorithm extends RootServiceJpa
		implements Algorithm, AutoCloseable {

	/** Listeners. */
	private List<ProgressListener> listeners = new ArrayList<>();

	/** The request cancel flag. */
	boolean requestCancel = false;

	/** Name of terminology to be loaded. */
	private String terminology;

	/** Terminology version */
	private String version;
	
	/** The log. */
	private static Logger log;
	
	/** The log file. */
	private File logFile;
	
	@SuppressWarnings("static-access")
	public RemoverAlgorithm(String terminology, String version)
			throws Exception {
		super();
		this.terminology = terminology;
		this.version = version;
		
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
	    
	    logFile = new File(logDirectory, "remove_" + terminology + ".log");
		LoggerUtility.setConfiguration("remove", logFile.getAbsolutePath());
		this.log = LoggerUtility.getLogger("remove");
	}

	@Override
	public void compute() throws Exception {
	  
	  // clear log before starting process
	  PrintWriter writer = new PrintWriter(logFile);
      writer.print("");
      writer.close(); 
	  
		log.info("Start removing terminology");
		log.info("  terminology  = " + terminology);
		log.info("  version      = " + version);
		
		try {
			Properties config = ConfigUtility.getConfigProperties();

			EntityManager manager = getEntityManager();
			if (manager == null) {
				// NOTE: ideal this would not use entity manager,
				// but we do not have services for all data types yet.
				EntityManagerFactory factory = Persistence
					.createEntityManagerFactory("MappingServiceDS", config);
				manager = factory.createEntityManager();
			}
			
			EntityTransaction tx = manager.getTransaction();
			
			try {
				
				// truncate all the tables that we are going to use first
				tx.begin();

				// delete RefSets
				Query query = manager.createQuery(
						"DELETE From SimpleRefSetMemberJpa rs where terminology = :terminology and terminologyVersion = :version");
				query.setParameter("terminology", terminology);
				query.setParameter("version", version);
				int deleteRecords = query.executeUpdate();
				log.info("simple_ref_set records deleted: " + deleteRecords);

				// delete Map RefSets
				query = manager.createQuery(
						"DELETE From SimpleMapRefSetMemberJpa rs where terminology = :terminology and terminologyVersion = :version");
				query.setParameter("terminology", terminology);
				query.setParameter("version", version);
				deleteRecords = query.executeUpdate();
				log.info("simple_map_ref_set records deleted: " + deleteRecords);

				// delete Complex Map RefSets
				query = manager.createQuery(
						"DELETE From ComplexMapRefSetMemberJpa rs where terminology = :terminology and terminologyVersion = :version");
				query.setParameter("terminology", terminology);
				query.setParameter("version", version);
				deleteRecords = query.executeUpdate();
				log.info("complex_map_ref_set records deleted: " + deleteRecords);

				// delete Attribute Value RefSets
				query = manager.createQuery(
						"DELETE From AttributeValueRefSetMemberJpa rs where terminology = :terminology and terminologyVersion = :version");
				query.setParameter("terminology", terminology);
				query.setParameter("version", version);
				deleteRecords = query.executeUpdate();
				log.info("attribute_value_ref_set records deleted: " + deleteRecords);

				// delete Language RefSets
				query = manager.createQuery(
						"DELETE From LanguageRefSetMemberJpa rs where terminology = :terminology and terminologyVersion = :version");
				query.setParameter("terminology", terminology);
				query.setParameter("version", version);
				deleteRecords = query.executeUpdate();
				log.info("language_ref_set records deleted: " + deleteRecords);

				// delete Terminology Elements (Descriptions)
				query = manager.createQuery(
						"DELETE From DescriptionJpa d where terminology = :terminology and terminologyVersion = :version");
				query.setParameter("terminology", terminology);
				query.setParameter("version", version);
				deleteRecords = query.executeUpdate();
				log.info("description records deleted: " + deleteRecords);

				// delete Terminology Elements (Relationships)
				query = manager.createQuery(
						"DELETE From RelationshipJpa r where terminology = :terminology and terminologyVersion = :version");
				query.setParameter("terminology", terminology);
				query.setParameter("version", version);
				deleteRecords = query.executeUpdate();
				log.info("relationship records deleted: " + deleteRecords);

				// delete Terminology Elements (Concepts)
				query = manager.createQuery(
						"DELETE From ConceptJpa c where terminology = :terminology and terminologyVersion = :version");
				query.setParameter("terminology", terminology);
				query.setParameter("version", version);
				deleteRecords = query.executeUpdate();
				log.info("concept records deleted: " + deleteRecords);

				// commit all deletes
				tx.commit();

				ContentService contentService = new ContentServiceJpa();
				log.info("Start removing tree positions from " + terminology);
				contentService.clearTreePositions(terminology, version);
				log.info("Done removing tree positions from " + terminology);
				contentService.close();
				log.info("Done removing terminology");

			} catch (Exception e) {
				tx.rollback();
				log.error(e.getMessage(), e);
				throw e;
			} finally {
				// Clean-up
				if (manager != null)
					manager.close();

				if (factory != null)
					factory.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		    log.error(e.getMessage(), e);
			throw new Exception("Unexpected exception:", e);
		} finally {
	        //remove remove logger configuration
			LoggerUtility.removeLogger("remove");
		}

	}

	/**
	 * Fires a {@link ProgressEvent}.
	 * 
	 * @param pct
	 *            percent done
	 * @param note
	 *            progress note
	 */
	public void fireProgressEvent(int pct, String note) {
		ProgressEvent pe = new ProgressEvent(this, pct, pct, note);
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).updateProgress(pe);
		}
		log.info("    " + pct + "% " + note);
	}

	/* see superclass */
	@Override
	public void reset() throws Exception {
		// n/a
	}

	/* see superclass */
	@Override
	public void addProgressListener(ProgressListener l) {
		listeners.add(l);
	}

	/* see superclass */
	@Override
	public void removeProgressListener(ProgressListener l) {
		listeners.remove(l);
	}

	/* see superclass */
	@Override
	public void cancel() {
		requestCancel = true;
	}

	@Override
	public void checkPreconditions() throws Exception {
		// n/a
	}

}
