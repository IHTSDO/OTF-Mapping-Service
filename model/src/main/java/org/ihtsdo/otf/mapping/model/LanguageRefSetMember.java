package org.ihtsdo.otf.mapping.model;

/**
 * Represents a language reference set member
 */
public interface LanguageRefSetMember extends DescriptionRefSetMember {

	/**
	 *  returns the acceptabilityId
	 *  @return the acceptability id
	 * 
	 */
	public Long getAcceptabilityId();
	
	/**
	 * sets the acceptabilityId
	 * @param acceptabilityId the acceptability id
	 */
	public void setAcceptabilityId(Long acceptabilityId);
}
