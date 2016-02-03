/**
 * Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.helpers;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.jpa.FeedbackConversationJpa;
import org.ihtsdo.otf.mapping.jpa.FeedbackJpa;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapPrincipleJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapRelationJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.reports.ReportDefinitionJpa;
import org.ihtsdo.otf.mapping.reports.ReportJpa;
import org.ihtsdo.otf.mapping.reports.ReportResultJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.TreePositionJpa;
import org.ihtsdo.otf.mapping.workflow.TrackingRecordJpa;

/**
 * Performs utility functions relating to Lucene indexes and Hibernate Search.
 */
public class IndexUtility {

  /** The sort field analyzed map. */
  public static Map<String, Map<String, Boolean>> sortFieldAnalyzedMap =
      new HashMap<>();

  /** The string field names map. */
  private static Map<Class<?>, Set<String>> stringFieldNames = new HashMap<>();

  /** The field names map. */
  private static Map<Class<?>, Set<String>> allFieldNames = new HashMap<>();

  // Initialize the field names maps
  static {
    try {
      Class<?>[] classes =
          new Class<?>[] {
              ConceptJpa.class, MapProjectJpa.class, MapRecordJpa.class,
              TreePositionJpa.class, TrackingRecordJpa.class,
              FeedbackConversationJpa.class, FeedbackJpa.class,
              ReportJpa.class, MapAdviceJpa.class, MapUserJpa.class,
              MapRelationJpa.class, MapPrincipleJpa.class,
              ReportDefinitionJpa.class, ReportResultJpa.class,
              DescriptionJpa.class, RelationshipJpa.class

          };
      for (Class<?> clazz : classes) {
        stringFieldNames.put(clazz,
            IndexUtility.getIndexedFieldNames(clazz, true));
        allFieldNames.put(clazz,
            IndexUtility.getIndexedFieldNames(clazz, false));
      }
    } catch (Exception e) {
      e.printStackTrace();
      stringFieldNames = null;
    }
  }

  /**
   * Returns the indexed field names for a given class.
   *
   * @param clazz the clazz
   * @param stringOnly the string only flag
   * @return the indexed field names
   * @throws Exception the exception
   */
  public static Set<String> getIndexedFieldNames(Class<?> clazz,
    boolean stringOnly) throws Exception {

    // If already initialized, return computed values
    if (stringOnly && stringFieldNames.containsKey(clazz)) {
      return stringFieldNames.get(clazz);
    }
    if (!stringOnly && allFieldNames.containsKey(clazz)) {
      return allFieldNames.get(clazz);
    }

    // Avoid ngram and sort fields (these have special uses)
    Set<String> exclusions = new HashSet<>();
    // exclusions.add("Sort");
    exclusions.add("nGram");
    exclusions.add("NGram");

    // When looking for default fields, exclude definitions and branches
    Set<String> stringExclusions = new HashSet<>();
    stringExclusions.add("definitions");
    stringExclusions.add("branch");

    Set<String> fieldNames = new HashSet<>();

    // first cycle over all methods
    for (Method m : clazz.getMethods()) {

      // if no annotations, skip
      if (m.getAnnotations().length == 0) {
        continue;
      }

      // check for @IndexedEmbedded
      if (m.isAnnotationPresent(IndexedEmbedded.class)) {
        throw new Exception(
            "Unable to handle @IndexedEmbedded on methods, specify on field");
      }

      // determine if there's a fieldBridge (which converts the field)
      boolean hasFieldBridge = false;
      if (m.isAnnotationPresent(Field.class)) {
        if (!m.getAnnotation(Field.class).bridge().impl().toString()
            .equals("void")) {
          hasFieldBridge = true;
        }
      }

      // for non-embedded fields, only process strings
      // This is because we're handling string based query here
      // Other fields can always be used with fielded query clauses
      if (stringOnly && !hasFieldBridge
          && !m.getReturnType().equals(String.class)) {
        continue;
      }

      // check for @Field annotation
      if (m.isAnnotationPresent(Field.class)) {
        String fieldName =
            getFieldNameFromMethod(m, m.getAnnotation(Field.class));
        fieldNames.add(fieldName);
      }

      // check for @Fields annotation
      if (m.isAnnotationPresent(Fields.class)) {
        for (Field field : m.getAnnotation(Fields.class).value()) {
          String fieldName = getFieldNameFromMethod(m, field);

          fieldNames.add(fieldName);
        }
      }
    }

    // second cycle over all fields
    for (java.lang.reflect.Field f : getAllFields(clazz)) {
      // check for @IndexedEmbedded
      if (f.isAnnotationPresent(IndexedEmbedded.class)) {

        // Assumes field is a collection, and has a OneToMany, ManyToMany, or
        // ManyToOne
        // annotation
        Class<?> jpaType = null;
        if (f.isAnnotationPresent(OneToMany.class)) {
          jpaType = f.getAnnotation(OneToMany.class).targetEntity();
        } else if (f.isAnnotationPresent(ManyToMany.class)) {
          jpaType = f.getAnnotation(ManyToMany.class).targetEntity();
        } else if (f.isAnnotationPresent(ManyToOne.class)) {
          jpaType = f.getAnnotation(ManyToOne.class).targetEntity();
        } else if (f.isAnnotationPresent(OneToOne.class)) {
          jpaType = f.getAnnotation(OneToOne.class).targetEntity();
        } else {
          throw new Exception("Unable to determine jpa type, @IndexedEmbedded "
              + "must be used with @OneToMany, @ManyToOne, or @ManyToMany - "
              + clazz + ", " + f.getName());

        }

        for (String embeddedField : getIndexedFieldNames(jpaType, stringOnly)) {
          fieldNames.add(f.getName() + "." + embeddedField);
        }
      }

      // determine if there's a fieldBridge (which converts the field)
      boolean hasFieldBridge = false;
      if (f.isAnnotationPresent(Field.class)) {
        if (f.getAnnotation(Field.class).bridge().impl().toString()
            .equals("void")) {
          hasFieldBridge = true;
        }
      }

      // for non-embedded fields, only process strings
      if (stringOnly && !hasFieldBridge && !f.getType().equals(String.class))
        continue;

      // check for @Field annotation
      if (f.isAnnotationPresent(Field.class)) {
        String fieldName =
            getFieldNameFromField(f, f.getAnnotation(Field.class));
        fieldNames.add(fieldName);
      }

      // check for @Fields annotation
      if (f.isAnnotationPresent(Fields.class)) {
        for (Field field : f.getAnnotation(Fields.class).value()) {
          String fieldName = getFieldNameFromField(f, field);
          fieldNames.add(fieldName);
        }
      }

    }

    // Apply filters
    Set<String> filteredFieldNames = new HashSet<>();
    OUTER: for (String fieldName : fieldNames) {
      for (String exclusion : exclusions) {
        if (fieldName.contains(exclusion)) {
          continue OUTER;
        }
      }
      for (String exclusion : stringExclusions) {
        if (stringOnly && fieldName.contains(exclusion)) {
          continue OUTER;
        }
      }
      filteredFieldNames.add(fieldName);
    }

    return filteredFieldNames;
  }

