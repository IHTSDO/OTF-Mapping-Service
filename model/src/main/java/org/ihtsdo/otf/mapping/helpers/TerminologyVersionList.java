package org.ihtsdo.otf.mapping.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
	 * Removes the dup versions.
	 */
	public void removeDupVersions() {
		List<TerminologyVersion> tmpList = new ArrayList<>();
		Set<String> observedVersions = new HashSet<>();

		// TODO: Other restrictions required to define approach?
		for (TerminologyVersion tv : terminologyVersionList) {
			if (!observedVersions.contains(tv.getVersion())) {
				observedVersions.add(tv.getVersion());
				tmpList.add(new TerminologyVersion(tv.getTerminology(), tv.getVersion(), tv.getScope(),
						tv.getAwsZipFileName()));
			}
		}

		terminologyVersionList.clear();
		terminologyVersionList.addAll(tmpList);
	}

	/**
	 * Removes all duplicated Terminology Versions.
	 */
	public void removeDups() {
		List<TerminologyVersion> tmpList = new ArrayList<>();
		List<String> observedTriplets = new ArrayList<>();
		List<TerminologyVersion> notAddedTriplets = new ArrayList<>();

		// TODO: Other restrictions required to define approach?
		for (TerminologyVersion tv : terminologyVersionList) {
			String triplet = tv.getTerminology().toLowerCase() + "-" + tv.getVersion().toLowerCase() + "-"
					+ tv.getScope();

			if (!observedTriplets.contains(triplet)) {
				observedTriplets.add(triplet);
				tmpList.add(new TerminologyVersion(tv.getTerminology(), tv.getVersion(), tv.getScope(),
						tv.getAwsZipFileName()));
			} else {
				notAddedTriplets.add(new TerminologyVersion(tv.getTerminology(), tv.getVersion(), tv.getScope(),
						tv.getAwsZipFileName()));
			}
		}

		terminologyVersionList.clear();
		terminologyVersionList.addAll(tmpList);
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
