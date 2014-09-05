package org.ihtsdo.otf.mapping.helpers;

import java.util.List;

public interface TreePositionDescription {

	public String getName();
	
	public void setName(String name);
	
	public List<TreePositionReferencedConcept> getReferencedConcepts();
	
	public void setReferencedConcepts(List<TreePositionReferencedConcept> referencedConcepts);
	
	public void addReferencedConcept(TreePositionReferencedConcept referencedConcept);
	
	public void removeReferencedConcept(TreePositionReferencedConcept referencedConcept);
}
