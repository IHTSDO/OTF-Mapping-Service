/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.GmdnMetadataHelper;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Component;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Converts GMDN data to RF2 objects.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-gmdn
 * @phase package
 */
public class TerminologyGmdnLoaderMojo extends AbstractMojo {

  /** The date format. */
  final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  /** The date format2. */
  final SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyyMMdd");

  /**
   * The input dir.
   *
   * @parameter
   * @required
   */
  private String inputDir;

  // NOTE: default visibility is used instead of private
  // so that the inner class parser does not require
  // the use of synthetic accessors

  /**
   * Name of terminology to be loaded.
   * @parameter
   * @required
   */
  String terminology;

  /**
   * Terminology version.
   *
   * @parameter
   * @required
   */
  String version;

  /** The effective time. */
  Date effectiveTime;

  /** The concept map. */
  Map<String, Concept> conceptMap;

  /** The roots. */
  List<String> roots = new ArrayList<>();

  /** The content service. */
  ContentService contentService;

  /** Child to parent code map NOTE: this assumes a single superclass. */
  Map<String, String> chdParMap;

  /** Indicates subclass relationships NOTE: this assumes a single superclass. */

  Map<String, Boolean> parChildrenMap;

  /** The helper. */
  GmdnMetadataHelper helper;

  /** The id ct for id assignment (descriptions and relationships). */
  int idCt = 1000;

