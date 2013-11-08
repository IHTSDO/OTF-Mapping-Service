package org.ihtsdo.otf.mapping.helpers;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.ihtsdo.otf.mapping.jpa.ConceptJpa;


public class JpaTest {

	private EntityManager manager;

	public JpaTest(EntityManager manager) {
		this.manager = manager;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EntityManagerFactory factory = Persistence.createEntityManagerFactory("MappingServiceDS");
		EntityManager manager = factory.createEntityManager();
		JpaTest test = new JpaTest(manager);

		EntityTransaction tx = manager.getTransaction();
		try {
			tx.begin();
			test.countConcepts();
		
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
            tx.rollback();
		}

		//test.listEmployees();

		System.out.println(".. done");
		manager.close();
		factory.close();
	}

	private void countConcepts() {
		int numOfConcepts = manager.createQuery("Select a From ConceptJpa a", ConceptJpa.class).getResultList().size();
		System.out.println("concept count: " + numOfConcepts);
		
		/**if (numOfConcepts == 0) {
			UserDetails userDetails = new UserDetails();
			userDetails.setUserId(154);
			userDetails.setUserName("Captain Nemo");
			manager.persist(userDetails);
		}*/
	}

	/**private void listEmployees() {
		List<Employee> resultList = manager.createQuery("Select a From Employee a", Employee.class).getResultList();
		System.out.println("num of employess:" + resultList.size());
		for (Employee next : resultList) {
			System.out.println("next employee: " + next);
		}
	}*/


}
