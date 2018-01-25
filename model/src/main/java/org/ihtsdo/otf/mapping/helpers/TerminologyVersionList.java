package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

/**
 * Container for terminology versions stored on AWS.
 *
 * @author ${author}
 */
public class TerminologyVersionList {

	/** The entries. */
	private List<TerminologyVersion> terminologyVersionList = new ArrayList<>();

	/**
	 * Instantiates an empty {@link TerminologyVersionList}.
	 */
	public TerminologyVersionList() {
		// do nothing
	}

	/**
	 * Returns the key value pair list.
	 * 
	 * @return the key value pair list
	 */
	@XmlElement(name = "TerminologyVersion")
	public List<TerminologyVersion> getTerminologyVersionList() {
		return terminologyVersionList;
	}

	/**
	 * Sets the key value pair list.
	 * 
	 * @param TerminologyVersionList
	 *            the key value pair list
	 */
	public void setTerminologyVersionList(List<TerminologyVersion> TerminologyVersionList) {
		this.terminologyVersionList = TerminologyVersionList;
	}

	/**
	 * Adds the key value pair.
	 * 
	 * @param TerminologyVersion
	 *            the key value pair
	 */
	public void addTerminologyVersion(TerminologyVersion TerminologyVersion) {
		terminologyVersionList.add(TerminologyVersion);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((terminologyVersionList == null) ? 0 : terminologyVersionList.hashCode());
		return result;
	}

	/**
	 * Removes all duplicated Terminology Versions.
	 * Currently, script is only programmatically excluding those with "_INT_" when others are found
	 * Note: All other duplicates will be added to return list.
	 */
	public Map<String, Set<TerminologyVersion>> removeDups() {
		List<TerminologyVersion> tmpList = new ArrayList<>();
		Map<String, TerminologyVersion> singleMap = new HashMap<>();
		Map<String, Set<TerminologyVersion>>  dupMap = new HashMap<>();

		// Identify Dups
		for (TerminologyVersion tv : terminologyVersionList) {
			String triplet;
			if (tv.getScope() != null && !tv.getScope().isEmpty()) {
				triplet = tv.getTerminology().toLowerCase() + "-" + tv.getVersion().toLowerCase() + "-"
						+ tv.getScope();
			} else {
				triplet = tv.getTerminology().toLowerCase() + "-" + tv.getVersion().toLowerCase();
			}
			
			if (singleMap.containsKey(triplet)) {
				// Add current triplet and triplet listed in singleList version to dupList
				if (!dupMap.containsKey(triplet)) {
					dupMap.put(triplet, new HashSet<TerminologyVersion>());
				}
				
				dupMap.get(triplet).add(tv);
				dupMap.get(triplet).add(singleMap.get(triplet));
			} else {
				singleMap.put(triplet, tv);
			}
		}
			
		// Add those not found to have dups
		for (String triplet : singleMap.keySet()) {
			if (!dupMap.containsKey(triplet)) {
				tmpList.add(singleMap.get(triplet));
			}
		}

		// Exclude those with "_INT_" and add remaining pairs/triplets to list
		for (String triplet : dupMap.keySet()) {
			for (TerminologyVersion tv : dupMap.get(triplet)) {
				if (!tv.getAwsZipFileName().toLowerCase().contains("_int_")) {
					tmpList.add(tv);
					break;
				}
			}
		}

		terminologyVersionList.clear();
		terminologyVersionList.addAll(tmpList);
		
		return dupMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		TerminologyVersionList other = (TerminologyVersionList) obj;
		if (terminologyVersionList == null) {
			if (other.terminologyVersionList != null)
				return false;
		} else if (!terminologyVersionList.equals(other.terminologyVersionList))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer listString = new StringBuffer();

		int counter = 0;
		for (TerminologyVersion tv : getTerminologyVersionList()) {
			listString.append("\t#" + ++counter + " ");
			listString.append(tv.toString() + System.getProperty("line.separator"));
		}

		return "TerminologyVersionList [TerminologyVersionList=" + System.getProperty("line.separator")
				+ listString.toString() + "]";
	}
}