  /**
   * Executes the plugin.
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting loading GMDN terminology");
    getLog().info("  inputDir = " + inputDir);
    getLog().info("  terminology = " + terminology);
    getLog().info("  version = " + version);

    FileInputStream fis = null;
    InputStream inputStream = null;
    Reader reader = null;
    try {

      // Check dir and get files
      if (!new File(inputDir).exists()) {
        throw new MojoFailureException(
            "Specified input directory does not exist");
      }

      // Validate inputDir
      File termFile = null;
      File collectivetermFile = null;
      File cttreenodeFile = null;
      File termcollectivetermFile = null;
      for (final File file : new File(inputDir).listFiles()) {
        if (file.getName().contains("termcollectiveterm")) {
          termcollectivetermFile = file;
        } else if (file.getName().contains("collectiveterm")) {
          collectivetermFile = file;
        } else if (file.getName().contains("cttreenode")) {
          cttreenodeFile = file;
        } else if (file.getName().contains("term")) {
          termFile = file;
        }
      }
      if (termFile == null || collectivetermFile == null
          || cttreenodeFile == null || termcollectivetermFile == null) {
        getLog().error("term = " + termFile);
        getLog().error("collectiveterm = " + collectivetermFile);
        getLog().error("termcollectiveterm = " + termcollectivetermFile);
        getLog().error("cttreenode = " + cttreenodeFile);
        throw new Exception("Input dir does not have all necessary files.");
      }

      // Open service and start initial transaction
      // wait until the end to commit
      contentService = new ContentServiceJpa();
      contentService.setTransactionPerOperation(false);
      contentService.beginTransaction();

      // create Metadata
      effectiveTime = new Date();
      getLog().info("  Create metadata classes - " + effectiveTime);
      helper =
          new GmdnMetadataHelper(terminology, version,
              dateFormat2.format(effectiveTime), contentService);
      conceptMap = helper.createMetadata();

      // Initialize par/chd maps
      chdParMap = new HashMap<>();
      parChildrenMap = new HashMap<>();

      //
      // Parse the termVV_V.xml file
      //

      // Prep SAX parser
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      SAXParser saxParser = factory.newSAXParser();
      DefaultHandler handler = new TermHandler();

      // Open XML and begin parsing
      getLog().info("  Process term file");
      fis = new FileInputStream(termFile);
      inputStream = checkForUtf8BOM(fis);
      reader = new InputStreamReader(inputStream, "UTF-8");
      InputSource is = new InputSource(reader);
      is.setEncoding("UTF-8");
      saxParser.parse(is, handler);
      fis.close();
      inputStream.close();
      reader.close();

      //
      // Parse the collectivetermVV_V.xml file
      //

      // Prep SAX parser
      getLog().info("  Process collectiveterm file");
      factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      saxParser = factory.newSAXParser();
      handler = new CollectiveTermHandler();

      // Open XML and begin parsing
      fis = new FileInputStream(collectivetermFile);
      inputStream = checkForUtf8BOM(fis);
      reader = new InputStreamReader(inputStream, "UTF-8");
      is = new InputSource(reader);
      is.setEncoding("UTF-8");
      saxParser.parse(is, handler);
      fis.close();
      inputStream.close();
      reader.close();

      //
      // Parse the termcollectivetermVV_V.xml file
      //

      // Prep SAX parser
      getLog().info("  Process termcollectiveterm file");
      factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      saxParser = factory.newSAXParser();
      handler = new TermCollectiveTermHandler();

      // Open XML and begin parsing
      fis = new FileInputStream(termcollectivetermFile);
      inputStream = checkForUtf8BOM(fis);
      reader = new InputStreamReader(inputStream, "UTF-8");
      is = new InputSource(reader);
      is.setEncoding("UTF-8");
      saxParser.parse(is, handler);
      fis.close();
      inputStream.close();
      reader.close();

      //
      // Parse the cttreenode.xml file
      //

      // Prep SAX parser
      getLog().info("  Process cttreenode file");
      factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      saxParser = factory.newSAXParser();
      handler = new CtTreeNodeHandler();

      // Open XML and begin parsing
      fis = new FileInputStream(cttreenodeFile);
      inputStream = checkForUtf8BOM(fis);
      reader = new InputStreamReader(inputStream, "UTF-8");
      is = new InputSource(reader);
      is.setEncoding("UTF-8");
      saxParser.parse(is, handler);
      fis.close();
      inputStream.close();
      reader.close();

      // Commit when finished
      contentService.commit();

      // Let the service create its own transaction.
      final String isaRelType = conceptMap.get("isa").getTerminologyId();
      for (final String root : roots) {
        getLog().info("Create tree positions for " + root + ", " + isaRelType);
        contentService.computeTreePositions(terminology, version, isaRelType,
            root);
      }

      getLog().info("Done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Conversion of GMDN to RF2 objects failed", e);
    } finally {
      try {
        if (fis != null) {
          fis.close();
        }
      } catch (IOException e) {
        // do nothing
      }
      try {
        if (inputStream != null) {
          inputStream.close();
        }
      } catch (IOException e) {
        // do nothing
      }
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (IOException e) {
        // do nothing
      }
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
    final PushbackInputStream pushbackInputStream =
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
   * Base SAX Parser handler for declaring vars.
   */
  class BaseHandler extends DefaultHandler {

    /** The chars. */
    StringBuilder chars = new StringBuilder();

    /** The label chars - used for description text. */
    StringBuilder labelChars = new StringBuilder();

    /** The tag stack. */
    Stack<String> tagStack = new Stack<>();

    /**
     * Instantiates an empty {@link BaseHandler}.
     */
    public BaseHandler() {
      super();
    }

    /* see superclass */
    @Override
    public void startElement(String uri, String localName, String qName,
      Attributes attributes) throws SAXException {

      // add current tag to stack
      tagStack.push(qName.toLowerCase());

    }

    /* see superclass */
    @Override
    public void endElement(String uri, String localName, String qName)
      throws SAXException {

      // pop tag stack and clear characters
      tagStack.pop();
      chars = new StringBuilder();

    }

    /* see superclass */
    @Override
    public void characters(char ch[], int start, int length) {
      chars.append(new String(ch, start, length));
    }
  }

  /**
   * SAX Parser handler for term<version>.xml. This parser creates
   * concepts/descriptions for terms.
   */
  class TermHandler extends BaseHandler {

    /** The concept. */
    private Concept concept = null;

    /** The description. */
    private Description description = null;

    /** The term id. */
    private String termId = null;

