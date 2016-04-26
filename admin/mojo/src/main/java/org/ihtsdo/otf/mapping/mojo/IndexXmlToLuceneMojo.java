package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Converts XML index file to HTML pages.
 * 
 * See admin/indexes/pom.xml for a sample execution.
 * 
 * @goal convert-to-lucene
 */
public class IndexXmlToLuceneMojo extends AbstractMojo {

  /** The writer. */
  IndexWriter writer = null;

  /** The out. */
  private PrintWriter out = null;

  /** The words. */
  Set<String> words = null;

  /** Delimiter string - not part of a word. */
  private static final String DELIM = " \t-({[)}]_!@#%&\\:;\"',.?/~+=|<>$`^";

  /**
   * The input directory.
   * 
   * @parameter
   * @required
   */
  private String inputDir;

  /**
   * The input XML file.
   * 
   * @parameter
   */
  private String inputFile;

  /**
   * The terminology.
   * 
   * @parameter
   */
  String terminology;

  /**
   * The terminology version.
   * 
   * @parameter
   */
  String terminologyVersion;

  /**
   * The domain.
   * 
   * @parameter
   */
  String domain;

  /**
   * The dictionary file.
   * 
   */
  private String dictionaryFile;

  /** The output dir. */
  private String outputDir;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting to convert index XML to Lucene");
    getLog().info("  inputDir = " + inputDir);
    getLog().info("  inputFile = " + inputFile);
    getLog().info("  terminology = " + terminology);
    getLog().info("  terminologyVersion = " + terminologyVersion);
    getLog().info("  domain = " + domain);

    // Use XML parser to go through extracted XML files
    // and produce HTLM files

