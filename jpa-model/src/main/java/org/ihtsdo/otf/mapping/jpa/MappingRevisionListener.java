package org.ihtsdo.otf.mapping.jpa;

import org.hibernate.envers.RevisionListener;

/**
 * Listener for custom revision entity Goal: set the audit field userName to the
 * currently logged in user [NOT CURRENTLY IMPLEMENTED]
 * @author Patrick
 * 
 */
public class MappingRevisionListener implements RevisionListener {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.envers.RevisionListener#newRevision(java.lang.Object)
	 */
	@Override
	public void newRevision(Object revisionEntity) {

		MappingRevisionEntity mappingRevisionEntity =
				(MappingRevisionEntity) revisionEntity;

		mappingRevisionEntity.setTimestamp(System.currentTimeMillis());
		mappingRevisionEntity.setUserName("default");
	}

}
