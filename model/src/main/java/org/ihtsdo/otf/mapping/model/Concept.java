package org.ihtsdo.otf.mapping.model;

import java.util.Set;

/**
 * Represents a concept in a terminology.
 */
public interface Concept extends Component {

	/**
	 * Returns the definition status id.
	 *
	 * @return the definition status id
	 */
	public Long getDefinitionStatusId();

	/**
	 * Sets the definition status id.
	 *
	 * @param definitionStatusId the definition status id
	 */
	public void setDefinitionStatusId(Long definitionStatusId);

	/**
	 * Returns the descriptions.
	 *
	 * @return the descriptions
	 */
	public Set<Description> getDescriptions();

	/**
	 * Sets the descriptions.
	 *
	 * @param descriptions the descriptions
	 */
	public void setDescriptions(Set<Description> descriptions);

	/**
	 * Adds the description.
	 *
	 * @param description the description
	 */
	public void addDescription(Description description);

	/**
	 * Removes the description.
	 *
	 * @param description the description
	 */
	public void removeDescription(Description description);

	/**
	 * Returns the relationships.
	 *
	 * @return the relationships
	 */
	public Set<Relationship> getRelationships();

	/**
	 * Sets the relationships.
	 *
	 * @param relationships the relationships
	 */
	public void setRelationships(Set<Relationship> relationships);

	/**
	 * Returns the inverse relationships.
	 *
	 * @return the inverse relationships
	 */
	public Set<Relationship> getInverseRelationships();

	/**
	 * Sets the inverse relationships.
	 *
	 * @param inverseRelationships the inverse relationships
	 */
	public void setInverseRelationships(Set<Relationship> inverseRelationships);
	
	/**
	 * Gets the default preferred name.
	 *
	 * @return the default preferred name
	 */
	public String getDefaultPreferredName();
	
	/**
	 * Sets the default preferred name.
	 *
	 * @param defaultPreferredName the new default preferred name
	 */
	public void setDefaultPreferredName(String defaultPreferredName);
}
