package org.ihtsdo.otf.mapping.workflow;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.model.MapProject;

/**
 * Reference implementation of {@link Workflow}.
 */
@Entity
@Table(name="workflow")
@Indexed
public class WorkflowJpa implements Workflow {
	
	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
  /**  The map project. */
  @OneToOne(targetEntity = MapProjectJpa.class, fetch = FetchType.LAZY)
	private MapProject mapProject;
	
	/**  The tracking records for unmapped in scope concepts. */
	@OneToMany(targetEntity = WorkflowTrackingRecordJpa.class)
	@IndexedEmbedded(targetElement = WorkflowTrackingRecordJpa.class)
	private Set<WorkflowTrackingRecord> trackingRecords = new HashSet<>();
	
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
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#getMapProject()
	 */
	@Override
	public MapProject getMapProject() {
		return mapProject;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#setMapProject(org.ihtsdo.otf.mapping.model.MapProject)
	 */
	@Override
	public void setMapProject(MapProject mapProject) {
		this.mapProject = mapProject;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#getTrackingRecordsForConflictConcepts()
	 */
	@Override
	public Set<WorkflowTrackingRecord> getTrackingRecordsForConflictConcepts() {
		Set<WorkflowTrackingRecord> conflictRecords = new HashSet<>();
		for (WorkflowTrackingRecord trackingRecord : trackingRecords) {
			if (trackingRecord.isHasDiscrepancy())
				conflictRecords.add(trackingRecord);
		}
		return conflictRecords;
	}




	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "WorkflowJpa [id=" + id + ", mapProject=" + mapProject
				+ ", trackingRecords="
				+ trackingRecords + "]";
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#getTrackingRecordsForUnmappedInScopeConcepts()
	 */
	@Override
	public Set<WorkflowTrackingRecord> getTrackingRecordsForUnmappedInScopeConcepts() {
		Set<WorkflowTrackingRecord> unmappedTrackingRecords = new HashSet<>();
		for (WorkflowTrackingRecord trackingRecord : trackingRecords) {
			if (!trackingRecord.isHasDiscrepancy())
				unmappedTrackingRecords.add(trackingRecord);
		}
		return unmappedTrackingRecords;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#setTrackingRecords(java.util.Set)
	 */
	@Override
	public void setTrackingRecords(Set<WorkflowTrackingRecord> trackingRecords) {
		this.trackingRecords = trackingRecords;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#addTrackingRecord(org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord)
	 */
	@Override
	public void addTrackingRecord(WorkflowTrackingRecord trackingRecord) {
		if (trackingRecords == null)
			trackingRecords = new HashSet<>();
		trackingRecords.add(trackingRecord);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#removeTrackingRecord(org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord)
	 */
	@Override
	public void removeTrackingRecord(WorkflowTrackingRecord trackingRecord) {
		trackingRecords.remove(trackingRecord);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#getTrackingRecords()
	 */
	@Override
	public Set<WorkflowTrackingRecord> getTrackingRecords() {
		return trackingRecords;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result =
				prime * result + ((mapProject == null) ? 0 : mapProject.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		WorkflowJpa other = (WorkflowJpa) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (mapProject == null) {
			if (other.mapProject != null)
				return false;
		} else if (!mapProject.equals(other.mapProject))
			return false;
    return true;
  }
}
