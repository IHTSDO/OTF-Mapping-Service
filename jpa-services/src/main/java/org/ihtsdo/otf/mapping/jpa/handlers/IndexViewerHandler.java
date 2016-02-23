package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.apache.lucene.document.Fieldable;
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
    final List<String> subSearchResults = new ArrayList<>();
    final List<String> subSubSearchResults = new ArrayList<>();

    final Properties config = ConfigUtility.getConfigProperties();

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
        performCodeSearch(terminology, terminologyVersion, domain, searchField);
    if (mainSearchResults.size() == 0) {
      mainSearchResults = performSearch(terminology, terminologyVersion, domain,
          searchField, startLevel, endLevel, null,
          (subSearchField != null && !subSearchField.equals("undefined")
              && !subSearchField.equals("")));
    }

    if (subSearchField == null || subSearchField.equals("undefined")
        || subSearchField.equals("")) {
      for (final String result : mainSearchResults) {
        SearchResult searchResult = new SearchResultJpa();
        searchResult.setValue(result);
        searchResult.setValue2(linkToLabelMap.get(result));
        searchResultList.addSearchResult(searchResult);
      }
      return sortSearchResultList(searchResultList);
    } else {
      startLevel = Integer
          .parseInt(config.getProperty("index.viewer.subSearchStartLevel"));
      endLevel = Integer
          .parseInt(config.getProperty("index.viewer.subSearchEndLevel"));
      for (int i = 0; i < mainSearchResults.size(); i++) {
        subSearchResults.addAll(performSearch(terminology, terminologyVersion,
            domain, subSearchField, startLevel, endLevel,
            mainSearchResults.get(i), false));
      }
    }

    if (subSubSearchField == null || subSubSearchField.equals("undefined")
        || subSubSearchField.equals("")) {
      for (final String result : subSearchResults) {
        SearchResult searchResult = new SearchResultJpa();
        searchResult.setValue(result);
        searchResult.setValue2(linkToLabelMap.get(result));
        searchResultList.addSearchResult(searchResult);
      }
      return sortSearchResultList(searchResultList);
    } else {
      startLevel = Integer
          .parseInt(config.getProperty("index.viewer.subSubSearchStartLevel"));
      endLevel = Integer
          .parseInt(config.getProperty("index.viewer.subSubSearchEndLevel"));
      for (int i = 0; i < subSearchResults.size(); i++) {
        subSubSearchResults.addAll(performSearch(terminology,
            terminologyVersion, domain, subSubSearchField, startLevel, endLevel,
            subSearchResults.get(i), false));
      }
    }

    for (final String result : subSubSearchResults) {
      SearchResult searchResult = new SearchResultJpa();
      searchResult.setValue(result);
      searchResult.setValue2(linkToLabelMap.get(result));
      searchResultList.addSearchResult(searchResult);
    }

    return sortSearchResultList(searchResultList);

  }

  private SearchResultList sortSearchResultList(SearchResultList list) {
    // sort search results
    List<SearchResult> searchResults = list.getSearchResults();
    Collections.sort(searchResults, new Comparator<SearchResult>() {
      public int compare(SearchResult o1, SearchResult o2) {
        return o1.getValue2().compareTo(o2.getValue2());
      }
    });
    list.setSearchResults(searchResults);
    return list;
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

    Logger.getLogger(this.getClass()).debug("Perform index search ");
    Logger.getLogger(this.getClass()).debug("  terminology = " + terminology);
    Logger.getLogger(this.getClass()).debug("  domain = " + domain);
    Logger.getLogger(this.getClass()).debug("  searchStr = " + searchStr);

    final Properties config = ConfigUtility.getConfigProperties();
    final String prop = config.getProperty("index.viewer.data");
    if (prop == null) {
      return new ArrayList<>();
    }
    final String indexesDir = prop + "/" + terminology + "/"
        + terminologyVersion + "/lucene/" + domain;

    final List<String> searchResults = new ArrayList<>();
    // configure
    final File selectedDomainDir = new File(indexesDir);

    String query = "";
    if (startLevel != -1 && endLevel != -1)
      query = searchStr + " " + getLevelConstraint(startLevel, endLevel);
    if (requireHasChild)
      query = query + " hasChild:true";
    if (subSearchAnchor != null && subSearchAnchor.indexOf(".") == -1)
      query = query + " topLink:" + subSearchAnchor;
    if (subSearchAnchor != null && subSearchAnchor.indexOf(".") != -1)
      query = query + " topLink:"
          + subSearchAnchor.substring(0, subSearchAnchor.indexOf('.'));

    int maxHits = Integer.parseInt(config.getProperty("index.viewer.maxHits"));

    // Open index
    Logger.getLogger(this.getClass()).debug("  Open index reader");
    final Directory dir = FSDirectory.open(selectedDomainDir);
    final IndexReader reader = IndexReader.open(dir);

    // Prep searcher
    Logger.getLogger(this.getClass()).debug("  Prep searcher");
    final IndexSearcher searcher = new IndexSearcher(reader);
    final String defaultField = "title";
    final Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
    fieldAnalyzers.put("code", new KeywordAnalyzer());
    final PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
        new StandardAnalyzer(Version.LUCENE_36), fieldAnalyzers);

    Logger.getLogger(this.getClass()).debug("  Prep searcher");
    final QueryParser parser =
        new QueryParser(Version.LUCENE_36, defaultField, analyzer);

    // Prep query
    parser.setAllowLeadingWildcard(true);
    parser.setDefaultOperator(Operator.AND);
    TopDocs hits = null;
    final Query q = parser.parse(query);
    hits = searcher.search(q, maxHits);

    final ScoreDoc[] scoreDocs = hits.scoreDocs;
    for (int n = 0; n < scoreDocs.length; ++n) {
      final ScoreDoc sd = scoreDocs[n];
      final Document d = searcher.doc(sd.doc);
      final String link = d.get("link");
      final String levelTag = d.get("level");
      final String label = d.get("label");
      int level = Integer.parseInt(levelTag);

      String linkFirstComponent = (link.indexOf(".") != -1)
          ? link.substring(0, link.indexOf(".")) : link;
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
   * Perform code search.
   *
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @param domain the domain
   * @param code the code
   * @return the list
   * @throws Exception the exception
   */
  private List<String> performCodeSearch(String terminology,
    String terminologyVersion, String domain, String code) throws Exception {
    Logger.getLogger(this.getClass()).debug("Perform index code search ");
    Logger.getLogger(this.getClass()).debug("  terminology = " + terminology);
    Logger.getLogger(this.getClass()).debug("  domain = " + domain);
    Logger.getLogger(this.getClass()).debug("  code = " + code);

    return performSearch(terminology, terminologyVersion, domain,
        code.contains("code:") ? code : "code:" + code, 0, 2, null, false);

  }

  private Document getDocumentForLink(String terminology,
    String terminologyVersion, String domain, String linkStr) throws Exception {
    Logger.getLogger(this.getClass()).debug("Perform index link search ");
    Logger.getLogger(this.getClass()).debug("  terminology = " + terminology);
    Logger.getLogger(this.getClass()).debug("  domain = " + domain);
    Logger.getLogger(this.getClass()).debug("  link = " + linkStr);

    final Properties config = ConfigUtility.getConfigProperties();
    final String prop = config.getProperty("index.viewer.data");
    if (prop == null) {
      return null;
    }
    final String indexesDir = prop + "/" + terminology + "/"
        + terminologyVersion + "/lucene/" + domain;

    // configure
    final File selectedDomainDir = new File(indexesDir);

    String query = "link:" + linkStr;

    int maxHits = Integer.parseInt(config.getProperty("index.viewer.maxHits"));

    // Open index
    Logger.getLogger(this.getClass()).debug("  Open index reader");
    final Directory dir = FSDirectory.open(selectedDomainDir);
    final IndexReader reader = IndexReader.open(dir);

    // Prep searcher
    Logger.getLogger(this.getClass()).debug("  Prep searcher");
    final IndexSearcher searcher = new IndexSearcher(reader);
    final String defaultField = "title";
    final Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
    fieldAnalyzers.put("link", new KeywordAnalyzer());
    final PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
        new StandardAnalyzer(Version.LUCENE_36), fieldAnalyzers);

    Logger.getLogger(this.getClass()).debug("  Prep searcher");
    final QueryParser parser =
        new QueryParser(Version.LUCENE_36, defaultField, analyzer);

    // Prep query
    parser.setAllowLeadingWildcard(true);
    parser.setDefaultOperator(Operator.AND);
    TopDocs hits = null;

    try {
      final Query q = parser.parse(query);
      hits = searcher.search(q, maxHits);

      final ScoreDoc[] scoreDocs = hits.scoreDocs;

      // expect exactly one result
      if (scoreDocs.length != 1) {
        throw new Exception("Unexpected number of results (" + scoreDocs.length
            + ") for link search " + linkStr);
      }
      final Document d = searcher.doc(scoreDocs[0].doc);
      return d;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {

      analyzer.close();
      reader.close();
      dir.close();
      searcher.close();
    }

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

    if (dataDir == null || dataDir.isEmpty()) {
      return searchResultList;
    }
    for (final File termDir : new File(dataDir).listFiles()) {
      // Find terminology directory
      if (termDir.getName().toLowerCase().equals(terminology.toLowerCase())) {
        for (final File versionDir : termDir.listFiles()) {
          // Find version directory
          if (versionDir.getName().equals(terminologyVersion)) {
            for (final File typeDir : versionDir.listFiles()) {
              // find html directory
              if (typeDir.getName().equals("html")) {
                // find domain directories
                for (final File domainDir : typeDir.listFiles()) {
                  SearchResult searchResult = new SearchResultJpa();
                  searchResult.setValue(domainDir.getName());
                  searchResultList.addSearchResult(searchResult);
                  Logger.getLogger(ContentServiceJpa.class)
                      .debug("  Index domain found: " + domainDir.getName());
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

    final SearchResultList searchResultList = new SearchResultListJpa();

    // Local directory
    final String dataDir =
        ConfigUtility.getConfigProperties().getProperty("index.viewer.data");

    for (final File termDir : new File(dataDir).listFiles()) {
      // Find terminology directory
      if (termDir.getName().equals(terminology)) {
        for (final File versionDir : termDir.listFiles()) {
          // find version directory
          if (versionDir.getName().equals(terminologyVersion)) {
            for (final File typeDir : versionDir.listFiles()) {
              // find html directory
              if (typeDir.getName().equals("html")) {
                for (final File domainDir : typeDir.listFiles()) {
                  // find domain directory
                  if (domainDir.getName().equals(index)) {
                    Logger.getLogger(ContentServiceJpa.class)
                        .debug("  Pages for index domain found: "
                            + domainDir.getName());
                    // find pages
                    for (final File pageFile : domainDir.listFiles()) {
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

  public String getDetailsAsHtmlForLink(String terminology,
    String terminologyVersion, String domain, String link) throws Exception {

    if (link == null || link.isEmpty()) {
      throw new Exception("Empty or null label cannot be parsed");
    }

    String sublink = link;

    List<Document> documents = new ArrayList<>();
    while (sublink.length() > 0) {

      Logger.getLogger(getClass()).debug("Searching for link " + sublink);

      // perform the search
      Document d =
          getDocumentForLink(terminology, terminologyVersion, domain, sublink);

      // insert before later links
      documents.add(0, d);

      // construct the link text and add to details
      for (Fieldable field : d.getFields()) {
        Logger.getLogger(getClass())
            .debug("  " + field.name() + ": " + d.get(field.name()));
      }

      // truncate
      if (sublink.lastIndexOf(".") == -1) {
        sublink = "";
      } else {
        sublink = sublink.substring(0, sublink.lastIndexOf("."));
      }
    }

    String htmlFragment = "";

    // cycle over documents in reverse order (top-down)
    for (int i = 0; i < documents.size(); i++) {

      Document d = documents.get(i);

      // construct the link text and add to details
      for (Fieldable field : d.getFields()) {
        Logger.getLogger(getClass())
            .debug("  " + field.name() + ": " + d.get(field.name()));
      }

      // add indentation based on position
      for (int j = 0; j < i; j++) {
        htmlFragment += " - ";
      }

      // if the top level, bold the title
      if (i == 0) {
        htmlFragment += "<strong>" + d.get("title") + "</strong>";
      } else {
        htmlFragment += d.get("title");
      }
      
      // if code present, add
      if (d.get("code") != null) {
        htmlFragment += "&nbsp;" + d.get("code");
      }
      
      // add line break
      htmlFragment += "<br>";
      
    }

    return htmlFragment;

  }

}
