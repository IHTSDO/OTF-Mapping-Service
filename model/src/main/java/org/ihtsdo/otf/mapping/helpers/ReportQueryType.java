package org.ihtsdo.otf.mapping.helpers;

/**
 * Enumerated types of queries for report.
 * Determines type of handler to be used.
 */
public enum ReportQueryType {

	/**  lucene type. */
	LUCENE,
	
	/**  HQL type. */
	HQL,
	
	/**  SQL type. */
	SQL
}
