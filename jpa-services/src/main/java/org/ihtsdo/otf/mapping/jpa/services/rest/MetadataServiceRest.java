package org.ihtsdo.otf.mapping.jpa.services.rest;

import org.ihtsdo.otf.mapping.helpers.KeyValuePairList;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairLists;

public interface MetadataServiceRest {

	/**
	   * Returns all metadata for a terminology and version.
	   *
	   * @param terminology the terminology
	   * @param version the version
	   * @param authToken the auth token
	   * @return the all metadata
	   * @throws Exception the exception
	   */
	KeyValuePairLists getMetadata(String terminology, String version, String authToken) throws Exception;

	/**
	   * Returns all metadata for the latest version.
	   *
	   * @param terminology the terminology
	   * @param authToken the auth token
	   * @return the metadata
	   * @throws Exception the exception
	   */
	KeyValuePairLists getAllMetadata(String terminology, String authToken) throws Exception;

	/**
	   * Returns all terminologies with only their latest version.
	   *
	   * @param authToken the auth token
	   * @return the all terminologies latest versions
	   * @throws Exception the exception
	   */
	KeyValuePairList getAllTerminologiesLatestVersions(String authToken) throws Exception;

	/**
	   * Returns all terminologies and all versions.
	   *
	   * @param authToken the auth token
	   * @return all terminologies and versions
	   * @throws Exception the exception
	   */
	KeyValuePairLists getAllTerminologiesVersions(String authToken) throws Exception;
	
	/**
	 * Returns the all downloaded gmdn versions as a semi-colon delimited string.
	 *
	 * @param authToken the auth token
	 * @return the all gmdn versions
	 * @throws Exception the exception
	 */
	String getAllGmdnVersions(String authToken) throws Exception;

	/**
	 * Returns the latest atc version from the api.
	 *
	 * @param authToken the auth token
	 * @return the atc version
	 * @throws Exception the exception
	 */
	String getAllAtcVersions(String authToken) throws Exception;

    /**
     * Returns the latest ICPC2_NO version from the api.
     *
     * @param authToken the auth token
     * @return the icpc2_NO version
     * @throws Exception the exception
     */
    String getAllIcpc2NOVersions(String authToken) throws Exception;
  
    /**
     * Returns the latest ICD10NO version from the api.
     *
     * @param authToken the auth token
     * @return the ICD10NO version
     * @throws Exception the exception
     */
    String getAllIcd10NOVersions(String authToken) throws Exception;
   
	/**
	 * Returns the latest mims allergy version from the loading folder.
	 *
	 * @param authToken the auth token
	 * @return the mims allergy version
	 * @throws Exception the exception
	 */
	String getAllMimsAllergyVersions(String authToken) throws Exception;
}