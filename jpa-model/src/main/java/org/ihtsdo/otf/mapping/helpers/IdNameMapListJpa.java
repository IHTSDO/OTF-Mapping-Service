package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.helpers.IdNameMap;
import org.ihtsdo.otf.mapping.helpers.IdNameMapList;

/**
 * Container for all of the metadata.
 * Contains a list of 
 * IdNameMaps that each contain one type of metadata.
 *
 * @author ${author}
 */
@XmlRootElement(name = "idNameMapList")
public class IdNameMapListJpa implements IdNameMapList {


	/** The id name map list. */
	private List<IdNameMap> idNameMapList = new ArrayList<IdNameMap>();


	/**
	 * Instantiates an empty {@link IdNameMapListJpa}.
	 */
	public IdNameMapListJpa() {
		// do nothing
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMapList#getIdNameMapList()
	 */
	@Override
	@XmlElement(type=IdNameMapJpa.class, name = "idNameMaps")
	public List<IdNameMap> getIdNameMapList() {
		return idNameMapList;
	}

  
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMapList#setIdNameMapList(java.util.List)
	 */
	@Override
	public void setIdNameMapList(List<IdNameMap> idNameMapList) {
		this.idNameMapList = idNameMapList;
	}
	

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMapList#getCount()
	 */
	@Override
	@XmlElement(name = "count")
	public int getCount() {
		return idNameMapList.size();
	}


	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMapList#addIdNameMap(org.ihtsdo.otf.mapping.services.IdNameMap)
	 */
	@Override
	public void addIdNameMap(IdNameMap idNameMap) {
		idNameMapList.add(idNameMap);
		
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMapList#removeIdNameMap(org.ihtsdo.otf.mapping.services.IdNameMap)
	 */
	@Override
	public void removeIdNameMap(IdNameMap idNameMap) {
		idNameMapList.remove(idNameMap);
		
	}
}
