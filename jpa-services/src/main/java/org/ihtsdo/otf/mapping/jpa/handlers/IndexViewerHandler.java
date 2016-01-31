package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * A handler for index viewer calls.
 */
public class IndexViewerHandler {

  /** Track main level label by first component of link. */
  private Map<String, String> linkToLabelMap = new HashMap<>();

  /**
   * Instantiates an empty {@link IndexViewerHandler}.
   */
  public IndexViewerHandler() {

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ContentService#performAggregatedSearch(
   * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
   * java.lang.String)
   */
  /**
   * Find index entries.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param domain the domain
   * @param searchField the search field
   * @param subSearchField the sub search field
   * @param subSubSearchField the sub sub search field
   * @param allFlag the all flag
   * @return the search result list
   * @throws Exception the exception
   */
  public SearchResultList findIndexEntries(String terminology,
    String terminologyVersion, String domain, String searchField,
    String subSearchField, String subSubSearchField, boolean allFlag)
    throws Exception {

    SearchResultList searchResultList = new SearchResultListJpa();

    List<String> mainSearchResults = new ArrayList<>();
    List<String> subSearchResults = new ArrayList<>();
    List<String> subSubSearchResults = new ArrayList<>();

    Properties config = ConfigUtility.getConfigProperties();

    if (allFlag) {
      config.setProperty("index.viewer.searchStartLevel", "0");
      config.setProperty("index.viewer.searchEndLevel", "9");
      config.setProperty("index.viewer.subSearchStartLevel", "0");
      config.setProperty("index.viewer.subSearchEndLevel", "9");
      config.setProperty("index.viewer.subSubSearchStartLevel", "0");
      config.setProperty("index.viewer.subSubSearchEndLevel", "9");
    } else {
      config.setProperty("index.viewer.searchStartLevel", "0");
      config.setProperty("index.viewer.searchEndLevel", "0");
      config.setProperty("index.viewer.subSearchStartLevel", "1");
      config.setProperty("index.viewer.subSearchEndLevel", "1");
      config.setProperty("index.viewer.subSubSearchStartLevel", "2");
      config.setProperty("index.viewer.subSubSearchEndLevel", "2");
    }

    int startLevel =
        Integer.parseInt(config.getProperty("index.viewer.searchStartLevel"));
    int endLevel =
        Integer.parseInt(config.getProperty("index.viewer.searchEndLevel"));
    mainSearchResults =
        performSearch(terminology, terminologyVersion, domain, searchField,
            startLevel, endLevel, null, (subSearchField != null
                && !subSearchField.equals("undefined") && !subSearchField
                .equals("")));

    if (subSearchField == null || subSearchField.equals("undefined")
        || subSearchField.equals("")) {
      for (String result : mainSearchResults) {
        SearchResult searchResult = new SearchResultJpa();
        searchResult.setValue(result);
        searchResult.setValue2(linkToLabelMap.get(result));
        searchResultList.addSearchResult(searchResult);
      }
      return searchResultList;
    } else {
      startLevel =
          Integer.parseInt(config
              .getProperty("index.viewer.subSearchStartLevel"));
      endLevel =
          Integer
              .parseInt(config.getProperty("index.viewer.subSearchEndLevel"));
      for (int i = 0; i < mainSearchResults.size(); i++) {
        subSearchResults.addAll(performSearch(terminology, terminologyVersion,
            domain, subSearchField, startLevel, endLevel,
            mainSearchResults.get(i), false));
      }
    }

    if (subSubSearchField == null || subSubSearchField.equals("undefined")
        || subSubSearchField.equals("")) {
      for (String result : subSearchResults) {
        SearchResult searchResult = new SearchResultJpa();
        searchResult.setValue(result);
        searchResult.setValue2(linkToLabelMap.get(result));
        searchResultList.addSearchResult(searchResult);
      }
      return searchResultList;
    } else {
      startLevel =
          Integer.parseInt(config
              .getProperty("index.viewer.subSubSearchStartLevel"));
      endLevel =
          Integer.parseInt(config
              .getProperty("index.viewer.subSubSearchEndLevel"));
      for (int i = 0; i < subSearchResults.size(); i++) {
        subSubSearchResults.addAll(performSearch(terminology,
            terminologyVersion, domain, subSubSearchField, startLevel,
            endLevel, subSearchResults.get(i), false));
      }
    }

    for (String result : subSubSearchResults) {
      SearchResult searchResult = new SearchResultJpa();
      searchResult.setValue(result);
      searchResult.setValue2(linkToLabelMap.get(result));
      searchResultList.addSearchResult(searchResult);
    }
    return searchResultList;

  }

