package org.ihtsdo.otf.mapping.pojo;

import org.ihtsdo.otf.mapping.model.SimpleMapRefSetMember;

/**
 * Concrete implementation of {@link SimpleMapRefSetMember}.
 */
public class SimpleMapRefSetMemberImpl extends AbstractConceptRefSetMember
		implements SimpleMapRefSetMember {
	
	/** The map target */
	private String mapTarget;

	/** returns the map target
	 * @return the map target
	 */
	@Override
	public String getMapTarget() {
		return this.mapTarget;
	}

	/** sets the map target
	 * @param mapTarget the map target
	 */
	@Override
	public void setMapTarget(String mapTarget) {
		this.mapTarget = mapTarget;
	}

}
