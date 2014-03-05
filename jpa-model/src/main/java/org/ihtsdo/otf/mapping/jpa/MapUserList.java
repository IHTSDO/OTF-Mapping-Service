package org.ihtsdo.otf.mapping.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.ihtsdo.otf.mapping.model.MapUser;

/**
 * Container for map entrys.
 */
@XmlRootElement(name = "mapUserList")
public class MapUserList {

	/** The map entrys. */
	private List<MapUser> mapUsers = new ArrayList<MapUser>();

	/**
	 * Instantiates a new map entry list.
	 */
	public MapUserList() {
		// do nothing
	}

	/**
	 * Adds the map entry
	 * 
	 * @param MapUser
	 *            the map entry
	 */
	public void addMapUser(MapUser MapUser) {
		mapUsers.add(MapUser);
	}

	/**
	 * Removes the map entry.
	 * 
	 * @param MapUser
	 *            the map entry
	 */
	public void removeMapUser(MapUser MapUser) {
		mapUsers.remove(MapUser);
	}

	/**
	 * Sets the map entrys.
	 * 
	 * @param mapUsers
	 *            the new map entrys
	 */
	public void setMapUsers(List<MapUser> mapUsers) {
		this.mapUsers = new ArrayList<MapUser>();
		this.mapUsers.addAll(mapUsers);
		
		
	}
	
	/**
	 * Sorts the map entrys alphabetically by name
	 */
	public void sortMapUsers() {
	
		Collections.sort(this.mapUsers,
			new Comparator<MapUser>() {
				@Override
				public int compare(MapUser o1, MapUser o2) {
					return o1.getId().compareTo(o2.getId());
				}

			});
}

	/**
	 * Gets the map entrys.
	 * 
	 * @return the map entrys
	 */
	@XmlElement(type=MapUserJpa.class, name="mapUser")
	public List<MapUser> getMapUsers() {
		return mapUsers;
	}
	
	/**
	 * Return the count as an xml element
	 * @return the number of objects in the list
	 */
	@XmlElement(name = "count")
	public int getCount() {
		return mapUsers.size();
	}

}
