package org.ihtsdo.otf.mapping.jpa.services.rest;

import org.ihtsdo.otf.mapping.helpers.PfsParameterJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.TerminologyVersionList;
import org.ihtsdo.otf.mapping.rf2.Concept;

/**
 * The Interface ContentServiceRest.
 *
 * @author ${author}
 */
public interface ContentServiceRest {

	/**
	   * Returns the concept for id, terminology, and terminology version.
	   *
	   * @param terminologyId the terminology id
	   * @param terminology the concept terminology
	   * @param terminologyVersion the terminology version
	   * @param authToken the auth token
	   * @return the concept
	   * @throws Exception the exception
	   */
	Concept getConcept(String terminologyId, String terminology, String terminologyVersion, String authToken)
			throws Exception;

	/**
	   * Returns the concept for id, terminology. Looks in the latest version of the
	   * terminology.
	   *
	   * @param terminologyId the id
	   * @param terminology the concept terminology
	   * @param authToken the auth token
	   * @return the concept
	   * @throws Exception the exception
	   */
	Concept getConcept(String terminologyId, String terminology, String authToken) throws Exception;

	/**
	   * Returns the concept for search string.
	   *
	   * @param query the lucene search string
	   * @param authToken the auth token
	   * @return the concept for id
	   * @throws Exception the exception
	   */
	SearchResultList findConceptsForQuery(String query, String authToken) throws Exception;

	/**
	   * Returns the descendants of a concept as mapped by relationships and inverse
	   * relationships.
	   *
	   * @param terminologyId the terminology id
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findDescendantConcepts(String terminologyId, String terminology, String terminologyVersion,
			String authToken) throws Exception;

	/**
	   * Returns the immediate children of a concept given terminology information.
	   *
	   * @param id the terminology id
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findChildConcepts(String id, String terminology, String terminologyVersion, String authToken)
			throws Exception;

	/**
	   * Find delta concepts for terminology.
	   *
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param authToken the auth token
	   * @param pfsParameter the pfs parameter
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findDeltaConceptsForTerminology(String terminology, String terminologyVersion, String authToken,
			PfsParameterJpa pfsParameter) throws Exception;

	/**
	   * Returns the index viewer indexes.
	   *
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param authToken the auth token
	   * @return the index viewer indexes
	   * @throws Exception the exception
	   */
	SearchResultList getIndexDomains(String terminology, String terminologyVersion, String authToken) throws Exception;

	/**
	   * Returns the index viewer pages for index.
	   *
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param index the index
	   * @param authToken the auth token
	   * @return the index viewer pages for index
	   * @throws Exception the exception
	   */
	SearchResultList getIndexViewerPagesForIndex(String terminology, String terminologyVersion, String index,
			String authToken) throws Exception;

	/**
	   * Returns the index viewer details for link.
	   *
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param domain the domain
	   * @param link the link
	   * @param authToken the auth token
	   * @return the index viewer details for link
	   * @throws Exception the exception
	   */
	String getIndexViewerDetailsForLink(String terminology, String terminologyVersion, String domain, String link,
			String authToken) throws Exception;

	/**
	   * Find index viewer search result entries.
	   *
	   * @param terminology the terminology
	   * @param terminologyVersion the terminology version
	   * @param domain the domain
	   * @param searchField the search field
	   * @param subSearchField the sub search field
	   * @param subSubSearchField the sub sub search field
	   * @param allFlag the all flag
	   * @param authToken the auth token
	   * @return the search result list
	   * @throws Exception the exception
	   */
	SearchResultList findIndexViewerEntries(String terminology, String terminologyVersion, String domain,
			String searchField, String subSearchField, String subSubSearchField, boolean allFlag, String authToken)
			throws Exception;

	/**
	 * Loads unpublished complex maps.
	 *
	 * @param inputFile The input file.
	 * @param memberFlag The members flag.
	 * @param recordFlag The records flag.
	 * @param refsetId the refset id
	 * @param workflowStatus The workflow status to assign to created map records.
	 * @param authToken The auth token
	 * @throws Exception The execution exception
	 */
	void loadMapRecordRf2ComplexMap(String inputFile, Boolean memberFlag, Boolean recordFlag, String refsetId,
			String workflowStatus, String authToken) throws Exception;
	
	/**
	 * Loads simple maps. - the members flag loads refset members if "true" - the
	 * records flag loads map records if "true"
	 *
	 * @param inputFile The input file.
	 * @param memberFlag the member flag
	 * @param recordFlag the record flag
	 * @param refsetId the refset id
	 * @param workflowStatus the workflow status
	 * @param authToken The auth token
	 * @throws Exception The execution exception
	 */
	void loadMapRecordRf2SimpleMap(String inputFile, Boolean memberFlag, Boolean recordFlag, String refsetId, 
			String workflowStatus, String authToken) throws Exception;
	
	/**
	 * Converts claml data to RF2 objects.
	 * 
	 * @param terminology The terminology.
	 * @param version The terminology version.
	 * @param inputDir The directory where the input files are located.
	 * @param authToken The auth token
	 * @throws Exception The execution exception
	 */
	void loadTerminologyClaml(String terminology, String version,
			String inputDir, String authToken) throws Exception;
	

    /**
     * Download terminology gmdn.
     *
     * @param authToken the auth token
     * @throws Exception the exception
     */
    void downloadTerminologyGmdn(String authToken) throws Exception;	
	
	/**
	 * Converts GMDN data to RF2 objects.
	 * 
	 * @param version The terminology version.
	 * @param inputDir The directory where the input files are located.
	 * @param authToken The auth token
	 * @throws Exception The execution exception
	 */
	void loadTerminologyGmdn(String version,
			String inputDir, String authToken) throws Exception;	
	
