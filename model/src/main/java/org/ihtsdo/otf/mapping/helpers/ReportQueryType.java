package org.ihtsdo.otf.mapping.helpers;

/**
 * Enum of query types for report. Determines type of handler to be used. NONE
 * is used for "diff" reports.
 */
public enum ReportQueryType {

  /** lucene type. */
  LUCENE,

  /** HQL type. */
  HQL,

  /** SQL type. */
  SQL,

  /** No query used, no query type */
  NONE
}
