package org.ihtsdo.otf.mapping.workflow;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.helpers.WorkflowPath;

/**
 * Default implementation of {@link TrackingRecordJpa}.
 */
@Entity
@Indexed
@Table(name = "tracking_records", uniqueConstraints = @UniqueConstraint(columnNames = { "terminologyId", "terminology",
		"terminologyVersion", "mapProjectId" }) )
public class TrackingRecordJpa implements TrackingRecord {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;

	/** The map project. */
	@Column(nullable = false)
	private Long mapProjectId;

	/** The terminology. */
	@Column(nullable = false)
	private String terminology;

	/** The terminology id. */
	@Column(nullable = false)
	private String terminologyId;

	/** The terminology version. */
	@Column(nullable = false)
	private String terminologyVersion;

	/** The default preferred name. */
	@Column(nullable = false)
	private String defaultPreferredName;

	/** The sort key. */
	@Column(nullable = false)
	private String sortKey;

	/** The workflow path. */
	@Enumerated(EnumType.STRING)
	private WorkflowPath workflowPath;

	/**
	 * The workflow status. Pairs are constructed in the format
	 * workflowStatus_userName e.g. NEW_dmo, EDITING_DONE_kli
	 */
	@Column(nullable = true)
	private String userAndWorkflowStatusPairs;

	/** The map record ids. */
	@ElementCollection
	@CollectionTable(name = "tracking_records_map_records", joinColumns = @JoinColumn(name = "id") )
	@Column(nullable = true)
	private Set<Long> mapRecordIds = new HashSet<>();

	/** The assigned user names. */
	@Column(nullable = true)
	private String assignedUserNames = null;