	/**
	 * Removes a terminology from a database.
	 * 
	 * @param refsetId Refset Id to be deleted.
	 * @param authToken The auth token
	 * @return boolean Indicate if success or failure.
	 * @throws Exception The execution exception
	 */
	boolean removeMapRecord(String refsetId, String authToken) throws Exception;	
	
	/**
	 * Removes a terminology from a database.
	 * 
	 * @param terminology The terminology.
	 * @param version The terminology version.
	 * @param authToken The auth token
	 * @return boolean Indicate if success or failure.
	 * @throws Exception The execution exception
	 */
	boolean removeTerminology(String terminology, String version,
			String authToken) throws Exception;
	
	/**
	 * Loads an RF2 Delta of SNOMED CT data.
	 *
	 * @param terminology The terminology.
	 * @param lastPublicationDate Date (YYYYMMDD) of last publication.
	 * @param inputDir The directory where the input files are located.
	 * @param authToken The auth token
	 * @throws Exception The execution exception
	 */
	void loadTerminologyRf2Delta(String terminology, String lastPublicationDate,
			String inputDir, String authToken) throws Exception;
	
	/**
	 * Loads an RF2 Snapshot of SNOMED CT data into a database.
	 *
	 * @param terminology The terminology.
	 * @param version The terminology version.
	 * @param inputDir The directory where the input files are located.
	 * @param treePositions Indicate if tree positions should be calculated.
	 * @param sendNotification Indicate if a notification should be sent.
	 * @param authToken The auth token
	 * @throws Exception The execution exception
	 */
  void loadTerminologyRf2Snapshot(String terminology, String version,
    String inputDir, Boolean treePositions, Boolean sendNotification,
			String authToken) throws Exception;

	/**
	 * loads a simple code list data file.
	 * 
	 * The format of the file is: code|string[|synonym,...]
	 * 
	 * It uses the claml metadata help for metadata
	 * 
	 * @param terminology The terminology.
	 * @param version The terminology version.
	 * @param inputDir The directory where the input files are located.
	 * @param authToken The auth token
	 * @throws Exception The execution exception
	 */
	void loadTerminologySimple(String terminology, String version,
			String inputDir, String authToken) throws Exception;

	/**
	 * Removes and loads an RF2 Snapshot of SNOMED CT data into a database.
	 * 
	 * @param terminology The terminology.
	 * @param version The terminology version.
	 * @param inputDir The directory where the input files are located.
	 * @param treePositions Indicate if tree positions should be calculated.
	 * @param sendNotification Indicate if a notification should be sent.
	 * @param authToken The auth token
	 * @throws Exception The execution exception
	 */
	void reloadTerminologyRf2Snapshot(String terminology, String version,
			String inputDir, Boolean treePositions, Boolean sendNotification,
			String authToken) throws Exception;
	
	/**
	 * Removes and loads simple maps. - the members flag loads refset members if "true" - the
	 * records flag loads map records if "true"
	 *
	 * @param refsetId Refset Id to be deleted.
	 * @param inputFile The input file.
	 * @param memberFlag the member flag
	 * @param recordFlag the record flag
	 * @param workflowStatus the workflow status
	 * @param authToken The auth token
	 * @return true, if successful
	 * @throws Exception The execution exception
	 */
	boolean reloadMapRecord(String refsetId, String inputFile, Boolean memberFlag,
			Boolean recordFlag, String workflowStatus, String authToken)
			throws Exception;


  /**
   * Returns the terminology versions.
   *
   * @param terminology the terminology
   * @param authToken the auth token
   * @return the terminology versions
   * @throws Exception the exception
   */
  TerminologyVersionList getTerminologyVersions(String terminology,
    String authToken) throws Exception;

  /**
   * Returns the terminology version scopes.
   *
   * @param terminology the terminology
   * @param version the version
   * @param authToken the auth token
   * @return the terminology version scopes
   * @throws Exception the exception
   */
  TerminologyVersionList getTerminologyVersionScopes(String terminology,
    String version, TerminologyVersionList termVersionList, String authToken) throws Exception;

  /**
   * Load terminology rf 2 snapshot aws.
   *
   * @param terminology the terminology
   * @param version the version
   * @param awsFileName the aws file name
   * @param treePositions the tree positions
   * @param sendNotification the send notification
   * @param authToken the auth token
   * @throws Exception the exception
   */
  void loadTerminologyAwsRf2Snapshot(String terminology, String version,
    String awsFileName, Boolean treePositions, Boolean sendNotification,
    String authToken) throws Exception;

  /**
   * Reload terminology aws rf 2 snapshot.
   *
   * @param terminology the terminology
   * @param removeVersion the remove version
   * @param loadVersion the load version
   * @param awsZipFileName the aws zip file name
   * @param treePositions the tree positions
   * @param sendNotification the send notification
   * @param authToken the auth token
   * @throws Exception the exception
   */
  void reloadTerminologyAwsRf2Snapshot(String terminology, String removeVersion,
    String loadVersion, String awsZipFileName, Boolean treePositions,
    Boolean sendNotification, String authToken) throws Exception;

  /**
   * Reload refset member aws snapshot.
   *
   * @param refsetId the refset id
   * @param awsFileName the aws file name
   * @param authToken the auth token
   * @return true, if successful
   * @throws Exception the exception
   */
  boolean reloadRefsetMemberAwsSnapshot(String refsetId, String awsFileName,
    String authToken) throws Exception;

}