  /**
   * Helper function to get a field name from a method and annotation.
   *
   * @param m the reflected, annotated method, assumed to be of form
   *          getFieldName()
   * @param annotationField the annotation field
   * @return the indexed field name
   */
  private static String getFieldNameFromMethod(Method m, Field annotationField) {
    // iannotationField annotationFieldield has a speciannotationFieldied name,
    // use that
    if (annotationField.name() != null && !annotationField.name().isEmpty())
      return annotationField.name();

    // otherwise, assume method name of form getannotationFieldName
    // where the desired value is annotationFieldName
    if (m.getName().startsWith("get")) {
      return StringUtils.uncapitalize(m.getName().substring(3));
    } else if (m.getName().startsWith("is")) {
      return StringUtils.uncapitalize(m.getName().substring(2));
    } else
      return m.getName();

  }

  /**
   * Helper function get a field name from reflected Field and annotation.
   *
   * @param annotatedField the reflected, annotated field
   * @param annotationField the field annotation
   * @return the indexed field name
   */
  private static String getFieldNameFromField(
    java.lang.reflect.Field annotatedField, Field annotationField) {
    if (annotationField.name() != null && !annotationField.name().isEmpty()) {
      return annotationField.name();
    }

    return annotatedField.getName();
  }

  /**
   * Returns the all fields.
   *
   * @param type the type
   * @return the all fields
   */
  private static java.lang.reflect.Field[] getAllFields(Class<?> type) {
    if (type.getSuperclass() != null) {
      return ArrayUtils.addAll(getAllFields(type.getSuperclass()),
          type.getDeclaredFields());
    }
    return type.getDeclaredFields();
  }

  /**
   * Returns the name analyzed pairs from annotation.
   *
   * @param clazz the clazz
   * @param sortField the sort field
   * @return the name analyzed pairs from annotation
   * @throws NoSuchMethodException the no such method exception
   * @throws SecurityException the security exception
   */
  public static Map<String, Boolean> getNameAnalyzedPairsFromAnnotation(
    Class<?> clazz, String sortField) throws NoSuchMethodException,
    SecurityException {
    final String key = clazz.getName() + "." + sortField;
    if (sortFieldAnalyzedMap.containsKey(key)) {
      return sortFieldAnalyzedMap.get(key);
    }

    // initialize the name->analyzed pair map
    Map<String, Boolean> nameAnalyzedPairs = new HashMap<>();

    Method m =
        clazz.getMethod("get" + sortField.substring(0, 1).toUpperCase()
            + sortField.substring(1), new Class<?>[] {});

    Set<org.hibernate.search.annotations.Field> annotationFields =
        new HashSet<>();

    // check for Field annotation
    if (m.isAnnotationPresent(org.hibernate.search.annotations.Field.class)) {
      annotationFields.add(m
          .getAnnotation(org.hibernate.search.annotations.Field.class));
    }

    // check for Fields annotation
    if (m.isAnnotationPresent(org.hibernate.search.annotations.Fields.class)) {
      // add all specified fields
      for (org.hibernate.search.annotations.Field f : m.getAnnotation(
          org.hibernate.search.annotations.Fields.class).value()) {
        annotationFields.add(f);
      }
    }

    // cycle over discovered fields and put name and analyze == YES into map
    for (org.hibernate.search.annotations.Field f : annotationFields) {
      nameAnalyzedPairs.put(f.name(), f.analyze().equals(Analyze.YES) ? true
          : false);
    }

    sortFieldAnalyzedMap.put(key, nameAnalyzedPairs);

    return nameAnalyzedPairs;
  }

