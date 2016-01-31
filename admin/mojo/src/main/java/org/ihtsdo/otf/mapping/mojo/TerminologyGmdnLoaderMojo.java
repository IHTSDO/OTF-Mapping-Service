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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;
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
          // Add the concept
          // CASCADE will handle descriptions
          Logger.getLogger(getClass()).debug("    concept = " + concept);
          contentService.addConcept(concept);
        }

        // </termID> - set the description terminology id
        else if (qName.equalsIgnoreCase("termID")) {
          description.setTerminologyId("term-" + chars.toString().trim());

          // Use the "termID" as the key
          conceptMap.put(chars.toString().trim(), concept);
        }

        // </termCode> - set the concept terminology id
        else if (qName.equalsIgnoreCase("termCode")) {
          concept.setTerminologyId(chars.toString().trim());
        }

        // </termIsIVD> - add a description so we can show in the detail
        else if (qName.equalsIgnoreCase("termIsIVD")) {
          final Description ivd = new DescriptionJpa();
          setCommonFields(ivd);
          ivd.setTypeId(Long.parseLong(conceptMap.get("ivdTerm")
              .getTerminologyId()));
          ivd.setCaseSignificanceId(Long.parseLong(conceptMap.get(
              "defaultCaseSignificance").getTerminologyId()));
          ivd.setLanguageCode("en");
          ivd.setConcept(concept);
          Logger.getLogger(getClass())
              .debug("    description = " + description);
          concept.addDescription(ivd);
          ivd.setActive(true);
          ivd.setTerm(chars.toString().trim());
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
          // Add the concept
          // CASCADE will handle descriptions
          Logger.getLogger(getClass()).debug("    concept = " + concept);
          contentService.addConcept(concept);
        }

        // </collectivetermID> - set the description terminology id
        else if (qName.equalsIgnoreCase("collectivetermID")) {
          description.setTerminologyId("ct-" + chars.toString().trim());

          // Ue the "collectiveTermID" as the key
          conceptMap.put(chars.toString().trim(), concept);
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

    /** The relationship. */
    private Relationship relationship = null;

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
        // create and configure rel
        relationship = new RelationshipJpa();
        setCommonFields(relationship);
        relationship.setActive(true);
        relationship.setCharacteristicTypeId(Long.parseLong(conceptMap.get(
            "defaultCharacteristicType").getTerminologyId()));
        relationship.setModifierId(Long.parseLong(conceptMap.get(
            "defaultModifier").getTerminologyId()));
        relationship.setRelationshipGroup(null);
        relationship.setTypeId(Long.parseLong(conceptMap.get("isa")
            .getTerminologyId()));
      }
    }

    /* see superclass */
    @Override
    public void endElement(String uri, String localName, String qName)
      throws SAXException {
      try {

        // Encountered </collectiveterm> - put concept into map add desc
        if (qName.equalsIgnoreCase("termcollectiveterm")) {
          // Add the relationship
          // the concepts on either end must already be added
          Logger.getLogger(getClass()).debug("    rel = " + relationship);
          contentService.addRelationship(relationship);
        }

        // </termID> - set the term id
        else if (qName.equalsIgnoreCase("termID")) {
          final Concept source = conceptMap.get(chars.toString().trim());
          if (source == null) {
            throw new Exception("source concept is missing - "
                + chars.toString().trim());
          }
          relationship.setSourceConcept(source);
        }

        // </collectivetermID> - set the description terminology id
        else if (qName.equalsIgnoreCase("collectivetermID")) {
          final Concept destination = conceptMap.get(chars.toString().trim());
          if (destination == null) {
            throw new Exception("destination concept is missing - "
                + chars.toString().trim());
          }
          relationship.setDestinationConcept(conceptMap.get(chars.toString()
              .trim()));
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
   * creates "isa" relationships between "collectiveterm" and "collectiveterm"
   */
  class CtTreeNodeHandler extends BaseHandler {

    /** Map of cttreenodeID => collectivetermID. */
    private Map<String, String> nodeTermMap = new HashMap<>();

    /** Map of cttreenodeID => parent cttreenodeID. */
    private Map<String, String> nodeChdParMap = new HashMap<>();

    /** The cttreenode id. */
    private String cttreenodeID = null;

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
          cttreenodeID = chars.toString().trim();
        }

        // </collectivetermID> - id of child collective term
        else if (qName.equalsIgnoreCase("collectivetermID")) {
          // Map the cttreenode
          nodeTermMap.put(cttreenodeID, chars.toString().trim());
        }

        // </parentnodeID> - id of the parent tree node
        else if (qName.equalsIgnoreCase("parentnodeID")) {
          // If chars, not a root node
          if (chars != null && !chars.toString().trim().isEmpty()) {
            // add chd node (this) and parent node reference
            nodeChdParMap.put(cttreenodeID, chars.toString().trim());
          }
          // else a root node
          else {
            final String rootTerm = nodeTermMap.get(cttreenodeID);
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
          helper.createIsaRelationship(
              conceptMap.get(nodeTermMap.get(parNode)),
              conceptMap.get(nodeTermMap.get(chdNode)),
              "gmdn-" + String.valueOf(++idCt), terminology, version,
              dateFormat2.format(effectiveTime));
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
    component.setTerminology(terminology);
    component.setTerminologyVersion(version);
    // An id is required due to unique constraints on components
    // make a fake id
    component.setTerminologyId(String.valueOf("gmdn-" + (++idCt)));
  }

}
