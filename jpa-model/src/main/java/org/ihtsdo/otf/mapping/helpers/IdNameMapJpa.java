package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.helpers.IdNameMap;


/**
 * Container for id name pairs (id, name metadata tuples).
 *
 * @author ${author}
 */
@XmlRootElement(name = "idNameMap")
public class IdNameMapJpa implements IdNameMap {


	/** The entries. */
	private List<IdNamePair> entries = new ArrayList<IdNamePair>();
	
	/** The name. */
	private String name = "";

	/**
	 * Instantiates a new id name map.
	 */
	public IdNameMapJpa() {
		// do nothing
	}

	/**
	 * Gets the List of IdNamePairs.
	 * 
	 * @return the id name pair list
	 */
	@Override
	@XmlElement(type=IdNamePairJpa.class, name = "idNamePair")
	public List<IdNamePair> getIdNamePairList() {
		return entries;
	}

	/**
	 * Sets the IdNameMap.
	 * 
	 * @param idNamePairList
	 *            the new id name pair list
	 */
	@Override
	public void setIdNamePairList(List<IdNamePair> idNamePairList) {
		this.entries = idNamePairList;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMap#addIdNameMapEntry(java.lang.Long, java.lang.String)
	 */
	@Override
	public void addIdNamePair(IdNamePair idNamePair) {
		entries.add(idNamePair);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMap#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;	
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.services.IdNameMap#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entries == null) ? 0 : entries.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IdNameMapJpa other = (IdNameMapJpa) obj;
		if (entries == null) {
			if (other.entries != null)
				return false;
		} else if (!entries.equals(other.entries))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "IdNameMapJpa [entries=" + entries + ", name=" + name + "]";
	}


}
