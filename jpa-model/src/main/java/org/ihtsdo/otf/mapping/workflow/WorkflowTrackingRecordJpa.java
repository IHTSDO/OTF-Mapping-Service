package org.ihtsdo.otf.mapping.workflow;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Store;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapUser;

/**
 * Default implementatino of {@link WorkflowTrackingRecordJpa}.
 */
@Entity
@Table(name = "workflow_tracking_records")
@Indexed
public class WorkflowTrackingRecordJpa implements WorkflowTrackingRecord {

	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	/** The terminology. */
	@Column(nullable = false)
	private String terminology;

	/**  The terminology id. */
	@Column(nullable = false)
	private String terminologyId;

	/** The terminology version. */
	@Column(nullable = false)
	private String terminologyVersion;
	
	/**  The default preferred name. */
	@Column(nullable = false)
	private String defaultPreferredName;
	
	/**  The has discrepancy. */
	@Column(unique = false, nullable = false)
	private boolean hasDiscrepancy = false;

	/**  The sort key. */
	@Column(nullable = false)
	private String sortKey;
	
	/**  The map records. */
	@OneToMany(targetEntity = MapRecordJpa.class)
	@IndexedEmbedded(targetElement = MapRecordJpa.class)
	private Set<MapRecord> mapRecords = new HashSet<>();

	
	/**  The assigned users. */
	@ManyToMany(targetEntity=MapUserJpa.class, fetch=FetchType.EAGER)
	@IndexedEmbedded(targetElement=MapUserJpa.class)
	private Set<MapUser> assignedUsers = new HashSet<>();
	
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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
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

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#setDefaultPreferredName(java.lang.String)
	 */
	@Override
	public void setDefaultPreferredName(String defaultPreferredName) {
		this.defaultPreferredName = defaultPreferredName;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#getDefaultPreferredName()
	 */
	@Override
	public String getDefaultPreferredName() {
		return defaultPreferredName;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#setHasDiscrepancy(boolean)
	 */
	@Override
	public void setHasDiscrepancy(boolean hasDiscrepancy) {
		this.hasDiscrepancy = hasDiscrepancy;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#isHasDiscrepancy()
	 */
	@Override
	public boolean isHasDiscrepancy() {
		return hasDiscrepancy;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#setSortKey(java.lang.String)
	 */
	@Override
	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#getSortKey()
	 */
	@Override
	public String getSortKey() {
		return sortKey;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#getAssignedUsers()
	 */
	@Override
	public Set<MapUser> getAssignedUsers() {
		return assignedUsers;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#setAssignedUsers(java.util.Set)
	 */
	@Override
	public void setAssignedUsers(Set<MapUser> assignedUsers) {
		this.assignedUsers = assignedUsers;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#addAssignedSpecialist(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void addAssignedUser(MapUser assignedUser) {
		this.assignedUsers.add(assignedUser);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#removeAssignedSpecialist(org.ihtsdo.otf.mapping.model.MapUser)
	 */
	@Override
	public void removeAssignedUser(MapUser assignedUser) {
		this.assignedUsers.remove(assignedUser);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#getMapRecords()
	 */
	@Override
	public Set<MapRecord> getMapRecords() {
		return mapRecords;
	}
	

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#setMapRecords(java.util.Set)
	 */
	@Override
	public void setMapRecords(Set<MapRecord> mapRecords) {
		this.mapRecords = mapRecords;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#addMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public void addMapRecord(MapRecord mapRecord) {
		this.mapRecords.add(mapRecord);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord#removeMapRecord(org.ihtsdo.otf.mapping.model.MapRecord)
	 */
	@Override
	public void removeMapRecord(MapRecord mapRecord) {
		this.mapRecords.remove(mapRecord);
	}

	@Override
	public String toString() {
		return "WorkflowTrackingRecordJpa [id=" + id + ", terminology="
				+ terminology + ", terminologyId=" + terminologyId
				+ ", terminologyVersion=" + terminologyVersion
				+ ", defaultPreferredName=" + defaultPreferredName
				+ ", hasDiscrepancy=" + hasDiscrepancy + ", sortKey=" + sortKey
				+ ", mapRecords=" + mapRecords + ", assignedUsers="
				+ assignedUsers + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result =
				prime
						* result
						+ ((assignedUsers == null) ? 0 : assignedUsers
								.hashCode());
		result =
				prime
						* result
						+ ((defaultPreferredName == null) ? 0 : defaultPreferredName
								.hashCode());
		result = prime * result + (hasDiscrepancy ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result =
				prime * result + ((mapRecords == null) ? 0 : mapRecords.hashCode());
		result = prime * result + ((sortKey == null) ? 0 : sortKey.hashCode());
		result =
				prime * result + ((terminology == null) ? 0 : terminology.hashCode());
		result =
				prime * result
						+ ((terminologyId == null) ? 0 : terminologyId.hashCode());
		result =
				prime
						* result
						+ ((terminologyVersion == null) ? 0 : terminologyVersion.hashCode());
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
		WorkflowTrackingRecordJpa other = (WorkflowTrackingRecordJpa) obj;
		if (assignedUsers == null) {
			if (other.assignedUsers != null)
				return false;
		} else if (!assignedUsers.equals(other.assignedUsers))
			return false;
		if (defaultPreferredName == null) {
			if (other.defaultPreferredName != null)
				return false;
		} else if (!defaultPreferredName.equals(other.defaultPreferredName))
			return false;
		if (hasDiscrepancy != other.hasDiscrepancy)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (mapRecords == null) {
			if (other.mapRecords != null)
				return false;
		} else if (!mapRecords.equals(other.mapRecords))
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
		return true;
	}


}
