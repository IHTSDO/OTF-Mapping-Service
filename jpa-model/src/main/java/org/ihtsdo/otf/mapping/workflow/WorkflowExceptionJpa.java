package org.ihtsdo.otf.mapping.workflow;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

// TODO: Auto-generated Javadoc
/**
 * The Class WorkflowExceptionJpa.
 */
@Entity
@Table(name = "workflow_exceptions", uniqueConstraints = @UniqueConstraint(columnNames = {
		"terminologyId", "terminology", "terminologyVersion", "mapProjectId"
}))
public class WorkflowExceptionJpa implements WorkflowException {
	
	/** The id. */
	@Id
	@GeneratedValue
	private Long id;
	
	/** The map project id. */
	@Column(nullable = false)
	private Long mapProjectId;
	
	/** The terminology id. */
	@Column(nullable = false)
	private String terminologyId;
	
	/** The terminology. */
	@Column(nullable = false)
	private String terminology;
	
	/** The terminology version. */
	@Column(nullable = false)
	private String terminologyVersion;
	
	/** The false conflict map record ids. */
	@ElementCollection
	@CollectionTable(name = "workflow_exception_false_conflicts", joinColumns = @JoinColumn(name = "id"))
	@Column(nullable = true)
	private Set<Long> falseConflictMapRecordIds = new HashSet<>();

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#getId()
	 */
	@Override
	public Long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#setId(java.lang.Long)
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}
	
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#getMapProjectId()
	 */
	@Override
	public Long getMapProjectId() {
		return mapProjectId;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#setMapProjectId(java.lang.Long)
	 */
	@Override
	public void setMapProjectId(Long mapProjectId) {
		this.mapProjectId = mapProjectId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#getTerminologyId()
	 */
	@Override
	public String getTerminologyId() {
		return terminologyId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#setTerminologyId(java.lang.String)
	 */
	@Override
	public void setTerminologyId(String terminologyId) {
		this.terminologyId = terminologyId;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#getTerminology()
	 */
	@Override
	public String getTerminology() {
		return terminology;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#setTerminology(java.lang.String)
	 */
	@Override
	public void setTerminology(String terminology) {
		this.terminology = terminology;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#getTerminologyVersion()
	 */
	@Override
	public String getTerminologyVersion() {
		return terminologyVersion;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#setTerminologyVersion(java.lang.Long)
	 */
	@Override
	public void setTerminologyVersion(String terminologyVersion) {
		this.terminologyVersion = terminologyVersion;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#getFalseConflictMapRecordIds()
	 */
	@Override
	public Set<Long> getFalseConflictMapRecordIds() {
		return falseConflictMapRecordIds;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#setFalseConflictMapRecordIds(java.util.Set)
	 */
	@Override
	public void setFalseConflictMapRecordIds(Set<Long> falseConflictMapRecordIds) {
		if (falseConflictMapRecordIds == null) this.falseConflictMapRecordIds = new HashSet<>();
		else this.falseConflictMapRecordIds = falseConflictMapRecordIds;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#addFalseConflictMapRecordId(java.lang.Long)
	 */
	@Override
	public void addFalseConflictMapRecordId(Long id) {
		this.falseConflictMapRecordIds.add(id);
		
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.workflow.WorkflowException#removeFalseConflictMapRecordId(java.lang.Long)
	 */
	@Override
	public void removeFalseConflictMapRecordId(Long id) {
		this.falseConflictMapRecordIds.remove(id);
	}
	
	/**
	 * Checks if is empty.
	 *
	 * @return true, if is empty
	 */
	@Override
	public boolean isEmpty() {
		if (this.falseConflictMapRecordIds.size() == 0) return true;
		else return false;
	}
	
	
	
}