  /**
   * Apply pfs to lucene query2.
   *
   * @param clazz the clazz
   * @param fieldNamesKey the field names key
   * @param query the query
   * @param pfs the pfs
   * @param manager the manager
   * @return the full text query
   * @throws Exception the exception
   */
  public static FullTextQuery applyPfsToLuceneQuery(Class<?> clazz,
    Class<?> fieldNamesKey, String query, PfsParameter pfs,
    EntityManager manager) throws Exception {

    FullTextQuery fullTextQuery = null;

    // Build up the query
    StringBuilder pfsQuery = new StringBuilder();
    if (query != null)
      pfsQuery.append(query);
    if (pfs != null) {
      if (pfs.getQueryRestriction() != null
          && !pfs.getQueryRestriction().isEmpty()) {
        pfsQuery.append(" AND " + pfs.getQueryRestriction());
      }
    }

    // Set up the "full text query"
    FullTextEntityManager fullTextEntityManager =
        Search.getFullTextEntityManager(manager);
    SearchFactory searchFactory = fullTextEntityManager.getSearchFactory();

    Query luceneQuery;
    QueryParser queryParser =
        new MultiFieldQueryParser(Version.LUCENE_36,
            IndexUtility.getIndexedFieldNames(fieldNamesKey, true).toArray(
                new String[] {}), searchFactory.getAnalyzer(clazz));
    Logger.getLogger(IndexUtility.class).info("  query = " + pfsQuery);
    luceneQuery = queryParser.parse(pfsQuery.toString());

    // Validate query terms
    luceneQuery =
        luceneQuery.rewrite(fullTextEntityManager.getSearchFactory()
            .getIndexReaderAccessor().open(clazz));
    Set<Term> terms = new HashSet<>();
    luceneQuery.extractTerms(terms);
    for (Term t : terms) {
      if (t.field() != null
          && !t.field().isEmpty()
          && !IndexUtility.getIndexedFieldNames(fieldNamesKey, false).contains(
              t.field())) {
        throw new LocalException("Query references invalid field name "
            + t.field() + ", "
            + IndexUtility.getIndexedFieldNames(fieldNamesKey, false));
      }
    }

    fullTextQuery =
        fullTextEntityManager.createFullTextQuery(luceneQuery, clazz);

    if (pfs != null) {
      // if start index and max results are set, set paging
      if (pfs.getStartIndex() >= 0 && pfs.getMaxResults() >= 0) {
        fullTextQuery.setFirstResult(pfs.getStartIndex());
        fullTextQuery.setMaxResults(pfs.getMaxResults());
      }

      // if sort field is specified, set sort key
      if (pfs.getSortField() != null && !pfs.getSortField().isEmpty()) {
        Map<String, Boolean> nameToAnalyzedMap =
            IndexUtility.getNameAnalyzedPairsFromAnnotation(clazz,
                pfs.getSortField());
        String sortField = null;

        if (nameToAnalyzedMap.size() == 0) {
          throw new Exception(clazz.getName()
              + " does not have declared, annotated method for field "
              + pfs.getSortField());
        }

        // first check the default name (rendered as ""), if not analyzed, use
        // this as sort
        if (nameToAnalyzedMap.get("") != null
            && nameToAnalyzedMap.get("").equals(false)) {
          sortField = pfs.getSortField();
        }

        // otherwise check explicit [name]Sort index
        else if (nameToAnalyzedMap.get(pfs.getSortField() + "Sort") != null
            && nameToAnalyzedMap.get(pfs.getSortField() + "Sort").equals(false)) {
          sortField = pfs.getSortField() + "Sort";
        }

        // if none, throw exception
        if (sortField == null) {
          throw new Exception(
              "Could not retrieve a non-analyzed Field annotation for get method for variable name "
                  + pfs.getSortField());
        }

        Sort sort = new Sort(new SortField(sortField, SortField.STRING));
        fullTextQuery.setSort(sort);
      }
    }
    return fullTextQuery;
  }
}
