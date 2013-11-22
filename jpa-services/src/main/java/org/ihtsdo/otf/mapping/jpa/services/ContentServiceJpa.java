package org.ihtsdo.otf.mapping.jpa.services;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;


import org.ihtsdo.otf.mapping.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;


public class ContentServiceJpa implements ContentService {
		
	/** The factory. */
	private EntityManagerFactory factory;

	/**
	 * Instantiates an empty {@link ContentServiceJpa}.
	 */
	public ContentServiceJpa() {
		factory =
				Persistence.createEntityManagerFactory("MappingServiceDS");
	}

	/**
	 * Returns the concept for id.
	 *
	 * @param id the id
	 * @return the concept for id
	 */
	public Concept getConceptForId(Long id) {
		return getConcept(id);

	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.ContentService#getConcept(java.lang.Long)
	 */
	@Override
	public Concept getConcept(Long conceptId) {
		EntityManager manager = factory.createEntityManager();
		Concept cpt = manager.find(ConceptJpa.class, conceptId);
		manager.detach(cpt);
		manager.close();
		return cpt;
	}

}
