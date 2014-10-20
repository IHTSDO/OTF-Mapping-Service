package org.ihtsdo.otf.mapping.reports;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.ReportQueryType;
import org.ihtsdo.otf.mapping.helpers.ReportResultType;
import org.ihtsdo.otf.mapping.helpers.ReportTimePeriod;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// TODO: Auto-generated Javadoc
/**
 * The Class ReportDefinitionJpa.
 */
@Entity
@Audited
@Table(name = "report_definitions", uniqueConstraints = {
		@UniqueConstraint(columnNames = {
				"name"
		})
})
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "reportDefinition")
public class ReportDefinitionJpa implements ReportDefinition {

	/** Auto-generated id. */
	@Id
	@GeneratedValue
	private Long id;

	/** The report type name. */
	@Column(nullable = false)
	private String name;
	
	/** The is diff report. */
	@Column(nullable = false)
	private boolean isDiffReport = false;
	
	/** The is rate report. */
	@Column(nullable = false)
	private boolean isRateReport = false;
	
	/** The time period (in days) for diff and rate reports */
	@Enumerated(EnumType.STRING)
	private ReportTimePeriod timePeriod;
	
	/** The result type. */
	@Enumerated(EnumType.STRING)
	private ReportResultType resultType;

	/** The query type. */
	@Enumerated(EnumType.STRING)
	private ReportQueryType queryType;

	/** The query. */
	@Column(nullable = false, length = 10000)
	private String query;

	/** The role required. */
	@Enumerated(EnumType.STRING)
	private MapUserRole roleRequired;

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	@Override
	public Long getId() {
		return id;
	}

	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the new id
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Gets the report name.
	 * 
	 * @return the report name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the report name.
	 * 
	 * @param name
	 *            the new report name
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getResultType()
	 */
	/**
	 * Gets the result type.
	 * 
	 * @return the result type
	 */
	@Override
	public ReportResultType getResultType() {
		return resultType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.helpers.ReportDefinition#setResultType(org.ihtsdo
	 * .otf.mapping.helpers.ReportResultType)
	 */
	/**
	 * Sets the result type.
	 * 
	 * @param resultType
	 *            the new result type
	 */
	@Override
	public void setResultType(ReportResultType resultType) {
		this.resultType = resultType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getQueryType()
	 */
	/**
	 * Gets the query type.
	 * 
	 * @return the query type
	 */
	@Override
	public ReportQueryType getQueryType() {
		return queryType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.helpers.ReportDefinition#setQueryType(org.ihtsdo
	 * .otf.mapping.helpers.ReportQueryType)
	 */
	/**
	 * Sets the query type.
	 * 
	 * @param queryType
	 *            the new query type
	 */
	@Override
	public void setQueryType(ReportQueryType queryType) {
		this.queryType = queryType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getQuery()
	 */
	/**
	 * Gets the query.
	 * 
	 * @return the query
	 */
	@Override
	public String getQuery() {
		return query;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ihtsdo.otf.mapping.helpers.ReportDefinition#setQuery(java.lang.String
	 * )
	 */
	/**
	 * Sets the query.
	 * 
	 * @param query
	 *            the new query
	 */
	@Override
	public void setQuery(String query) {
		this.query = query;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ihtsdo.otf.mapping.helpers.ReportDefinition#getRoleRequired()
	 */
	/**
	 * Gets the role required.
	 * 
	 * @return the role required
	 */
	@Override
	public MapUserRole getRoleRequired() {
		return roleRequired;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportDefinition#setRoleRequired(org.ihtsdo.otf.mapping.helpers.MapUserRole)
	 */
	@Override
	public void setRoleRequired(MapUserRole roleRequired) {
		this.roleRequired = roleRequired;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportDefinition#isDiffReport()
	 */
	@Override
	public boolean isDiffReport() {
		return isDiffReport;
	}
	
	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportDefinition#setDiffReport(boolean)
	 */
	@Override
	public void setDiffReport(boolean isDiffReport) {
		this.isDiffReport = isDiffReport;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportDefinition#getTimePeriodInDays()
	 */
	@Override
	public ReportTimePeriod getTimePeriod() {
		return this.timePeriod;
	}

	/* (non-Javadoc)
	 * @see org.ihtsdo.otf.mapping.reports.ReportDefinition#setTimePeriodInDays(int)
	 */
	@Override
	public void setTimePeriod(ReportTimePeriod timePeriod) {
		this.timePeriod = timePeriod;
		
	}

	@Override
	public String toString() {
		return "ReportDefinitionJpa [name=" + name + ", isDiffReport="
				+ isDiffReport + ", isRateReport=" + isRateReport
				+ ", timePeriod=" + timePeriod + ", resultType=" + resultType
				+ ", queryType=" + queryType + ", query=" + query
				+ ", roleRequired=" + roleRequired + "]";
	}

}
