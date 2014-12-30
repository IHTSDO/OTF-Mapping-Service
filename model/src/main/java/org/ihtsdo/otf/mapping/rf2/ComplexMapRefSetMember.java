package org.ihtsdo.otf.mapping.rf2;

/**
 * Generically represents a complex map reference set member
 */
public interface ComplexMapRefSetMember extends ConceptRefSetMember {

  /**
   * returns the mapBlock
   * @return the mapBlock
   */
  public int getMapBlock();

  /**
   * sets the mapBlock
   * @param mapBlock the mapBlock
   */
  public void setMapBlock(int mapBlock);

  /**
   * returns the mapBlockRule
   * @return the mapBlockRule
   */
  public String getMapBlockRule();

  /**
   * sets the mapBlockRule
   * @param mapBlockRule the mapBlockRule
   */
  public void setMapBlockRule(String mapBlockRule);

  /**
   * returns the mapBlockAdvice
   * @return the mapBlockAdvice
   */
  public String getMapBlockAdvice();

  /**
   * sets the mapBlockAdvice
   * @param mapBlockAdvice the mapBlockAdvice
   */
  public void setMapBlockAdvice(String mapBlockAdvice);

  /**
   * returns the mapGroup
   * @return the mapGroup
   * 
   */
  public int getMapGroup();

  /**
   * sets the mapGroup
   * @param mapGroup the mapGroup
   */
  public void setMapGroup(int mapGroup);

  /**
   * returns the mapPriority
   * @return the mapPriority
   * 
   */
  public int getMapPriority();

  /**
   * sets the mapPriority
   * @param mapPriority the mapPriority
   */
  public void setMapPriority(int mapPriority);

  /**
   * returns the mapRule
   * @return the mapRule
   * 
   */
  public String getMapRule();

  /**
   * sets the mapRule
   * @param mapRule the mapRule
   */
  public void setMapRule(String mapRule);

  /**
   * returns the mapAdvice
   * @return the mapAdvice
   * 
   */
  public String getMapAdvice();

  /**
   * sets the mapAdvice
   * @param mapAdvice the mapAdvice
   */

  public void setMapAdvice(String mapAdvice);

  /**
   * returns the mapTarget
   * @return the mapTarget
   * 
   */

  public String getMapTarget();

  /**
   * sets the mapTarget
   * @param mapTarget the mapTarget
   */

  public void setMapTarget(String mapTarget);

  /**
   * returns the mapRelationId
   * @return the mapRelationId
   * 
   */
  public Long getMapRelationId();

  /**
   * sets the mapRelationId
   * @param mapRelationId the mapRelationId
   */
  public void setMapRelationId(Long mapRelationId);

}