    /** The ivd. */
    private boolean ivd = false;

    /**
     * Instantiates a new local handler.
     */
    public TermHandler() {
      super();
    }

    /* see superclass */
    @Override
    public void startElement(String uri, String localName, String qName,
      Attributes attributes) throws SAXException {

      super.startElement(uri, localName, qName, attributes);

      // Create new concept and description
      if (qName.equalsIgnoreCase("term")) {
        // create and configure concept
        concept = new ConceptJpa();
        setCommonFields(concept);
        concept.setDefinitionStatusId(Long.parseLong(conceptMap.get(
            "defaultDefinitionStatus").getTerminologyId()));

        // create and configure description
        description = new DescriptionJpa();
        setCommonFields(description);
        description.setTypeId(Long.parseLong(conceptMap.get("term")
            .getTerminologyId()));
        description.setCaseSignificanceId(Long.parseLong(conceptMap.get(
            "defaultCaseSignificance").getTerminologyId()));
        description.setLanguageCode("en");
        description.setActive(true);

        // connect concept and description
        Logger.getLogger(getClass()).debug("    description = " + description);
        concept.addDescription(description);
        description.setConcept(concept);
      }
    }

    /* see superclass */
    @Override
    public void endElement(String uri, String localName, String qName)
      throws SAXException {
      try {

        // Encountered </term> - put concept into map add desc
        if (qName.equalsIgnoreCase("term")) {
          // Add the concept (if active)
          // CASCADE will handle descriptions
          if (concept.isActive() && ivd) {
            // Use the "termID" as the key
            conceptMap.put(termId, concept);
            contentService.addConcept(concept);
            Logger.getLogger(getClass()).debug("    concept = " + concept);

          }
          // reset ivd
          ivd = false;
        }

        // </termID> - set the description terminology id
        else if (qName.equalsIgnoreCase("termID")) {
          description.setTerminologyId("term-" + chars.toString().trim());
          termId = chars.toString().trim();
        }

        // </termCode> - set the concept terminology id
        else if (qName.equalsIgnoreCase("termCode")) {
          concept.setTerminologyId(chars.toString().trim());
        }

        // </termIsIVD> - add a description so we can show in the detail
        else if (qName.equalsIgnoreCase("termIsIVD")) {
          if (!chars.toString().trim().isEmpty()) {
            final Description ivdDesc = new DescriptionJpa();
            setCommonFields(ivdDesc);
            ivdDesc.setTypeId(Long.parseLong(conceptMap.get("ivdTerm")
                .getTerminologyId()));
            ivdDesc.setCaseSignificanceId(Long.parseLong(conceptMap.get(
                "defaultCaseSignificance").getTerminologyId()));
            ivdDesc.setLanguageCode("en");
            ivdDesc.setConcept(concept);
            Logger.getLogger(getClass()).debug(
                "    description = " + description);
            concept.addDescription(ivdDesc);
            ivdDesc.setActive(true);
            ivdDesc.setTerm(chars.toString().trim());

            if (chars.toString().trim().equals("IVD")) {
              ivd = true;
            }
          }

        }

        // </termName> - set the name
        else if (qName.equalsIgnoreCase("termName")) {
          concept.setDefaultPreferredName(chars.toString().trim());
          description.setTerm(chars.toString().trim());
        }

        // </termDefinition> - create and add a definition
        else if (qName.equalsIgnoreCase("termDefinition")) {
          final Description definition = new DescriptionJpa();
          setCommonFields(definition);
          definition.setTypeId(Long.parseLong(conceptMap.get("definitionTerm")
              .getTerminologyId()));
          definition.setCaseSignificanceId(Long.parseLong(conceptMap.get(
              "defaultCaseSignificance").getTerminologyId()));
          definition.setLanguageCode("en");
          definition.setConcept(concept);
          Logger.getLogger(getClass())
              .debug("    description = " + description);
          concept.addDescription(definition);
          definition.setActive(true);
          definition.setTerm(chars.toString().trim());
        }

        // </termStatus> - set active flag
        else if (qName.equalsIgnoreCase("termStatus")) {
          concept.setActive(chars.toString().trim().equals("Active"));
          for (final Description desc : concept.getDescriptions()) {
            desc.setActive(concept.isActive());
          }
        }

        // </modifiedDate> - set active flag
        else if (qName.equalsIgnoreCase("modifiedDate")) {
          concept.setEffectiveTime(dateFormat.parse(chars.toString().trim()));
          for (final Description desc : concept.getDescriptions()) {
            desc.setEffectiveTime(concept.getEffectiveTime());
          }
        }

        // </createdDate> - set active flag
        else if (qName.equalsIgnoreCase("createdDate")) {
          // nowhere to put this
        }

      } catch (Exception e) {
        throw new SAXException(e);
      }

      super.endElement(uri, localName, qName);

    }

