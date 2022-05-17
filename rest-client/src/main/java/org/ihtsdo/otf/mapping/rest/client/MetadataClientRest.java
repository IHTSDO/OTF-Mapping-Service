/**
 * 
 */
package org.ihtsdo.otf.mapping.rest.client;

import org.ihtsdo.otf.mapping.helpers.KeyValuePairList;
import org.ihtsdo.otf.mapping.helpers.KeyValuePairLists;
import org.ihtsdo.otf.mapping.jpa.services.rest.MetadataServiceRest;

/**
 * @author Nuno Marques
 *
 */
public class MetadataClientRest extends RootClientRest implements MetadataServiceRest {

	@Override
	public KeyValuePairLists getMetadata(String terminology, String version, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public KeyValuePairLists getAllMetadata(String terminology, String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public KeyValuePairList getAllTerminologiesLatestVersions(String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public KeyValuePairLists getAllTerminologiesVersions(String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public String getAllGmdnVersions(String authToken) throws Exception{
      // TODO Auto-generated method stub
      return null;  
    }
    
    @Override
    public String getAllAtcVersions(String authToken) throws Exception{
      // TODO Auto-generated method stub
      return null;  
    }

    @Override
    public String getAllIcpc2NOVersions(String authToken) throws Exception{
      // TODO Auto-generated method stub
      return null;  
    }

	@Override
	public String getAllMimsAllergyVersions(String authToken) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
}
