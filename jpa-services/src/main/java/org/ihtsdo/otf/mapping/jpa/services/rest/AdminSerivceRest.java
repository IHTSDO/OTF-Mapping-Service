package org.ihtsdo.otf.mapping.jpa.services.rest;

public interface AdminSerivceRest {

	void luceneReindex(String indexedObjects, String authToken) throws Exception;

}