/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.algo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.ihtsdo.otf.mapping.algo.Algorithm;
import org.ihtsdo.otf.mapping.jpa.algo.helpers.Claml3MetadataHelper;
import org.ihtsdo.otf.mapping.jpa.helpers.LoggerUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.SimpleRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.ProgressListener;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Claml3LoaderAlgorithm extends RootServiceJpa implements Algorithm, AutoCloseable {

  /** Listeners. */
  private List<ProgressListener> listeners = new ArrayList<>();

  /** The request cancel flag. */
  private boolean requestCancel = false;

  /** The input file */
  private String inputFile;

  /** Name of terminology to be loaded. */
  private String terminology;

  /** The effective time. */
  private String effectiveTime;

  /** The terminology version. */
  private String terminologyVersion;

  /** Metadata counter. */
  private int metadataCounter;

  /** The content service. */
  private ContentService contentService;

  /** The helper. */
  private Claml3MetadataHelper helper;

  /** The concept map. */
  private Map<String, Concept> conceptMap;

  /** The roots. */
  private List<String> roots = new ArrayList<>();

  /** The roots. */
  private String lang = "en";

  /** Child to parent code map NOTE: this assumes a single superclass */
  private Map<String, String> chdParMap;

  /**
   * Indicates subclass relationships NOTE: this assumes a single superclass
   */
  private Map<String, Boolean> parChildrenMap;

  /** The concept usage map. */
  private Map<String, String> conceptUsageMap = new HashMap<>();

  /** The date format. */
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

  /** The log. */
  private static Logger log;

  /** The log file. */
  private File logFile;

  public Claml3LoaderAlgorithm(String terminology, String terminologyVersion, String inputFile,
    String metadataCounter)
      throws Exception {

    super();
    this.terminology = terminology;
    this.terminologyVersion = terminologyVersion;
    this.inputFile = inputFile;
    this.metadataCounter = metadataCounter == null ? 1 : Integer.parseInt(metadataCounter);

    // initialize logger
    String rootPath =
        ConfigUtility.getConfigProperties().getProperty("map.principle.source.document.dir");
    if (!rootPath.endsWith("/") && !rootPath.endsWith("\\")) {
      rootPath += "/";
    }
    rootPath += "logs";
    File logDirectory = new File(rootPath);
    if (!logDirectory.exists()) {
      logDirectory.mkdir();
    }

    logFile = new File(logDirectory, "load_" + terminology + ".log");
    LoggerUtility.setConfiguration("load", logFile.getAbsolutePath());
    this.log = LoggerUtility.getLogger("load");
  }

  public void compute() throws Exception {

    // clear log before starting process
    PrintWriter writer = new PrintWriter(logFile);
    writer.print("");
    writer.close();

    contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();

    if (!new File(inputFile).exists()) {
      throw new Exception("Specified input file does not exist " + inputFile);
    }

    // open input file and get effective time and version
    findVersion(inputFile);

    // create Metadata
    log.info("  Create metadata classes");
    helper =
        new Claml3MetadataHelper(terminology, terminologyVersion, effectiveTime, contentService);
    helper.setMetadataCounter(metadataCounter);
    conceptMap = helper.createMetadata();

    chdParMap = new HashMap<>();
    parChildrenMap = new HashMap<>();

    final File file = new File(inputFile);

    // Open XML and begin parsing
    try (FileInputStream fis = new FileInputStream(file);
        InputStream inputStream = checkForUtf8BOM(fis);
        Reader reader = new InputStreamReader(inputStream, "UTF-8");) {

      // Prep SAX parser
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      final SAXParser saxParser = factory.newSAXParser();
      final DefaultHandler handler = new LocalHandler();

      final InputSource is = new InputSource(reader);
      is.setEncoding("UTF-8");
      saxParser.parse(is, handler);

      contentService.commit();

      // creating tree positions
      // first get isaRelType from metadata
      final MetadataService metadataService = new MetadataServiceJpa();
      final Map<String, String> hierRelTypeMap =
          metadataService.getHierarchicalRelationshipTypes(terminology, terminologyVersion);

      final String isaRelType = hierRelTypeMap.keySet().iterator().next().toString();
      metadataService.close();

      // Let the service create its own transaction.
      for (final String root : roots) {
        log.info("Start creating tree positions " + root + ", " + isaRelType);
        contentService.computeTreePositions(terminology, terminologyVersion, isaRelType, root);
      }
      contentService.close();

      log.info("Done ...");

    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      throw new Exception("Conversion of Claml to RF2 objects failed", e);
    }
  }

  /**
   * Check for utf8 bom.
   * 
   * @param inputStream the input stream
   * @return the input stream
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private static InputStream checkForUtf8BOM(InputStream inputStream) throws IOException {
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
   * The SAX parser handler.
   */
  private class LocalHandler extends DefaultHandler {

    /** The chars. */
    private StringBuilder chars = new StringBuilder();

    /** The rubric kind. */
    private String rubricKind = null;

    /** The rubric id. */
    private String rubricId = null;

    /** The code. */
    private String code = null;

    /** The parent code. */
    private String parentCode = null;

    /** The class usage. */
    private String classUsage = null;

    /** The descriptor counter. */
    private int descriptorIdCounter = 1000;
    
    /** The ref set member counter. */
    private int refSetMemberCounter = 1;

    /** The current sub classes. */
    private Set<String> currentSubClasses = new HashSet<>();

    /**
     * The rels map for holding data for relationships that will be built after
     * all concepts are created.
     */
    private Map<String, Set<Concept>> relsMap = new HashMap<>();

    /** Indicates rels are needed as a result of the SuperClass tag. */
    boolean isaRelNeeded = false;

    /**
     * The concept that is currently being built from the contents of a Class
     * tag.
     */
    private Concept concept = new ConceptJpa();

    /** The rel id counter. */
    int relIdCounter = 100;

    /**
     * Tag stack.
     */
    private Stack<String> tagStack = new Stack<>();

    /**
     * Instantiates a new local handler.
     */
    public LocalHandler() {
      super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     * java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
     
      // add current tag to stack
      tagStack.push(qName.toLowerCase());

      // check if we're processing a tag inside a label tag
      if(tagStack.contains("label") && !tagStack.peek().equals("label")) {
        chars.append(labelTagBuilder(uri, localName, qName, attributes));
      }
      
      if (qName.equalsIgnoreCase("classification")) {
        lang = attributes.getValue("xml:lang");
      }

      // Encountered Class tag, save code and class usage
      if (qName.equalsIgnoreCase("class") && attributes.getValue("kind") != null
          && (attributes.getValue("kind").equalsIgnoreCase("chapter")
              || attributes.getValue("kind").equalsIgnoreCase("block")
              || attributes.getValue("kind").equalsIgnoreCase("category"))) {
        code = attributes.getValue("code");
        log.info("  Encountered class " + code);
        
        //Save chapter level codes to "roots" list, to aid in tree position creation later.
        if (attributes.getValue("kind").equalsIgnoreCase("chapter")) {
          roots.add(code);
        }
      }
      
      // Check usage tag within a class tag
      if (qName.equalsIgnoreCase("usage") && tagStack.size() > 1
          && tagStack.elementAt(tagStack.size() - 2).equalsIgnoreCase("class")) {
        classUsage = attributes.getValue("kind");
        log.info("  Encountered class usage " + classUsage + ")");
      }

      // Encountered Superclass, add parent information
      // ASSUMPTION (tested): single inheritance
      if (qName.equalsIgnoreCase("superclass")) {
        if (parentCode != null)
          throw new IllegalStateException("Multiple SuperClass entries for " + code + " = "
              + parentCode + ", " + attributes.getValue("code"));
        parentCode = attributes.getValue("code");
        isaRelNeeded = true;
        Logger.getLogger(getClass())
            .info("  Class " + (code != null ? code : "") + " has parent " + parentCode);
        parChildrenMap.put(parentCode, true);
      }

      // Encountered "Subclass", save child information
      if (qName.equalsIgnoreCase("subclass")) {
        String childCode = attributes.getValue("code");
        currentSubClasses.add(childCode);
        Logger.getLogger(getClass())
            .info("  Class " + (code != null ? code : "") + " has child " + childCode);
        parChildrenMap.put(code, true);
      }

      // Encountered Rubric, save kind (for description type) and the id
      if (qName.equalsIgnoreCase("rubric")) {
        rubricKind = attributes.getValue("kind");
        // RAW 3/13/2023 rubricIds not being generated for current use-cases of
        // CCI and ICD-10-CA
        // rubricId = attributes.getValue("id");
        rubricId = "";
        log.info("  Class " + code + " has rubric " + rubricKind + ", " + rubricId);
      }

      // Encountered label within a rubric
      if (qName.equalsIgnoreCase("label") && tagStack.size() > 1
          && tagStack.elementAt(tagStack.size() - 2).equalsIgnoreCase("rubric")) {
       
        // Clear "characters" for new label
        chars = new StringBuilder();
       
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      try {

        //TESTTEST
        if(code != null && (code.equals("X") || code.equals("10"))) {
          log.info("  Stop here!");
        }
        //ENDTESTTEST
        
        // See if we're at the end of a mid-label element
        if(tagStack.contains("label") && !tagStack.peek().equals("label")) {
          // Don't end link references - we're skipping those
          if (qName.equals("a") || localName.equals("a")) {
            //Do nothing
          }
          else {
            chars.append("</");
            // Append the qualified name or the local name
            if (!qName.isEmpty()) {
              chars.append(qName);
            } else {
              chars.append(localName);
            }
            chars.append(">");
          }
        }
        
        // Encountered </Label> while in a Class, add
        // concept/description
        if (qName.equalsIgnoreCase("label") && tagStack.contains("class") && code != null) {

          //3 cases:
          //chars does not start with < sign - then proceed with code below
          //chars starts with a p-tag and ends with a closing p-tag, and contain no inner tags - strip the beginning and end p-tag, and then process with code below
          //chars are more complicated.  In that case, just use whatever's in there (proceed with code below).
          
          //Check for <p></p> tags, and strip if present
          if(chars.toString().startsWith("<p>") && chars.toString().endsWith("</p>") && !chars.substring(3, chars.toString().length()-4).contains("<")) {
            chars.delete(0, 3);
            chars.delete(chars.length()-4,chars.length());            
          }
          
          // For the first label in the code, create the concept
          if (!conceptMap.containsKey(code)) {
            concept.setTerminologyId(code);
            concept.setEffectiveTime(dateFormat.parse(effectiveTime));
            concept.setActive(true);
            concept.setModuleId(Long.valueOf(conceptMap.get("defaultModule").getTerminologyId()));
            concept.setDefinitionStatusId(
                Long.valueOf(conceptMap.get("defaultDefinitionStatus").getTerminologyId()));
            concept.setTerminology(terminology);
            concept.setTerminologyVersion(terminologyVersion);
            concept.setDefaultPreferredName(chars.toString());
            log.debug("  Add concept " + concept.getTerminologyId() + " "
                + concept.getDefaultPreferredName());
            // Persist now, but commit at the end after all
            // descriptions are
            // added
            contentService.addConcept(concept);

            conceptMap.put(code, concept);
          }

          // Add description to concept for this rubric
          final Description desc = new DescriptionJpa();
          desc.setTerminologyId(Integer.valueOf(descriptorIdCounter++).toString());
          desc.setEffectiveTime(dateFormat.parse(effectiveTime));
          desc.setActive(true);
          desc.setModuleId(Long.valueOf(conceptMap.get("defaultModule").getTerminologyId()));
          desc.setTerminology(terminology);
          desc.setTerminologyVersion(terminologyVersion);
          if (chars.toString().getBytes("UTF-8").length >= 4000) {
            log.warn("Rubric name too long = " + chars.toString());
            desc.setTerm(chars.toString().substring(0, chars.toString().length() >= 3999 ? 3999 : chars.toString().length()));
          }
          else {
            desc.setTerm(chars.toString());
          }
          desc.setConcept(concept);
          desc.setCaseSignificanceId(
              Long.valueOf(conceptMap.get("defaultCaseSignificance").getTerminologyId()));
          desc.setLanguageCode(lang);
          log.info("  Add Description for class " + code + " - " + rubricKind + " - "
              + (desc.getTerm().replaceAll("\r", "").replaceAll("\n", "")));
          if (conceptMap.containsKey(rubricKind))
            desc.setTypeId(Long.valueOf(conceptMap.get(rubricKind).getTerminologyId()));
          else {
            throw new IllegalStateException("rubricKind not in metadata " + rubricKind);
          }
          concept.addDescription(desc);
        }

        // Encountered </Class>
        // Save info for parents/children
        if (qName.equalsIgnoreCase("class")) {

          // if relationships for this concept will be added at
          // endDocument(),
          // save relevant data now in relsMap
          if (isaRelNeeded && concept.getTerminologyId() != null) {
            log.info("  Class " + code + " has parent " + parentCode);
            Set<Concept> children = new HashSet<>();
            // check if this parentCode already has children
            if (relsMap.containsKey(parentCode + ":" + "isa")) {
              children = relsMap.get(parentCode + ":" + "isa");
            }
            children.add(concept);
            relsMap.put(parentCode + ":" + "isa", children);
            for (final Concept child : children) {
              chdParMap.put(child.getTerminologyId(), parentCode);
            }
            parChildrenMap.put(parentCode, true);

          }

          // Record class level dagger/asterisk info as refset member
          if (classUsage != null) {
            log.info("  Class " + code + " has usage " + classUsage);
            log.info("    id = " + concept.getId());
            final SimpleRefSetMember member = new SimpleRefSetMemberJpa();
            member.setConcept(concept);
            member.setActive(true);
            member.setEffectiveTime(dateFormat.parse(effectiveTime));
            member.setModuleId(Long.valueOf(conceptMap.get("defaultModule").getTerminologyId()));
            member.setTerminology(terminology);
            member.setTerminologyId(Integer.valueOf(refSetMemberCounter++).toString());
            member.setTerminologyVersion(terminologyVersion);
            member.setRefSetId(conceptMap.get(classUsage).getTerminologyId());
            concept.addSimpleRefSetMember(member);
            if (concept.getId() != null) {
              // Add member
              contentService.addSimpleRefSetMember(member);
              conceptUsageMap.put(concept.getTerminologyId(), classUsage);
            }
          }

          // reset variables at the end of each
          // Class, Modifier, or ModifierClass
          code = null;
          parentCode = null;
          rubricKind = null;
          rubricId = null;
          concept = new ConceptJpa();
          currentSubClasses = new HashSet<>();
          classUsage = null;
          isaRelNeeded = false;
        }

      } catch (Exception e) {
        throw new SAXException(e);
      }

      // pop tag stack and clear characters
      tagStack.pop();
      //Don't clear out chars - we handle this in startElement
      //chars = new StringBuilder();

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

    
    public String labelTagBuilder(String uri, String localName, String qName, Attributes attributes) {
      StringBuilder tagBuilder = new StringBuilder();
      
      // Don't append link references
      if (qName.equals("a") || localName.equals("a")) {
        return "";
      }
      
      tagBuilder.append("<");

      // Append the qualified name or the local name
      if (!qName.isEmpty()) {
          tagBuilder.append(qName);
      } else {
          tagBuilder.append(localName);
      }

      // Append the Namespace URI, if present
      if (!uri.isEmpty()) {
          tagBuilder.append(" xmlns=\"").append(uri).append("\"");
      }

      // Append the attributes, if any
      for (int i = 0; i < attributes.getLength(); i++) {
          String attrQName = attributes.getQName(i);
          String attrValue = attributes.getValue(i);

          tagBuilder.append(" ").append(attrQName).append("=\"").append(attrValue).append("\"");
      }

      tagBuilder.append(">");
      
      return tagBuilder.toString();
    }
    
    
    /*
     * (non-Javadoc)
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException {
      // Add relationships now that all concepts have been created
      final Map<String, Integer> relDisambiguation = new HashMap<>();

      try {
        for (final Map.Entry<String, Set<Concept>> mapEntry : relsMap.entrySet()) {

          final String key = mapEntry.getKey();
          final String tokens[] = key.split(":");
          String parentCode = null;
          String id = null;
          String type = null;
          String label = null;

          // handle reference case
          if (tokens.length == 4) {
            parentCode = tokens[0];
            type = tokens[2];
            id = tokens[1];
            label = tokens[3];
            if (relDisambiguation.containsKey(id)) {
              int ct = relDisambiguation.get(id);
              ct++;
              relDisambiguation.put(id, ct);
              id = id + "~" + ct;
            } else {
              relDisambiguation.put(id, 1);
              id = id + "~1";
            }
            // tokens[3]; -- nothing to do with tokens[3] at this
            // point
          }

          // handle isa case
          else if (tokens.length == 2) {
            parentCode = tokens[0];
            type = tokens[1];
          }

          // fail otherwise
          else {
            throw new SAXException(
                "Unexpected number of tokens for relsMap entry " + tokens.length);
          }
          if (type.equals("aster"))
            type = "dagger-to-asterisk";
          if (type.equals("dagger"))
            type = "asterisk-to-dagger";
          for (final Concept childConcept : mapEntry.getValue()) {
            log.info("  Create Relationship " + childConcept.getTerminologyId() + " " + type + " "
                + parentCode + " " + id);
            if (conceptMap.containsKey(parentCode)) {
              final Relationship relationship = new RelationshipJpa();
              // For reference, use the provided id
              if (id != null) {
                relationship.setTerminologyId(id);
              }
              // otherwise, make a new id
              else {
                relationship.setTerminologyId(Integer.valueOf(relIdCounter++).toString());
              }
              relationship.setEffectiveTime(dateFormat.parse(effectiveTime));
              relationship.setActive(true);
              relationship
                  .setModuleId(Long.valueOf(conceptMap.get("defaultModule").getTerminologyId()));
              relationship.setTerminology(terminology);
              relationship.setTerminologyVersion(terminologyVersion);
              relationship.setCharacteristicTypeId(
                  Long.valueOf(conceptMap.get("defaultCharacteristicType").getTerminologyId()));
              relationship.setModifierId(
                  Long.valueOf(conceptMap.get("defaultModifier").getTerminologyId()));
              relationship.setDestinationConcept(conceptMap.get(parentCode));
              relationship.setSourceConcept(childConcept);
              if (!conceptMap.containsKey(type))
                throw new IllegalStateException("type not in conceptMap " + type);
              relationship.setTypeId(Long.valueOf(conceptMap.get(type).getTerminologyId()));
              relationship.setRelationshipGroup(Integer.valueOf(0));
              relationship.setLabel(label);
              Set<Relationship> rels = new HashSet<>();
              if (childConcept.getRelationships() != null)
                rels = childConcept.getRelationships();
              rels.add(relationship);
              childConcept.setRelationships(rels);

            } else {
              // throw new SAXException("Problem inserting
              // relationship, code "
              // + parentCode + " does not exist.");
              log.info("    WARNING rel to illegal concept");
            }
          }
        }

      } catch (ParseException e) {
        throw new SAXException(e);
      }
    }
  }

  /**
   * Find version.
   * 
   * @param inputFile
   * 
   * @throws Exception the exception
   */
  public void findVersion(String inputFile) throws Exception {

    BufferedReader br = new BufferedReader(new FileReader(inputFile));
    String line = null;
    if (terminologyVersion == null) {
      while ((line = br.readLine()) != null) {
        if (line.contains("<Meta name=\"year\"")) {
          terminologyVersion = line.replaceFirst(".*value=\"", "").replaceFirst("\".*", "");
          break;
        }
      }
    }

    br.close();
    log.info("terminologyVersion: " + terminologyVersion);

    try {
      dateFormat.parse(effectiveTime);
      log.info("effectiveTime: " + effectiveTime);
    } catch (Exception e) {
      // just use today
      effectiveTime = dateFormat.format(new Date());
      log.info("effectiveTime: " + effectiveTime);
    }
  }

  @Override
  public void addProgressListener(ProgressListener l) {
    listeners.add(l);
  }

  @Override
  public void removeProgressListener(ProgressListener l) {
    listeners.remove(l);
  }

  @Override
  public void reset() throws Exception {
    // n/a
  }

  @Override
  public void checkPreconditions() throws Exception {
    // n/a
  }

  @Override
  public void cancel() throws Exception {
    requestCancel = true;
  }

}
