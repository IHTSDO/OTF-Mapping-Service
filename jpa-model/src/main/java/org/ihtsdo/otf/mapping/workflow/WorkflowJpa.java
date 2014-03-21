package org.ihtsdo.otf.mapping.workflow;

import java.util.Set;

import javax.persistence.CascadeType;
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
 * The Class WorkflowJpa.
 *
 * @author ${author}
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
  @OneToOne(targetEntity = MapProjectJpa.class)
	private MapProject mapProject;
	
	/**  The tracking records for unmapped in scope concepts. */
	@OneToMany(targetEntity = WorkflowTrackingRecordJpa.class)
	@IndexedEmbedded(targetElement = WorkflowTrackingRecordJpa.class)
	private Set<WorkflowTrackingRecord> trackingRecordsForUnmappedInScopeConcepts;
	
	/**  The tracking records for conflict concepts. */
	@OneToMany(targetEntity = WorkflowTrackingRecordJpa.class)
	@IndexedEmbedded(targetElement = WorkflowTrackingRecordJpa.class)	
	private Set<WorkflowTrackingRecord> trackingRecordsForConflictConcepts;
	
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
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#getTrackingRecordsForUnmappedInScopeConcepts()
	 */
	@Override
	public Set<WorkflowTrackingRecord> getTrackingRecordsForUnmappedInScopeConcepts() {
		return trackingRecordsForUnmappedInScopeConcepts;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#setTrackingRecordsForUnmappedInScopeConcepts(java.util.Set)
	 */
	@Override
	public void setTrackingRecordsForUnmappedInScopeConcepts(
		Set<WorkflowTrackingRecord> trackingRecordsForUnmappedInScopeConcepts) {
		this.trackingRecordsForUnmappedInScopeConcepts = trackingRecordsForUnmappedInScopeConcepts;
		
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#addTrackingRecordsForUnmappedInScopeConcept(org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord)
	 */
	@Override
	public void addTrackingRecordsForUnmappedInScopeConcept(
		WorkflowTrackingRecord trackingRecordsForUnmappedInScopeConcept) {
		this.trackingRecordsForUnmappedInScopeConcepts.add(trackingRecordsForUnmappedInScopeConcept);		
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#removeTrackingRecordsForUnmappedInScopeConcept(org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord)
	 */
	@Override
	public void removeTrackingRecordsForUnmappedInScopeConcept(
		WorkflowTrackingRecord trackingRecordsForUnmappedInScopeConcept) {
		this.trackingRecordsForUnmappedInScopeConcepts.remove(trackingRecordsForUnmappedInScopeConcept);	
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#getTrackingRecordsForConflictConcepts()
	 */
	@Override
	public Set<WorkflowTrackingRecord> getTrackingRecordsForConflictConcepts() {
		return trackingRecordsForConflictConcepts;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#setTrackingRecordsForConflictConcepts(java.util.Set)
	 */
	@Override
	public void setTrackingRecordsForConflictConcepts(
		Set<WorkflowTrackingRecord> trackingRecordsForConflictConcepts) {
		this.trackingRecordsForConflictConcepts = trackingRecordsForConflictConcepts;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#addTrackingRecordsForConflictConcepts(org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord)
	 */
	@Override
	public void addTrackingRecordsForConflictConcepts(
		WorkflowTrackingRecord trackingRecordsForConflictConcept) {
		this.trackingRecordsForConflictConcepts.add(trackingRecordsForConflictConcept);
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.Workflow#removeTrackingRecordsForConflictConcepts(org.ihtsdo.otf.mapping.workflow.WorkflowTrackingRecord)
	 */
	@Override
	public void removeTrackingRecordsForConflictConcepts(
		WorkflowTrackingRecord trackingRecordsForConflictConcept) {
		this.trackingRecordsForConflictConcepts.remove(trackingRecordsForConflictConcept);
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
		result =
				prime
						* result
						+ ((trackingRecordsForConflictConcepts == null) ? 0
								: trackingRecordsForConflictConcepts.hashCode());
		result =
				prime
						* result
						+ ((trackingRecordsForUnmappedInScopeConcepts == null) ? 0
								: trackingRecordsForUnmappedInScopeConcepts.hashCode());
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
		if (trackingRecordsForConflictConcepts == null) {
			if (other.trackingRecordsForConflictConcepts != null)
				return false;
		} else if (!trackingRecordsForConflictConcepts
				.equals(other.trackingRecordsForConflictConcepts))
			return false;
		if (trackingRecordsForUnmappedInScopeConcepts == null) {
			if (other.trackingRecordsForUnmappedInScopeConcepts != null)
				return false;
		} else if (!trackingRecordsForUnmappedInScopeConcepts
				.equals(other.trackingRecordsForUnmappedInScopeConcepts))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "WorkflowJpa [id=" + id + ", mapProject=" + mapProject
				+ ", trackingRecordsForUnmappedInScopeConcepts="
				+ trackingRecordsForUnmappedInScopeConcepts
				+ ", trackingRecordsForConflictConcepts="
				+ trackingRecordsForConflictConcepts + "]";
	}

}