	/** The assigned user count. */
	@Column(nullable = false)
	private int assignedUserCount = 0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long getId() {
		return this.id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#getMapProjectId()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public Long getMapProjectId() {
		return this.mapProjectId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.workflow.TrackingRecord#setMapProjectId(java.lang
	 * .Long)
	 */
	@Override
	public void setMapProjectId(Long mapProjectId) {
		this.mapProjectId = mapProjectId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTerminologyVersion() {
		return terminologyVersion;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTerminologyVersion(String terminologyVersion) {
		this.terminologyVersion = terminologyVersion;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTerminology() {
		return terminology;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTerminology(String terminology) {
		this.terminology = terminology;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public String getTerminologyId() {
		return terminologyId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setTerminologyId(String terminologyId) {
		this.terminologyId = terminologyId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.workflow.TrackingRecord#setDefaultPreferredName
	 * (java.lang.String)
	 */
	@Override
	public void setDefaultPreferredName(String defaultPreferredName) {
		this.defaultPreferredName = defaultPreferredName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.workflow.TrackingRecord#getDefaultPreferredName ()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	@Analyzer(definition = "noStopWord")
	public String getDefaultPreferredName() {
		return defaultPreferredName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#setSortKey(java.
	 * lang.String)
	 */
	@Override
	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#getSortKey()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public String getSortKey() {
		return sortKey;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#getWorkflowPath()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
	public WorkflowPath getWorkflowPath() {
		return workflowPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.workflow.TrackingRecord#setWorkflowPath(org.ihtsdo
	 * .otf.mapping.helpers.WorkflowPath)
	 */
	@Override
	public void setWorkflowPath(WorkflowPath workflowPath) {
		this.workflowPath = workflowPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#
	 * getUserAndWorkflowStatusPairs ()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO, analyzer = @Analyzer(impl = WhitespaceAnalyzer.class) )
	public String getUserAndWorkflowStatusPairs() {
		return userAndWorkflowStatusPairs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#
	 * setUserAndWorkflowStatusPairs (java.lang.String)
	 */
	@Override
	public void setUserAndWorkflowStatusPairs(String userAndWorkflowStatusPairs) {
		this.userAndWorkflowStatusPairs = userAndWorkflowStatusPairs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#
	 * addUserAndWorkflowStatusPair (java.lang.String, java.lang.String)
	 */
	@Override
	public void addUserAndWorkflowStatusPair(String userName, String workflowStatus) {
		String pair = workflowStatus + "_" + userName;
		if (this.userAndWorkflowStatusPairs == null)
			this.userAndWorkflowStatusPairs = pair;
		if (this.userAndWorkflowStatusPairs.indexOf(pair) == -1) {
			this.userAndWorkflowStatusPairs += " " + pair;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#
	 * removeUserAndWorkflowStatusPair (java.lang.String, java.lang.String)
	 */
	@Override
	public void removeUserAndWorkflowStatusPair(String userName, String workflowStatus) {
		String pair = workflowStatus + "_" + userName;

		if (this.userAndWorkflowStatusPairs.indexOf(pair) != -1) {
			userAndWorkflowStatusPairs = userAndWorkflowStatusPairs.replaceAll(pair, "").replace("  ", " ").trim();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#getMapRecordIds()
	 */
	@Override
	public Set<Long> getMapRecordIds() {
		return mapRecordIds;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#setMapRecordIds(java
	 * .util.Set)
	 */
	@Override
	public void setMapRecordIds(Set<Long> mapRecordIds) {
		this.mapRecordIds = mapRecordIds;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.workflow.TrackingRecord#getAssignedUserNames()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	public String getAssignedUserNames() {
		return assignedUserNames;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.workflow.TrackingRecord#getAssignedUserCount()
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.YES, store = Store.NO)
	public int getAssignedUserCount() {
		if (this.assignedUserNames == null)
			this.assignedUserCount = 0;
		else {
			StringTokenizer st = new StringTokenizer(this.assignedUserNames, " ");
			this.assignedUserCount = st.countTokens();
		}
		return this.assignedUserCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.workflow.TrackingRecord#setAssignedUserNames(java
	 * .util.Set)
	 */
	@Override
	public void setAssignedUserNames(String assignedUserNames) {
		this.assignedUserNames = assignedUserNames;

		// call the count function
		this.getAssignedUserCount();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.workflow.TrackingRecord#addAssignedUserName(java
	 * .lang.String)
	 */
	@Override
	public void addAssignedUserName(String name) {
		// if string list is null, set it to name
		if (this.assignedUserNames == null)
			this.assignedUserNames = name;

		// otherwise, add if not already present
		else if (this.assignedUserNames.indexOf(name) == -1)
			this.assignedUserNames += " " + name;

		// call the count function
		this.getAssignedUserCount();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.workflow.TrackingRecord#removeAssignedUserName(
	 * java .lang.String)
	 */
	@Override
	public void removeAssignedUserName(String name) {
		if (this.assignedUserNames.indexOf(name) != -1) {

			// remove the name, tighten any double spaces remaining, and trim
			// the
			// string
			this.assignedUserNames = this.assignedUserNames.replace(name, "").replace("  ", " ").trim();

			// call the count function
			this.getAssignedUserCount();

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#addMapRecord(org
	 * .ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public void addMapRecordId(Long mapRecordId) {
		if (this.mapRecordIds == null)
			this.mapRecordIds = new HashSet<>();
		this.mapRecordIds.add(mapRecordId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.workflow.TrackingRecord#removeMapRecord(
	 * org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public void removeMapRecordId(Long mapRecordId) {
		if (this.mapRecordIds != null) {
			this.mapRecordIds.remove(mapRecordId);
		}
	}

	@Override
	public String toString() {
		return "TrackingRecordJpa [id=" + id + ", mapProjectId=" + mapProjectId + ", terminology=" + terminology
				+ ", terminologyId=" + terminologyId + ", terminologyVersion=" + terminologyVersion
				+ ", defaultPreferredName=" + defaultPreferredName + ", sortKey=" + sortKey + ", workflowPath="
				+ workflowPath + ", userAndWorkflowStatusPairs=" + userAndWorkflowStatusPairs + ", mapRecordIds="
				+ mapRecordIds + ", assignedUserNames=" + assignedUserNames + ", assignedUserCount=" + assignedUserCount
				+ "]";
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
		result = prime * result + assignedUserCount;
		result = prime * result + ((assignedUserNames == null) ? 0 : assignedUserNames.hashCode());
		result = prime * result + ((defaultPreferredName == null) ? 0 : defaultPreferredName.hashCode());
		result = prime * result + ((mapProjectId == null) ? 0 : mapProjectId.hashCode());
		result = prime * result + ((mapRecordIds == null) ? 0 : mapRecordIds.hashCode());
		result = prime * result + ((sortKey == null) ? 0 : sortKey.hashCode());
		result = prime * result + ((terminology == null) ? 0 : terminology.hashCode());
		result = prime * result + ((terminologyId == null) ? 0 : terminologyId.hashCode());
		result = prime * result + ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
		result = prime * result + ((userAndWorkflowStatusPairs == null) ? 0 : userAndWorkflowStatusPairs.hashCode());
		result = prime * result + ((workflowPath == null) ? 0 : workflowPath.hashCode());
		return result;
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
		TrackingRecordJpa other = (TrackingRecordJpa) obj;
		if (assignedUserCount != other.assignedUserCount)
			return false;
		if (assignedUserNames == null) {
			if (other.assignedUserNames != null)
				return false;
		} else if (!assignedUserNames.equals(other.assignedUserNames))
			return false;
		if (defaultPreferredName == null) {
			if (other.defaultPreferredName != null)
				return false;
		} else if (!defaultPreferredName.equals(other.defaultPreferredName))
			return false;
		if (mapProjectId == null) {
			if (other.mapProjectId != null)
				return false;
		} else if (!mapProjectId.equals(other.mapProjectId))
			return false;
		if (mapRecordIds == null) {
			if (other.mapRecordIds != null)
				return false;
		} else if (!mapRecordIds.equals(other.mapRecordIds))
			return false;
		if (sortKey == null) {
			if (other.sortKey != null)
				return false;
		} else if (!sortKey.equals(other.sortKey))
			return false;
		if (terminology == null) {
			if (other.terminology != null)
				return false;
		} else if (!terminology.equals(other.terminology))
			return false;
		if (terminologyId == null) {
			if (other.terminologyId != null)
				return false;
		} else if (!terminologyId.equals(other.terminologyId))
			return false;
		if (terminologyVersion == null) {
			if (other.terminologyVersion != null)
				return false;
		} else if (!terminologyVersion.equals(other.terminologyVersion))
			return false;
		if (userAndWorkflowStatusPairs == null) {
			if (other.userAndWorkflowStatusPairs != null)
				return false;
		} else if (!userAndWorkflowStatusPairs.equals(other.userAndWorkflowStatusPairs))
			return false;
		if (workflowPath != other.workflowPath)
			return false;
		return true;
	}

}
