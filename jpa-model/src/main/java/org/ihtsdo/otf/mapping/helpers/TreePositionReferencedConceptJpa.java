package org.ihtsdo.otf.mapping.helpers;

public class TreePositionReferencedConceptJpa implements
		TreePositionReferencedConcept {

	private String terminologyId;
	
	private String displayName;
	
	@Override
	public void setTerminologyId(String terminologyId) {
		this.terminologyId = terminologyId;

	}

	@Override
	public String getTerminologyId() {
		return this.terminologyId;
	}

	@Override
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String getDisplayName() {
		return this.displayName;
	}

}
