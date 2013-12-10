package org.ihtsdo.otf.mapping.jpa;

import static org.junit.Assert.fail;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.junit.Test;

public class MapProjectJpaTest {

	/** The manager. */
	private EntityManager manager;

	@Test
	public void testMapProjectLoad() {
		try {

			System.out.println("  In MapProjectJpaTest.java");

			// create Entitymanager
			EntityManagerFactory factory =
					Persistence.createEntityManagerFactory("MappingServiceDS");
			manager = factory.createEntityManager();

			EntityTransaction tx = manager.getTransaction();
			try {

				tx.begin();
				// truncate MapProjects
				Query query = manager.createQuery("DELETE From MapSpecialistJpa m");
				int deleteRecords = query.executeUpdate();
				System.out.println("mapSpecialist records deleted: " + deleteRecords);manager.createQuery("DELETE From MapProjectJpa m");
				query = manager.createQuery("DELETE From MapLeadJpa m");
				deleteRecords = query.executeUpdate();
				System.out.println("mapLead records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From MapAdviceJpa m");
				deleteRecords = query.executeUpdate();
				System.out.println("mapAdvice records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From MapProjectJpa m");
				deleteRecords = query.executeUpdate();
				System.out.println("mapProject records deleted: " + deleteRecords);
				tx.commit();
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
				fail("Failure to delete mapProject records.");
			}
			
			try {
				System.out.println("Loading MapProjects...");				
				tx.begin();
				loadMapProjects();
				tx.commit();
				
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
				fail("Failure to load mapProject records.");
			}

			// Clean-up
			manager.close();
			factory.close();

		} catch (Throwable e) {
			e.printStackTrace();
			fail("Failure to close entity manager or factory");
		}

	}

	private void loadMapProjects() throws Exception {

		MapProjectJpa mapProject = new MapProjectJpa();

		mapProject.setName("SNOMEDCT to ICD10CM Mapping");
		mapProject.setRefSetId(new Long("123456789"));
		mapProject.setSourceTerminology("SNOMEDCT");
		mapProject.setSourceTerminologyVersion("20130731");
		mapProject.setDestinationTerminology("ICD10CM");
		mapProject.setDestinationTerminologyVersion("2010");
		mapProject.setBlockStructure(true);
		mapProject.setGroupStructure(false);
		mapProject.setPublished(false);
		manager.persist(mapProject);
		
		MapLeadJpa mapLeadBrian = new MapLeadJpa();
		mapLeadBrian.setName("Brian");
		mapLeadBrian.setUserName("bcarlsen");
		mapLeadBrian.setEmail("bcarlsen@westcoastinformatics.com");
		manager.persist(mapLeadBrian);
		mapProject.addMapLead(mapLeadBrian);
		
		MapLeadJpa mapLeadRory = new MapLeadJpa();
		mapLeadRory.setName("Rory");
		mapLeadRory.setUserName("rda");
		mapLeadRory.setEmail("rda@ihtsdo.org");
		manager.persist(mapLeadRory);
		mapProject.addMapLead(mapLeadRory);
		
		MapSpecialistJpa mapSpecialistDeborah = new MapSpecialistJpa();
		mapSpecialistDeborah.setName("Deborah");
		mapSpecialistDeborah.setUserName("dshapiro");
		mapSpecialistDeborah.setEmail("dshapiro@westcoastinformatics.com");
		manager.persist(mapSpecialistDeborah);
		mapProject.addMapSpecialist(mapSpecialistDeborah);
		
		MapSpecialistJpa mapSpecialistPatrick = new MapSpecialistJpa();
		mapSpecialistPatrick.setName("Patrick");
		mapSpecialistPatrick.setUserName("pgranvold");
		mapSpecialistPatrick.setEmail("pgranvold@westcoastinformatics.com");
		manager.persist(mapSpecialistPatrick);
		mapProject.addMapSpecialist(mapSpecialistPatrick);



	}

}