    /* see superclass */
    @Override
    public void endDocument() throws SAXException {
      // n/a
    }

  }

  /**
   * SAX Parser handler for collectiveterm<version>.xml This parser creates
   * concepts/descriptions for collective terms.
   */
  class CollectiveTermHandler extends BaseHandler {

    /** The concept. */
    private Concept concept = null;

    /** The description. */
    private Description description = null;

    /** The term id. */
    private String termId = null;

    /**
     * Instantiates an empty {@link CollectiveTermHandler}.
     */
    public CollectiveTermHandler() {
      super();
    }

    /* see superclass */
    @Override
    public void startElement(String uri, String localName, String qName,
      Attributes attributes) throws SAXException {

      super.startElement(uri, localName, qName, attributes);

      // Create new concept and description
      if (qName.equalsIgnoreCase("collectiveterm")) {
        // create and configure concept
        concept = new ConceptJpa();
        setCommonFields(concept);
        concept.setDefinitionStatusId(Long.parseLong(conceptMap.get(
            "defaultDefinitionStatus").getTerminologyId()));

        // create and configure description
        description = new DescriptionJpa();
        setCommonFields(description);
        description.setTypeId(Long.parseLong(conceptMap.get("collectiveTerm")
            .getTerminologyId()));
        description.setCaseSignificanceId(Long.parseLong(conceptMap.get(
            "defaultCaseSignificance").getTerminologyId()));
        description.setLanguageCode("en");
        description.setActive(true);

        // connect concept and description
        Logger.getLogger(getClass()).debug("    description = " + description);
        concept.addDescription(description);
        description.setConcept(concept);
      }
    }

    /* see superclass */
    @Override
    public void endElement(String uri, String localName, String qName)
      throws SAXException {
      try {

        // Encountered </collectiveterm> - put concept into map add desc
        if (qName.equalsIgnoreCase("collectiveterm")) {
          // Add the concept i
          // CASCADE will handle descriptions
          if (concept.isActive()) {
            contentService.addConcept(concept);
            conceptMap.put(termId, concept);
          }

          Logger.getLogger(getClass()).debug("    concept = " + concept);
        }

        // </collectivetermID> - set the description terminology id
        else if (qName.equalsIgnoreCase("collectivetermID")) {
          description.setTerminologyId("ct-" + chars.toString().trim());
          termId = chars.toString().trim();
        }

        // </code> - set the concept terminology id
        else if (qName.equalsIgnoreCase("code")) {
          concept.setTerminologyId(chars.toString().trim());
        }

        // </name> - set the name
        else if (qName.equalsIgnoreCase("name")) {
          concept.setDefaultPreferredName(chars.toString().trim());
          description.setTerm(chars.toString().trim());
        }

        // </termDefinition> - create and add a definition
        else if (qName.equalsIgnoreCase("definition")) {
          final Description definition = new DescriptionJpa();
          setCommonFields(definition);
          definition.setTypeId(Long.parseLong(conceptMap.get("definitionTerm")
              .getTerminologyId()));
          definition.setCaseSignificanceId(Long.parseLong(conceptMap.get(
              "defaultCaseSignificance").getTerminologyId()));
          definition.setLanguageCode("en");
          definition.setConcept(concept);
          Logger.getLogger(getClass())
              .debug("    description = " + description);
          concept.addDescription(definition);
          definition.setActive(true);
          definition.setTerm(chars.toString().trim());
        }

        // </ctStatus> - set active flag
        else if (qName.equalsIgnoreCase("ctStatus")) {
          concept.setActive(chars.toString().trim().equals("Active"));
          for (final Description desc : concept.getDescriptions()) {
            desc.setActive(concept.isActive());
          }
        }

      } catch (Exception e) {
        throw new SAXException(e);
      }

      super.endElement(uri, localName, qName);

    }

