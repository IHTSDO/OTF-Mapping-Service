package org.ihtsdo.otf.mapping.jpa.algo;

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

	public MapsRemoverAlgorithm(String refsetId) throws Exception {
		super();
		this.refsetId = refsetId;
	}

	@Override
	public void compute() throws Exception {
		
		Logger.getLogger(getClass()).info("Starting removing terminology");
		Logger.getLogger(getClass()).info("  refsetId = " + refsetId);
		
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
				Logger.getLogger(getClass())
						.info("    simple_map_ref_set records deleted: "
								+ deleteRecords);

				query = manager.createQuery(
						"DELETE From ComplexMapRefSetMemberJpa rs where refsetId = :refsetId");
				query.setParameter("refsetId", refsetId);
				deleteRecords = query.executeUpdate();
				Logger.getLogger(getClass())
						.info("    complex_map_ref_set records deleted: "
								+ deleteRecords);

				tx.commit();
				Logger.getLogger(getClass()).info("Done ...");

			} catch (Exception e) {
				tx.rollback();
				throw e;
			}

			// Clean-up
			manager.close();
			factory.close();

		} catch (Exception e) {
			e.printStackTrace();
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