    try {

      // set the input directory
      String baseDir = inputDir + "/" + terminology + "/" + terminologyVersion;
      inputDir = baseDir + "/xml";

      // set the lucene output directory
      // TODO: put this into "lucene" directory of baseDir, not hibernate setting!
      String indexBaseDir =
          ConfigUtility.getConfigProperties().getProperty(
              "hibernate.search.default.indexBase");
      outputDir =
          indexBaseDir + "/" + terminology + "/" + terminologyVersion + "/"
              + domain;
      String dictionaryDir = outputDir + "/config/dict";
      dictionaryFile = "words.txt";

      getLog().info("  outputDir: " + outputDir);
      getLog().info("  dictionaryDir: " + dictionaryDir);

      // Set up index/dictionary directories and files
      new File(outputDir).mkdirs();
      new File(dictionaryDir, dictionaryFile).getParentFile().mkdirs();
      dictionaryFile =
          new File(dictionaryDir, dictionaryFile).getAbsolutePath();
      out = new PrintWriter(new FileWriter(new File(dictionaryFile)));

      // initialize state
      words = new HashSet<>();

      Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
      fieldAnalyzers.put("code", new KeywordAnalyzer());
      PerFieldAnalyzerWrapper analyzer =
          new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_36),
              fieldAnalyzers);

      // Open index writer
      IndexWriterConfig config =
          new IndexWriterConfig(Version.LUCENE_36, analyzer);
      config.setOpenMode(OpenMode.CREATE);
      writer =
          new IndexWriter(new SimpleFSDirectory(new File(outputDir)), config);

      // Prep SAX parser
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      SAXParser saxParser = factory.newSAXParser();
      DefaultHandler handler = new LocalHandler();

      // Open XML and begin parsing
      File file = new File(inputDir, inputFile);

      if (!file.exists()) {
        throw new MojoFailureException(
            "Specified index.viewer.ICD10.xml directory does not exist: "
                + inputFile);
      }

      InputStream inputStream = checkForUtf8BOM(new FileInputStream(file));
      Reader reader = new InputStreamReader(inputStream, "UTF-8");
      InputSource is = new InputSource(reader);
      is.setEncoding("UTF-8");
      saxParser.parse(is, handler);

      // close Lucene index
      writer.close();

      // Write dictionary entries
      List<String> orderedWords = new ArrayList<>(words);
      Collections.sort(orderedWords);
      for (String word : orderedWords)
        out.println(word);
      out.flush();
      out.close();

      getLog().info("Done...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Conversion of XML to HTML failed", e);
    }

  }

  /**
   * Check for utf8 bom.
   * 
   * @param inputStream the input stream
   * @return the input stream
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static InputStream checkForUtf8BOM(InputStream inputStream)
    throws IOException {
    PushbackInputStream pushbackInputStream =
        new PushbackInputStream(new BufferedInputStream(inputStream), 3);
    byte[] bom = new byte[3];
    if (pushbackInputStream.read(bom) != -1) {
      if (!(bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF)) {
        pushbackInputStream.unread(bom);
      }
    }
    return pushbackInputStream;
  }

  /**
   * The SAX parser handler
   */
  class LocalHandler extends DefaultHandler {

    /** The chars. */
    StringBuffer chars = null;

    /** The in tag. */
    Map<String, Integer> inTag = new HashMap<>();

    /** The data. */
    Map<String, String> data = new HashMap<>();

    /** The data stack. */
    Stack<Map<String, String>> dataStack = new Stack<>();

    /** The letter. */
    String letter = null;

    /** does mainTerm have term child? */
    boolean hasChild = false;

    /** The act. */
    int act = 0;

    /** The aname. */
    String aname = null;

    /** Indicates that there is a nemod tag embedded in the title tag */
    boolean nemodInTitle = false;

    /** Indicates if currently processing "title" tag */
    boolean inTitle = false;

    /** the title characters */
    StringBuffer titleChars = new StringBuffer();

    /**
     * Instantiates a new local handler.
     */
    public LocalHandler() {
      super();
      // prevent null pointer exception
      inTag.put("mainterm", 0);
      inTag.put("letter", 0);
      inTag.put("term", 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     * java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName,
      Attributes attributes) {

      // track whether we are in the title
      if (qName.equalsIgnoreCase("title")) {
        inTitle = true;
      }

      // handle embedded nemod in title
      if (qName.equalsIgnoreCase("nemod") && inTitle) {
        titleChars = chars;
        nemodInTitle = true;
      }

      // reset chars buffer
      chars = new StringBuffer();
      if (inTag.containsKey(qName.toLowerCase()))
        inTag.put(qName.toLowerCase(), inTag.get(qName.toLowerCase()) + 1);
      else
        inTag.put(qName.toLowerCase(), 1);

      // When encountering a new term or main term entry
      // push the data values onto the stack.
      if (qName.equalsIgnoreCase("term") || qName.equalsIgnoreCase("mainterm")) {
        if (data != null) {
          dataStack.push(data);
          hasChild = false;
        }
        data = new HashMap<>(0);
      }

      // handle mainterm
      if (qName.equalsIgnoreCase("mainterm")) {
        // increment aname counter
        act++;
        aname = letter + act;
        data.put("aname", aname);
      }

      // handle term
      if (qName.equalsIgnoreCase("term")) {
        // increment aname counter
        act++;
        aname = aname + "." + act;
        data.put("aname", aname);
        // acquire term level
        data.put("level", attributes.getValue("level"));

      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String localName, String qName)
      throws SAXException {
    	
    	if (qName.toLowerCase().contains("see")) {
    		Logger.getLogger(getClass()).info("see content " + qName + ", " + chars.toString());;
    	}

      // handle embedded nemod in title
      if (qName.equalsIgnoreCase("title") && nemodInTitle) {
        data.put("afterNemodTitleChars", chars.toString());
        chars = titleChars;
        titleChars = new StringBuffer();
      }

      // Put character data into map
      data.put(qName.toLowerCase(), chars.toString());

      // Handle case of a new letter
      if (qName.equalsIgnoreCase("title") && inTag.get("letter") > 0
          && inTag.get("mainterm") == 0) {
        letter = data.get("title");
      }

      // at an end mainterm tag, add document, pop stack
      if (qName.equalsIgnoreCase("mainterm")) {
        try {
          if (data.get("title") != null) {
            addDocumentForMainTerm();
          }
          data = dataStack.pop();
        } catch (CorruptIndexException e) {
          throw new SAXException(e);
        } catch (IOException e) {
          throw new SAXException(e);
        }
      }

      // at an end term tag, add document, pop stack
      // pop aname
      if (qName.equalsIgnoreCase("term")) {
        try {
          if (data.get("title") != null) {
            addDocumentForTerm();
          }
          data = dataStack.pop();
          hasChild = true;
          aname = aname.substring(0, aname.lastIndexOf('.'));
        } catch (CorruptIndexException e) {
          throw new SAXException(e);
        } catch (IOException e) {
          throw new SAXException(e);
        }
      }

      // Indicate tag end
      inTag.put(qName.toLowerCase(), inTag.get(qName.toLowerCase()) - 1);

      // Reset flags when ending title tag
      if (qName.equalsIgnoreCase("title") && inTitle) {
        inTitle = false;
        nemodInTitle = false;
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char ch[], int start, int length) {
      chars.append(new String(ch, start, length));
    }

    /**
     * Adds the dictionary words.
     */
    private void addDictionaryWords() {
      StringTokenizer st = new StringTokenizer(data.get("title"), DELIM);
      while (st.hasMoreTokens()) {
        String token = st.nextToken();
        if (!StandardAnalyzer.STOP_WORDS_SET.contains(token)
            && token.length() > 2) {
          words.add(token.toLowerCase());
        }
      }
    }

    /**
     * Adds the document for main term.
     * 
     * @throws CorruptIndexException the corrupt index exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void addDocumentForMainTerm() throws CorruptIndexException,
      IOException {
      Document doc = new Document();
      // getLog().info("mainTerm = " + data.get("title") + ", " +
      // data.get("code") + ", " + data.get("nemod") + ", " +
      // data.get("aname") + ", " + String.valueOf(hasChild));
      doc.add(new Field("s", getFirstWord(data.get("title")), Store.NO,
          Index.ANALYZED, TermVector.NO));
      doc.add(new Field("hasChild", String.valueOf(hasChild), Store.NO,
          Index.ANALYZED, TermVector.NO));
      doc.add(new Field("title", data.get("title"), Store.YES, Index.ANALYZED,
          TermVector.NO));
      if (data.get("nemod") != null)
        doc.add(new Field("nemod", data.get("nemod"), Store.YES, Index.ANALYZED,
            TermVector.NO));
      doc.add(new Field("label", data.get("title"), Store.YES,
          Index.NOT_ANALYZED, TermVector.NO));
      doc.add(new Field("link", data.get("aname"), Store.YES,
          Index.NOT_ANALYZED, TermVector.NO));
      doc.add(new Field("level", "0", Store.YES, Index.NOT_ANALYZED,
          TermVector.NO));
      if (data.get("code") != null)
        doc.add(new Field("code", data.get("code"), Store.YES, Index.ANALYZED,
            TermVector.NO));
      if (data.get("see") != null) {
        doc.add(new Field("see", data.get("see"), Store.YES, Index.ANALYZED, TermVector.NO));
      }
      if (data.get("seealso") != null) {
        doc.add(new Field("seealso", data.get("seealso"), Store.YES, Index.ANALYZED, TermVector.NO));
      }
      writer.addDocument(doc);

      addDictionaryWords();
    }

    /**
     * Adds the document for term.
     * 
     * @throws CorruptIndexException the corrupt index exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void addDocumentForTerm() throws CorruptIndexException, IOException {

      Document doc = new Document();
      // getLog().info("term = " + data.get("title") + ", " +
      // data.get("code") + ", " + data.get("nemod") + ", " +
      // data.get("level") + ", " + data.get("aname") + ", " +
      // String.valueOf(hasChild));
      doc.add(new Field("s", getFirstWord(data.get("title")), Store.NO,
          Index.ANALYZED, TermVector.NO));
      doc.add(new Field("hasChild", String.valueOf(hasChild), Store.NO,
          Index.ANALYZED, TermVector.NO));
      doc.add(new Field("title", data.get("title"), Store.YES, Index.ANALYZED,
          TermVector.NO));
      if (data.get("nemod") != null) {
        doc.add(new Field("nemod", data.get("nemod"), Store.YES, Index.ANALYZED,
            TermVector.NO));
      }
      String aname = data.get("aname");
      doc.add(new Field("link", aname, Store.YES, Index.NOT_ANALYZED,
          TermVector.NO));
      doc.add(new Field("topLink", aname.substring(0, aname.indexOf('.')),
          Store.NO, Index.ANALYZED, TermVector.NO));
      doc.add(new Field("level", data.get("level"), Store.YES,
          Index.NOT_ANALYZED, TermVector.NO));
      if (data.get("code") != null)
        doc.add(new Field("code", data.get("code"), Store.YES, Index.ANALYZED,
            TermVector.NO));
      if (data.get("see") != null) {
    	  Logger.getLogger(getClass()).info("see indexed " + data.get("see"));
        doc.add(new Field("see", data.get("see"), Store.YES, Index.ANALYZED, 
        	TermVector.NO));
      }
      if (data.get("seealso") != null) {
    	  Logger.getLogger(getClass()).info("seealso indexed " + data.get("seealso"));
        doc.add(new Field("seealso", data.get("seealso"), Store.YES, 
        	Index.ANALYZED, TermVector.NO));
      }
      writer.addDocument(doc);
      addDictionaryWords();
    }
  }

  /**
   * Gets the first word of the specified string.
   * 
   * @param str the str
   * @return the first word
   */
  @SuppressWarnings("static-method")
  public String getFirstWord(String str) {
    StringTokenizer st = new StringTokenizer(str, DELIM);
    if (st.hasMoreTokens())
      return st.nextToken().toLowerCase();
    else
      return "";
  }
}