    /* see superclass */
    @Override
    public void endDocument() throws SAXException {
      // n/a
    }

  }

  /**
   * SAX Parser handler for termcollectiveterm<version>.xml. This handler
   * creates "isa" relationships between "term" and "collectiveterm"
   */
  class TermCollectiveTermHandler extends BaseHandler {

    /** The chd id. */
    private String chdId = null;

    /** The par id. */
    private String parId = null;

    /** The chd par map. */
    private Map<String, Set<String>> parChdMap = new HashMap<>();

    /**
     * Instantiates an empty {@link TermCollectiveTermHandler}.
     */
    public TermCollectiveTermHandler() {
      super();
    }

    /* see superclass */
    @Override
    public void startElement(String uri, String localName, String qName,
      Attributes attributes) throws SAXException {

      super.startElement(uri, localName, qName, attributes);

      // Create new concept and description
      if (qName.equalsIgnoreCase("termcollectiveterm")) {
        // n/a
      }
    }

    /* see superclass */
    @Override
    public void endElement(String uri, String localName, String qName)
      throws SAXException {
      try {

        // Encountered </termcollectiveterm> - put
        if (qName.equalsIgnoreCase("termcollectiveterm")) {

          // If par and chd exist (e.g. are active), proceed
          if (conceptMap.containsKey(parId) && conceptMap.containsKey(chdId)) {
            if (!parChdMap.containsKey(parId)) {
              parChdMap.put(parId, new HashSet<String>());
            }
            parChdMap.get(parId).add(chdId);
          }
        }

        // </termID> - set the term id
        else if (qName.equalsIgnoreCase("termID")) {
          // the id
          chdId = chars.toString().trim();
        }

        // </collectivetermID> - set the description terminology id
        else if (qName.equalsIgnoreCase("collectivetermID")) {
          // the id
          parId = chars.toString().trim();
        }

      } catch (Exception e) {
        throw new SAXException(e);
      }

      super.endElement(uri, localName, qName);

    }