  /**
   * Performs the search.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param domain the domain
   * @param searchStr the search string
   * @param startLevel the start level
   * @param endLevel the end level
   * @param subSearchAnchor the sub search anchor
   * @param requireHasChild the require has child
   * @return the results
   * @throws Exception the exception
   */
  private List<String> performSearch(String terminology,
    String terminologyVersion, String domain, String searchStr, int startLevel,
    int endLevel, String subSearchAnchor, boolean requireHasChild)
    throws Exception {
    Logger.getLogger(this.getClass()).info("Perform index search ");
    Logger.getLogger(this.getClass()).info("  terminology = " + terminology);
    Logger.getLogger(this.getClass()).info("  domain = " + domain);
    Logger.getLogger(this.getClass()).info("  searchStr = " + searchStr);

    Properties config = ConfigUtility.getConfigProperties();
    String prop = config.getProperty("index.viewer.data");
    if (prop == null) {
      return new ArrayList<>();
    }
    String indexesDir =
        prop + "/" + terminology + "/" + terminologyVersion + "/lucene/"
            + domain;

    List<String> searchResults = new ArrayList<>();
    // configure
    File selectedDomainDir = new File(indexesDir);
    String query = searchStr + " " + getLevelConstraint(startLevel, endLevel);
    if (requireHasChild)
      query = query + " hasChild:true";
    if (subSearchAnchor != null && subSearchAnchor.indexOf(".") == -1)
      query = query + " topLink:" + subSearchAnchor;
    if (subSearchAnchor != null && subSearchAnchor.indexOf(".") != -1)
      query =
          query + " topLink:"
              + subSearchAnchor.substring(0, subSearchAnchor.indexOf('.'));

    int maxHits = Integer.parseInt(config.getProperty("index.viewer.maxHits"));

    // Open index
    Logger.getLogger(this.getClass()).info("  Open index reader");
    Directory dir = FSDirectory.open(selectedDomainDir);
    IndexReader reader = IndexReader.open(dir);

    // Prep searcher
    Logger.getLogger(this.getClass()).info("  Prep searcher");
    IndexSearcher searcher = new IndexSearcher(reader);
    String defaultField = "title";
    Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
    fieldAnalyzers.put("code", new KeywordAnalyzer());
    PerFieldAnalyzerWrapper analyzer =
        new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_36),
            fieldAnalyzers);

    Logger.getLogger(this.getClass()).info("  Prep searcher");
    QueryParser parser =
        new QueryParser(Version.LUCENE_36, defaultField, analyzer);

    // Prep query
    parser.setAllowLeadingWildcard(true);
    parser.setDefaultOperator(Operator.AND);
    TopDocs hits = null;
    Query q = parser.parse(query);
    hits = searcher.search(q, maxHits);

    ScoreDoc[] scoreDocs = hits.scoreDocs;
    for (int n = 0; n < scoreDocs.length; ++n) {
      final ScoreDoc sd = scoreDocs[n];
      final Document d = searcher.doc(sd.doc);
      final String link = d.get("link");
      final String levelTag = d.get("level");
      final String label = d.get("label");
      int level = Integer.parseInt(levelTag);

      String linkFirstComponent =
          (link.indexOf(".") != -1) ? link.substring(0, link.indexOf("."))
              : link;
      if (label != null)
        linkToLabelMap.put(linkFirstComponent, label);

      // If subSearchAnchor is specified, the link must start with it
      if (subSearchAnchor != null && !link.startsWith(subSearchAnchor + "."))
        continue;

      // If actual level is within desired range (inclusive), add match
      else if (level >= startLevel && level <= endLevel)
        searchResults.add(link);

    }
    analyzer.close();
    reader.close();
    dir.close();
    searcher.close();
    return searchResults;
  }

  /**
   * Returns the level constraint.
   *
   * @param s the s
   * @param e the e
   * @return the level constraint
   */
  @SuppressWarnings("static-method")
  private String getLevelConstraint(int s, int e) {
    if (s == e) {
      return "level:" + s;
    } else {
      return "level:[" + s + " TO " + e + "]";
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ContentService#getIndexDomains(java.lang
   * .String, java.lang.String)
   */
  /**
   * Returns the index domains.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the index domains
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  public SearchResultList getIndexDomains(String terminology,
    String terminologyVersion) throws Exception {

    SearchResultList searchResultList = new SearchResultListJpa();

    // Local directory
    String dataDir =
        ConfigUtility.getConfigProperties().getProperty("index.viewer.data");
    for (File termDir : new File(dataDir).listFiles()) {
      // Find terminology directory
      if (termDir.getName().equals(terminology)) {
        for (File versionDir : termDir.listFiles()) {
          // Find version directory
          if (versionDir.getName().equals(terminologyVersion)) {
            for (File typeDir : versionDir.listFiles()) {
              // find html directory
              if (typeDir.getName().equals("html")) {
                // find domain directories
                for (File domainDir : typeDir.listFiles()) {
                  SearchResult searchResult = new SearchResultJpa();
                  searchResult.setValue(domainDir.getName());
                  searchResultList.addSearchResult(searchResult);
                  Logger.getLogger(ContentServiceJpa.class).info(
                      "  Index domain found: " + domainDir.getName());
                }
              }
            }
          }
        }
      }
    }

    return searchResultList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.ihtsdo.otf.mapping.services.ContentService#getIndexViewerPagesForIndex
   * (java.lang.String, java.lang.String, java.lang.String)
   */
  /**
   * Returns the index pages for index.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param index the index
   * @return the index pages for index
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  public SearchResultList getIndexPagesForIndex(String terminology,
    String terminologyVersion, String index) throws Exception {

    SearchResultList searchResultList = new SearchResultListJpa();

    // Local directory
    String dataDir =
        ConfigUtility.getConfigProperties().getProperty("index.viewer.data");

    for (File termDir : new File(dataDir).listFiles()) {
      // Find terminology directory
      if (termDir.getName().equals(terminology)) {
        for (File versionDir : termDir.listFiles()) {
          // find version directory
          if (versionDir.getName().equals(terminologyVersion)) {
            for (File typeDir : versionDir.listFiles()) {
              // find html directory
              if (typeDir.getName().equals("html")) {
                for (File domainDir : typeDir.listFiles()) {
                  // find domain directory
                  if (domainDir.getName().equals(index)) {
                    Logger.getLogger(ContentServiceJpa.class).info(
                        "  Pages for index domain found: "
                            + domainDir.getName());
                    // find pages
                    for (File pageFile : domainDir.listFiles()) {
                      SearchResult searchResult = new SearchResultJpa();
                      searchResult.setValue(pageFile.getName().substring(0,
                          pageFile.getName().indexOf('.')));
                      searchResultList.addSearchResult(searchResult);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    return searchResultList;
  }

}