    /* see superclass */
    @Override
    public void endDocument() throws SAXException {
      try {
        // Handle adding relationships at the end, because we can
        // introduce intermediate levels if a level has to many
        // children
        final Set<String> origParChd = new HashSet<>(parChdMap.keySet());
        for (final String par : origParChd) {

          // If > 100, create intermediate layers
          if (parChdMap.get(par).size() > 100) {

            // Set up the index for the intermediate layer
            int idx = 0;
            // count up to 100 for each case
            int ct = 0;
            // Get original children
            final List<String> origChd = new ArrayList<>(parChdMap.get(par));
            // Sort this by code numerically
            Collections.sort(origChd, new Comparator<String>() {
              @Override
              public int compare(String o1, String o2) {
                int id1 =
                    Integer.parseInt(conceptMap.get(o1).getTerminologyId());
                int id2 =
                    Integer.parseInt(conceptMap.get(o2).getTerminologyId());
                return id1 - id2;
              }
            });

            parChdMap.put(par, new HashSet<String>());

            // Increment counter and prep for the first 100
            idx++;
            String newChd =
                par + "." + ("00" + idx).substring(("00" + idx).length() - 3);
            parChdMap.put(newChd, new HashSet<String>());
            parChdMap.get(par).add(newChd);
            String newChdStart = "START";
            String newChdEnd = null;

            // Iterate through original children
            for (final String chd : origChd) {
              // Get first start word
              if (newChdStart == null) {
                newChdStart = conceptMap.get(chd).getTerminologyId();
              }

              // Every 100 entries, create a new intermediate child
              if (++ct % 100 == 0) {
                // Get first word of the last child concept
                newChdEnd = conceptMap.get(chd).getTerminologyId();

                // add the concept - need to wait until the end
                // so we know the name of the condept
                Concept concept =
                    addIntermediateConcept(par, newChd, newChdStart, newChdEnd,
                        contentService);
                conceptMap.put(newChd, concept);

                // Increment counter and prep for the next 100
                idx++;
                newChdStart = null;
                newChd =
                    par + "."
                        + ("00" + idx).substring(("00" + idx).length() - 3);
                parChdMap.put(newChd, new HashSet<String>());
                parChdMap.get(par).add(newChd);

              }
              // Wire intermediate layer to original child
              parChdMap.get(newChd).add(chd);
            }

            // Add last concept
            if (!conceptMap.containsKey(newChd)) {
              newChdEnd = "END";

              // add the concept - need to wait until the end
              // so we know the name of the condept
              Concept concept =
                  addIntermediateConcept(par, newChd, newChdStart, newChdEnd,
                      contentService);
              conceptMap.put(newChd, concept);

            }
          }
        }

        // Now, we're ready to create relationships
        // with the revised parChdMap
        for (final String par : parChdMap.keySet()) {
          for (final String chd : parChdMap.get(par)) {

            final Concept chdConcept = conceptMap.get(chd);
            if (chdConcept == null) {
              throw new Exception("source concept is missing - " + chd);
            }

            final Concept parConcept = conceptMap.get(par);
            if (parConcept == null) {
              throw new Exception("destination concept is missing - " + par);
            }

            // Create relationship
            Logger.getLogger(getClass()).debug(
                "REL " + chd + ":" + chdConcept.getTerminologyId() + " => "
                    + par + ":" + parConcept.getTerminologyId());
            helper.createIsaRelationship(parConcept, chdConcept, "gmdn-"
                + String.valueOf(++idCt), terminology, version,
                dateFormat2.format(effectiveTime));

          }
        }
      } catch (Exception e) {
        throw new SAXException(e);
      }
    }

    /**
     * Adds the intermediate concept.
     *
     * @param par the par
     * @param newChd the new chd
     * @param newChdStart the new chd start
     * @param newChdEnd the new chd end
     * @param contentService the content service
     * @return the concept
     * @throws Exception the exception
     */
    private Concept addIntermediateConcept(String par, String newChd,
      String newChdStart, String newChdEnd, ContentService contentService)
      throws Exception {
      // Add a new concept for this
      final Concept concept = new ConceptJpa();
      setCommonFields(concept);
      concept.setDefinitionStatusId(Long.parseLong(conceptMap.get(
          "defaultDefinitionStatus").getTerminologyId()));
      concept.setTerminologyId(newChd);
      concept.setDefaultPreferredName(newChdStart + " - " + newChdEnd);

      // create and configure description
      final Description description = new DescriptionJpa();
      setCommonFields(description);
      description.setTypeId(Long.parseLong(conceptMap.get("term")
          .getTerminologyId()));
      description.setCaseSignificanceId(Long.parseLong(conceptMap.get(
          "defaultCaseSignificance").getTerminologyId()));
      description.setLanguageCode("en");
      description.setActive(true);
      description.setTerminologyId(newChd);
      description.setTerm(newChdStart + " - " + newChdEnd);

      contentService.addConcept(concept);
      Logger.getLogger(getClass()).debug("    concept = " + concept);

      return concept;
    }

  }

  /**
   * SAX Parser handler for termcollectiveterm<version>.xml. This handler
   * creates "isa" relationships between "collectiveterm" and "collectiveterm"
   */
  class CtTreeNodeHandler extends BaseHandler {

    /** Map of cttreenodeID => collectivetermID. */
    private Map<String, String> nodeTermMap = new HashMap<>();

    /** Map of cttreenodeID => parent cttreenodeID. */
    private Map<String, String> nodeChdParMap = new HashMap<>();

    /** The node id. */
    private String nodeId = null;

    /** The parent id. */
    private String parId = null;

    /** The term id. */
    private String termId = null;

    /**
     * Instantiates an empty {@link CtTreeNodeHandler}.
     */
    public CtTreeNodeHandler() {
      super();
    }

    /* see superclass */
    @Override
    public void startElement(String uri, String localName, String qName,
      Attributes attributes) throws SAXException {

      super.startElement(uri, localName, qName, attributes);

      // n/a - everything will be processed in endDocument
    }

    /* see superclass */
    @Override
    public void endElement(String uri, String localName, String qName)
      throws SAXException {
      try {

        // </cttreenodeID>
        if (qName.equalsIgnoreCase("cttreenodeID")) {
          nodeId = chars.toString().trim();
        }

        // </collectivetermID> - id of child collective term
        else if (qName.equalsIgnoreCase("collectivetermID")) {
          termId = chars.toString().trim();
        }

        // </parentnodeID> - id of the parent tree node
        else if (qName.equalsIgnoreCase("parentnodeID")) {
          parId = chars == null ? "" : chars.toString().trim();
        }

        // </cttreenode> - end tag
        else if (qName.equalsIgnoreCase("cttreenode")) {
          // Map the cttreenode
          nodeTermMap.put(nodeId, termId);

          // If chars, not a root node
          if (!parId.isEmpty()) {
            // add chd node (this) and parent node reference
            nodeChdParMap.put(nodeId, parId);
          }
          // else a root node
          else {
            final String rootTerm = nodeTermMap.get(nodeId);
            final String rootCode = conceptMap.get(rootTerm).getTerminologyId();
            Logger.getLogger(getClass()).info(
                "    ROOT = " + rootTerm
                    + (rootTerm.equals(rootCode) ? "" : ", " + rootCode));
            roots.add(rootCode);
          }
        }

      } catch (Exception e) {
        throw new SAXException(e);
      }

      super.endElement(uri, localName, qName);

    }

    /* see superclass */
    @Override
    public void endDocument() throws SAXException {
      // Contents of nodeTermMap and nodeChdParMap should be sufficient to
      // construct relationships
      for (final Map.Entry<String, String> entry : nodeChdParMap.entrySet()) {
        final String chdNode = entry.getKey();
        final String parNode = entry.getValue();

        // Create the relationship
        try {
          final String chd = nodeTermMap.get(chdNode);
          final String par = nodeTermMap.get(parNode);
          final Concept chdConcept = conceptMap.get(chd);
          final Concept parConcept = conceptMap.get(par);
          // Only if chd/par concepts are active
          if (chdConcept != null && parConcept != null) {
            Logger.getLogger(getClass()).debug(
                "REL2 " + chd + ":" + chdConcept.getTerminologyId() + " => "
                    + par + ":" + parConcept.getTerminologyId());
            helper.createIsaRelationship(
                conceptMap.get(nodeTermMap.get(parNode)),
                conceptMap.get(nodeTermMap.get(chdNode)),
                "gmdn-" + String.valueOf(++idCt), terminology, version,
                dateFormat2.format(effectiveTime));
          }
        } catch (Exception e) {
          throw new SAXException(e);
        }

      }
    }
  }

  /**
   * Sets the component common fields.
   *
   * @param component the component
   */
  void setCommonFields(Component component) {
    component.setModuleId(Long.valueOf(conceptMap.get("defaultModule")
        .getTerminologyId()));
    component.setActive(true);
    component.setTerminology(terminology);
    component.setTerminologyVersion(version);
    // An id is required due to unique constraints on components
    // make a fake id
    component.setTerminologyId(String.valueOf("gmdn-" + (++idCt)));
  }

}
