package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.ClamlMetadataHelper;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
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
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Converts claml data to RF2 objects.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-claml
 * @phase package
 */
public class TerminologyClamlLoaderMojo extends AbstractMojo {

  /** The date format. */
  final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

  /**
   * The input file
   * @parameter
   * @required
   */
  private String inputFile;

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
   * Terminology version
   * @parameter
   * @required
   */
  String version;

  /** The effective time. */
  String effectiveTime;

  /** The terminology version. */
  String terminologyVersion;

  /** The concept map. */
  Map<String, Concept> conceptMap;

  /** The roots. */
  List<String> roots = null;

  /** The content service. */
  ContentService contentService;

  /** Child to parent code map NOTE: this assumes a single superclass */
  Map<String, String> chdParMap;

  /** Indicates subclass relationships NOTE: this assumes a single superclass */
  Map<String, Boolean> parChildrenMap;

  /** The concept usage map. */
  Map<String, String> conceptUsageMap = new HashMap<>();

  /** The helper. */
  ClamlMetadataHelper helper;

  /**
   * Executes the plugin.
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting loading Claml terminology");
    getLog().info("  inputFile = inputFile");
    getLog().info("  terminology = " + terminology);
    getLog().info("  version = " + version);

    FileInputStream fis = null;
    InputStream inputStream = null;
    Reader reader = null;
    try {

      contentService = new ContentServiceJpa();
      contentService.setTransactionPerOperation(false);
      contentService.beginTransaction();

      if (!new File(inputFile).exists()) {
        throw new MojoFailureException("Specified input file does not exist");
      }

      // open input file and get effective time and version
      findVersion(inputFile);

      // create Metadata
      getLog().info("  Create metadata classes");
      helper =
          new ClamlMetadataHelper(terminology, terminologyVersion,
              effectiveTime, contentService);
      conceptMap = helper.createMetadata();

      chdParMap = new HashMap<>();
      parChildrenMap = new HashMap<>();

      // Prep SAX parser
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      final SAXParser saxParser = factory.newSAXParser();
      final DefaultHandler handler = new LocalHandler();

      // Open XML and begin parsing
      final File file = new File(inputFile);
      fis = new FileInputStream(file);
      inputStream = checkForUtf8BOM(fis);
      reader = new InputStreamReader(inputStream, "UTF-8");
      final InputSource is = new InputSource(reader);
      is.setEncoding("UTF-8");
      saxParser.parse(is, handler);

      contentService.commit();

      // creating tree positions
      // first get isaRelType from metadata
      final MetadataService metadataService = new MetadataServiceJpa();
      final Map<String, String> hierRelTypeMap =
          metadataService.getHierarchicalRelationshipTypes(terminology,
              terminologyVersion);
      final String isaRelType =
          hierRelTypeMap.keySet().iterator().next().toString();
      metadataService.close();

      // Let the service create its own transaction.
      for (final String root : roots) {
        getLog().info(
            "Start creating tree positions " + root + ", " + isaRelType);
        contentService.computeTreePositions(terminology, terminologyVersion,
            isaRelType, root);
      }
      contentService.close();

      getLog().info("Done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Conversion of Claml to RF2 objects failed", e);
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
   * The SAX parser handler.
   */
  class LocalHandler extends DefaultHandler {

    /** The chars. */
    private StringBuilder chars = new StringBuilder();

    /** The label chars - used for description text. */
    private StringBuilder labelChars = new StringBuilder();

    /** The rubric kind. */
    private String rubricKind = null;

    /** The rubric id. */
    private String rubricId = null;

    /** The code. */
    private String code = null;

    /** The parent code. */
    private String parentCode = null;

    /** The modifier code. */
    private String modifierCode = null;

    /** The modifier. */
    private String modifier = null;

    /** The class usage. */
    private String classUsage = null;

    /** The reference usage. */
    private String referenceUsage = null;

    /** The reference code. */
    private String referenceCode = null;

    /** The ref set member counter. */
    private int refSetMemberCounter = 1;

    /** The reference indicating a non-isa relationship. */
    private String reference = null;

    /** The current sub classes. */
    private Set<String> currentSubClasses = new HashSet<>();

    /**
     * This is a code => modifier map. The modifier must then be looked up in
     * modifier map to determine the code extensions and template concepts
     * associated with it.
     */
    private Map<String, List<String>> classToModifierMap = new HashMap<>();

    /**
     * This is a code => modifier map. If a code is modified but also blocked by
     * an entry in here, do not make children from the template classes.
     */
    private Map<String, List<String>> classToExcludedModifierMap =
        new HashMap<>();

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

    /** The modifier map. */
    private Map<String, Map<String, Concept>> modifierMap = new HashMap<>();

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
    public void startElement(String uri, String localName, String qName,
      Attributes attributes) throws SAXException {

      // add current tag to stack
      tagStack.push(qName.toLowerCase());

      if (qName.equalsIgnoreCase("meta")) {
        // e.g. <Meta name="TopLevelSort"
        // value="- A B D F H K L N P R S T U W X Y Z"/>
        final String name = attributes.getValue("name");
        if (name != null && name.equalsIgnoreCase("toplevelsort")) {
          String value = attributes.getValue("value");
          roots = new ArrayList<>();
          for (final String code : value.split(" ")) {
            getLog().info("  Adding root: " + code.trim());
            roots.add(code.trim());
          }
        }
        if (roots.size() == 0)
          throw new IllegalStateException("No roots found");
      }
      // Encountered Class tag, save code and class usage
      if (qName.equalsIgnoreCase("class")) {
        code = attributes.getValue("code");
        classUsage = attributes.getValue("usage");
        getLog().info(
            "  Encountered class " + code + " "
                + (classUsage == null ? "" : "(" + classUsage + ")"));
      }

      // Encountered Modifier tag, save code and class usage
      if (qName.equalsIgnoreCase("modifier")) {
        code = attributes.getValue("code");
        classUsage = attributes.getValue("usage");
        getLog().info(
            "  Encountered modifier " + code + " "
                + (classUsage == null ? "" : "(" + classUsage + ")"));
      }

      // Encountered ModifierClass, save modifier, modifierCode and class usage
      if (qName.equalsIgnoreCase("modifierclass")) {
        modifier = attributes.getValue("modifier");
        modifierCode = attributes.getValue("code");

        //
        // CLAML FIXER - ICD10 is broken, fix it here
        //
        if (modifier.endsWith("_4") && !modifierCode.startsWith(".")) {
          getLog().info("  FIXING broken code, adding . to _4 code");
          modifierCode = "." + modifierCode;
        }
        if (modifier.endsWith("_5") && modifierCode.startsWith(".")) {
          getLog().info("  FIXING broken code, removing . from _5 code");
          modifierCode = modifierCode.substring(1);
        }
        classUsage = attributes.getValue("usage");
        getLog().info(
            "  Encountered modifierClass " + modifierCode + " for " + modifier
                + " " + (classUsage == null ? "" : "(" + classUsage + ")"));
      }

      // Encountered Superclass, add parent information
      // ASSUMPTION (tested): single inheritance
      if (qName.equalsIgnoreCase("superclass")) {
        if (parentCode != null)
          throw new IllegalStateException("Multiple SuperClass entries for "
              + code + " = " + parentCode + ", " + attributes.getValue("code"));
        parentCode = attributes.getValue("code");
        isaRelNeeded = true;
        getLog().info(
            "  Class "
                + (code != null ? code : (modifier + ":" + modifierCode))
                + " has parent " + parentCode);
        parChildrenMap.put(parentCode, true);
      }

      // Encountered "Subclass", save child information
      if (qName.equalsIgnoreCase("subclass")) {
        String childCode = attributes.getValue("code");
        currentSubClasses.add(childCode);
        getLog().info(
            "  Class "
                + (code != null ? code : (modifier + ":" + modifierCode))
                + " has child " + childCode);
        parChildrenMap.put(code, true);
      }

      // Encountered ModifiedBy, save modifier code information
      if (qName.equalsIgnoreCase("modifiedby")) {
        final String modifiedByCode = attributes.getValue("code");
        getLog().info("  Class " + code + " modified by " + modifiedByCode);
        List<String> currentModifiers = new ArrayList<>();
        if (classToModifierMap.containsKey(code)) {
          currentModifiers = classToModifierMap.get(code);
        }
        currentModifiers.add(modifiedByCode);
        classToModifierMap.put(code, currentModifiers);
      }

      // Encountered ExcludeModifier, save excluded modifier code information
      if (qName.equalsIgnoreCase("excludemodifier")) {
        final String excludeModifierCode = attributes.getValue("code");
        getLog().info(
            "  Class and subclasses of " + code + " exclude modifier "
                + excludeModifierCode);
        List<String> currentModifiers = new ArrayList<>();
        if (classToExcludedModifierMap.containsKey(code)) {
          currentModifiers = classToExcludedModifierMap.get(code);
        }
        currentModifiers.add(excludeModifierCode);
        classToExcludedModifierMap.put(code, currentModifiers);

        // If the code contains a dash (-) we need to generate
        // all of the codes in the range
        if (code.indexOf("-") != -1) {
          final String[] startEnd = code.split("-");
          char letterStart = startEnd[0].charAt(0);
          char letterEnd = startEnd[1].charAt(0);
          int start = Integer.parseInt(startEnd[0].substring(1));
          int end = Integer.parseInt(startEnd[1].substring(1));
          for (char c = letterStart; c <= letterEnd; c++) {
            for (int i = start; i <= end; i++) {
              final String padI = "0000000000" + i;
              final String code =
                  c
                      + padI.substring(
                          padI.length() - startEnd[0].length() + 1,
                          padI.length());
              getLog().info(
                  "  Class and subclasses of " + code + " exclude modifier "
                      + excludeModifierCode);
              currentModifiers = new ArrayList<>();
              if (classToExcludedModifierMap.containsKey(code)) {
                currentModifiers = classToExcludedModifierMap.get(code);
              }
              currentModifiers.add(excludeModifierCode);
              classToExcludedModifierMap.put(code, currentModifiers);
            }
          }
        }

      }

      // Encountered Rubric, save kind (for description type) and the id
      if (qName.equalsIgnoreCase("rubric")) {
        rubricKind = attributes.getValue("kind");
        rubricId = attributes.getValue("id");
        getLog().info(
            "  Class " + code + " has rubric " + rubricKind + ", " + rubricId);
      }

      // Encountered Reference, append label chars and save usage
      if (qName.equalsIgnoreCase("reference")) {

        // add label chars if within a label tag
        if (tagStack.contains("label")) {
          // Append a space if we've already seen earlier fragments
          if (labelChars.length() != 0 && chars.toString().trim().length() > 0) {
            labelChars.append(" ");
          }
          labelChars.append(chars.toString().trim());
        } else {
          throw new SAXException(
              "Unexpected place to find reference -- not in label tag");
        }
        // Clear "characters"
        chars = new StringBuilder();

        // Save reference usage
        referenceUsage = attributes.getValue("usage");
        // the referenceCode is used when the value in the Reference tag
        // doesn't actually resolve to a code. We need this because it is
        // what we will ACTUALLY connect the relationship to
        referenceCode = attributes.getValue("code");
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
      try {

        // Encountered </Fragment>, append label characters
        if (qName.equalsIgnoreCase("fragment")) {
          // Append a space if we've already seen earlier fragments
          if (labelChars.length() != 0) {
            labelChars.append(" ");
          }
          labelChars.append(chars.toString().trim());
        }

        // Encountered </Para>, append label characters
        if (qName.equalsIgnoreCase("para")) {
          // Append a space if we've already seen earlier fragments
          if (labelChars.length() != 0 && chars.toString().trim().length() > 0) {
            labelChars.append(" ");
          }
          labelChars.append(chars.toString().trim());
        }

        // Encountered </Label> while in a Modifier
        // adding concept/preferred description for modifier class
        // NOTE: non-preferred descriptions still need to be added
        // to modified classes
        if (qName.equalsIgnoreCase("label") && modifierCode != null) {
          // Append a space if we've already seen earlier fragments
          if (labelChars.length() != 0 && chars.toString().trim().length() > 0) {
            labelChars.append(" ");
          }
          // Pick up any characters in the label tag
          labelChars.append(chars.toString().trim());
          addModifierClass();
          // reset label characters
          labelChars = new StringBuilder();
        }

        // Encountered </Label> while in a Class, add concept/description
        if (qName.equalsIgnoreCase("label") && tagStack.contains("class")) {

          // Append a space if we've already seen earlier fragments
          if (labelChars.length() != 0 && chars.toString().trim().length() > 0) {
            labelChars.append(" ");
          }
          // Pick up any characters in the label tag
          labelChars.append(chars.toString().trim());

          // For the first label in the code, create the concept
          if (!conceptMap.containsKey(code)) {
            concept.setTerminologyId(code);
            concept.setEffectiveTime(dateFormat.parse(effectiveTime));
            concept.setActive(true);
            concept.setModuleId(new Long(conceptMap.get("defaultModule")
                .getTerminologyId()));
            concept.setDefinitionStatusId(new Long(conceptMap.get(
                "defaultDefinitionStatus").getTerminologyId()));
            concept.setTerminology(terminology);
            concept.setTerminologyVersion(terminologyVersion);
            concept.setDefaultPreferredName(labelChars.toString());
            getLog().debug(
                "  Add concept " + concept.getTerminologyId() + " "
                    + concept.getDefaultPreferredName());
            // Persist now, but commit at the end after all descriptions are
            // added
            contentService.addConcept(concept);

            conceptMap.put(code, concept);
          }

          // Add description to concept for this rubric
          final Description desc = new DescriptionJpa();
          desc.setTerminologyId(rubricId);
          desc.setEffectiveTime(dateFormat.parse(effectiveTime));
          desc.setActive(true);
          desc.setModuleId(new Long(conceptMap.get("defaultModule")
              .getTerminologyId()));
          desc.setTerminology(terminology);
          desc.setTerminologyVersion(terminologyVersion);
          desc.setTerm(labelChars.toString());
          desc.setConcept(concept);
          desc.setCaseSignificanceId(new Long(conceptMap.get(
              "defaultCaseSignificance").getTerminologyId()));
          desc.setLanguageCode("en");
          getLog().info(
              "  Add Description for class " + code + " - " + rubricKind
                  + " - "
                  + (desc.getTerm().replaceAll("\r", "").replaceAll("\n", "")));
          if (conceptMap.containsKey(rubricKind))
            desc.setTypeId(new Long(conceptMap.get(rubricKind)
                .getTerminologyId()));
          else {
            throw new IllegalStateException("rubricKind not in metadata "
                + rubricKind);
          }
          concept.addDescription(desc);

          // reset label characters
          labelChars = new StringBuilder();
        }

        // Encountered </Label> while in a Class, add concept/description
        if (qName.equalsIgnoreCase("label") && tagStack.contains("modifier")) {
          // reset label characters
          labelChars = new StringBuilder();
        }

        // Encountered </Reference>, create info for later relationship creation
        if (qName.equalsIgnoreCase("reference")) {
          // relationships for this concept will be added at endDocument(),
          // save relevant data now in relsMap
          reference = chars.toString();
          getLog().info(
              "  Class " + code + " has reference to " + reference + " "
                  + (referenceUsage == null ? "" : "(" + referenceUsage + ")"));

          // If not "dagger" or "aster", it's just a normal reference
          if (referenceUsage == null) {
            referenceUsage = "reference";
          }

          // IF the reference tag didn't have a code attribute, use the text
          // instead
          if (referenceCode == null) {
            referenceCode = reference;
          }
          String key =
              referenceCode + ":" + rubricId + ":" + referenceUsage + ":"
                  + reference;
          // check assumption: key is unique
          if (relsMap.containsKey(key)) {
            throw new Exception("Rels key already exists: " + key);
          }
          // because of checking assumption, will never have >1 in the set
          Set<Concept> concepts = new HashSet<>();
          concepts.add(concept);
          relsMap.put(key, concepts);
        }

        // Encountered </ModifierClass>
        // Add the template concept to the map for this
        // ModifierClass's code (e.g. ".1" => template concept)
        // Add that to the overall map for the corresponding modifier
        if (qName.equalsIgnoreCase("modifierclass")) {
          Map<String, Concept> modifierCodeToClassMap = new HashMap<>();
          if (modifierMap.containsKey(modifier)) {
            modifierCodeToClassMap = modifierMap.get(modifier);
          }
          modifierCodeToClassMap.put(modifierCode, concept);
          modifierMap.put(modifier, modifierCodeToClassMap);
          getLog().info(
              "  Modifier " + modifier + " needs template class for "
                  + modifierCode);
        }

        // Encountered </Class> or </Modifier> or </ModifierClass>
        // Save info for parents/children
        if (qName.equalsIgnoreCase("class")
            || qName.equalsIgnoreCase("modifier")
            || qName.equalsIgnoreCase("modifierclass")) {

          // if relationships for this concept will be added at endDocument(),
          // save relevant data now in relsMap
          if (isaRelNeeded && concept.getTerminologyId() != null) {
            getLog().info("  Class " + code + " has parent " + parentCode);
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
            getLog().info("  Class " + code + " has usage " + classUsage);
            getLog().info("    id = " + concept.getId());
            final SimpleRefSetMember member = new SimpleRefSetMemberJpa();
            member.setConcept(concept);
            member.setActive(true);
            member.setEffectiveTime(dateFormat.parse(effectiveTime));
            member.setModuleId(new Long(conceptMap.get("defaultModule")
                .getTerminologyId()));
            member.setTerminology(terminology);
            member.setTerminologyId(new Integer(refSetMemberCounter++)
                .toString());
            member.setTerminologyVersion(terminologyVersion);
            member.setRefSetId(conceptMap.get(classUsage).getTerminologyId());
            concept.addSimpleRefSetMember(member);
            if (concept.getId() != null) {
              // Add member
              contentService.addSimpleRefSetMember(member);
              System.out.println("X = " + concept.getTerminologyId() + ", "
                  + classUsage);
              conceptUsageMap.put(concept.getTerminologyId(), classUsage);
            }
          }

          // If concept indicates modifiedby tag, add related children
          // also check subClassToModifierMap to see if
          // modifiers need to be created for this concept
          if (qName.equalsIgnoreCase("class") && code.indexOf("-") == -1) {
            modifierHelper(code);
          }

          // reset variables at the end of each
          // Class, Modifier, or ModifierClass
          code = null;
          parentCode = null;
          modifierCode = null;
          modifier = null;
          rubricKind = null;
          rubricId = null;
          concept = new ConceptJpa();
          currentSubClasses = new HashSet<>();
          classUsage = null;
          referenceUsage = null;
          isaRelNeeded = false;
        }

      } catch (Exception e) {
        throw new SAXException(e);
      }

      // pop tag stack and clear characters
      tagStack.pop();
      chars = new StringBuilder();

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
        for (final Map.Entry<String, Set<Concept>> mapEntry : relsMap
            .entrySet()) {

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
            // tokens[3]; -- nothing to do with tokens[3] at this point
          }

          // handle isa case
          else if (tokens.length == 2) {
            parentCode = tokens[0];
            type = tokens[1];
          }

          // fail otherwise
          else {
            throw new SAXException(
                "Unexpected number of tokens for relsMap entry "
                    + tokens.length);
          }
          if (type.equals("aster"))
            type = "dagger-to-asterisk";
          if (type.equals("dagger"))
            type = "asterisk-to-dagger";
          for (final Concept childConcept : mapEntry.getValue()) {
            getLog().info(
                "  Create Relationship " + childConcept.getTerminologyId()
                    + " " + type + " " + parentCode + " " + id);
            if (conceptMap.containsKey(parentCode)) {
              final Relationship relationship = new RelationshipJpa();
              // For reference, use the provided id
              if (id != null) {
                relationship.setTerminologyId(id);
              }
              // otherwise, make a new id
              else {
                relationship.setTerminologyId(new Integer(relIdCounter++)
                    .toString());
              }
              relationship.setEffectiveTime(dateFormat.parse(effectiveTime));
              relationship.setActive(true);
              relationship.setModuleId(new Long(conceptMap.get("defaultModule")
                  .getTerminologyId()));
              relationship.setTerminology(terminology);
              relationship.setTerminologyVersion(terminologyVersion);
              relationship.setCharacteristicTypeId(new Long(conceptMap.get(
                  "defaultCharacteristicType").getTerminologyId()));
              relationship.setModifierId(new Long(conceptMap.get(
                  "defaultModifier").getTerminologyId()));
              relationship.setDestinationConcept(conceptMap.get(parentCode));
              relationship.setSourceConcept(childConcept);
              if (!conceptMap.containsKey(type))
                throw new IllegalStateException("type not in conceptMap "
                    + type);
              relationship.setTypeId(new Long(conceptMap.get(type)
                  .getTerminologyId()));
              relationship.setRelationshipGroup(new Integer(0));
              relationship.setLabel(label);
              Set<Relationship> rels = new HashSet<>();
              if (childConcept.getRelationships() != null)
                rels = childConcept.getRelationships();
              rels.add(relationship);
              childConcept.setRelationships(rels);

            } else if (modifierMap.containsKey(parentCode)) {
              getLog().info("    IGNORE rel to modifier");
            } else {
              // throw new SAXException("Problem inserting relationship, code "
              // + parentCode + " does not exist.");
              getLog().info("    WARNING rel to illegal concept");
            }
          }
        }

      } catch (ParseException e) {
        throw new SAXException(e);
      }
    }

    /**
     * Adds the modifier class.
     * 
     * @throws Exception the exception
     */
    public void addModifierClass() throws Exception {

      // create concept if it doesn't exist
      String code = modifier + modifierCode;
      if (!conceptMap.containsKey(code)) {
        concept.setTerminologyId(modifier + modifierCode);
        concept.setEffectiveTime(dateFormat.parse(effectiveTime));
        concept.setActive(true);
        concept.setModuleId(new Long(conceptMap.get("defaultModule")
            .getTerminologyId()));
        concept.setDefinitionStatusId(new Long(conceptMap.get(
            "defaultDefinitionStatus").getTerminologyId()));
        concept.setTerminology(terminology);
        concept.setTerminologyVersion(terminologyVersion);
        concept.setDefaultPreferredName(labelChars.toString());
        getLog().info(
            "  Add modifier concept " + concept.getTerminologyId() + " "
                + concept.getDefaultPreferredName());
        // NOTE: we don't persist these modifier classes, the
        // classes they generate get added during modifierHelper
        conceptMap.put(code, concept);
      }

      // add description to concept
      final Description desc = new DescriptionJpa();
      desc.setTerminologyId(rubricId);
      desc.setEffectiveTime(dateFormat.parse(effectiveTime));
      desc.setActive(true);
      desc.setModuleId(new Long(conceptMap.get("defaultModule")
          .getTerminologyId()));
      desc.setTerminology(terminology);
      desc.setTerminologyVersion(terminologyVersion);
      desc.setTerm(chars.toString());
      desc.setConcept(concept);
      desc.setCaseSignificanceId(new Long(conceptMap.get(
          "defaultCaseSignificance").getTerminologyId()));
      desc.setLanguageCode("en");
      desc.setTypeId(new Long(conceptMap.get(rubricKind).getTerminologyId()));

      concept.addDescription(desc);
    }

    /**
     * Handle generating new concepts based on modifiers.
     * @param codeToModify
     * 
     * @throws Exception the exception
     */
    public void modifierHelper(String codeToModify) throws Exception {

      // Determine if "code" or any of its ancestor codes have modifiers
      // that are not blocked by excluded modifiers
      String cmpCode = codeToModify;
      final Map<String, String> modifiersToMatchedCodeMap = new HashMap<>();
      final Map<String, String> excludedModifiersToMatchedCodeMap =
          new HashMap<>();
      while (cmpCode.length() > 2) {
        getLog().info("    Determine if " + cmpCode + " has modifiers");

        // If a matching modifier is found for this or any ancestor code
        // add it
        if (classToModifierMap.containsKey(cmpCode)) {
          // Find and save all modifiers at this level
          for (final String modifier : classToModifierMap.get(cmpCode)) {
            modifiersToMatchedCodeMap.put(modifier, codeToModify);
            getLog().info("      Use modifier " + modifier + " for " + cmpCode);
            // If this modifier has been explicitly excluded at a lower level
            // then remove it. Note: if there's an excluded modifier higher up
            // it doesn't apply here because this modifier explicitly overrides
            // that exclusion
            if (excludedModifiersToMatchedCodeMap.containsKey(modifier)
                && isDescendantCode(
                    excludedModifiersToMatchedCodeMap.get(modifier), cmpCode)) {
              getLog().info(
                  "      Exclude modifier " + modifier + " for "
                      + modifiersToMatchedCodeMap.get(modifier) + " due to "
                      + excludedModifiersToMatchedCodeMap.get(modifier));
              if (!overrideExclusion(codeToModify, modifier)) {
                modifiersToMatchedCodeMap.remove(modifier);
              } else {
                getLog().info(
                    "      Override exclude modifier " + modifier + " for "
                        + codeToModify);
              }
            }
          }
        }

        // If a matching exclusion of a modifier is found and there
        // is not an explicit modifier that is more specific, remove it
        // NOTE: this can go after the earlier section because we'll always
        // find an excluded modifier at a level lower than where it was defined
        if (classToExcludedModifierMap.containsKey(cmpCode)) {
          for (final String modifier : classToExcludedModifierMap.get(cmpCode)) {
            // Check manual exclusion overrides.
            excludedModifiersToMatchedCodeMap.put(modifier, cmpCode);
          }
        }

        cmpCode = TerminologyClamlLoaderMojo.this.chdParMap.get(cmpCode);
        if (cmpCode == null) {
          break;
        }
      }

      // Determine the modifiers that apply to the current code
      Set<String> modifiersForCode = modifiersToMatchedCodeMap.keySet();
      getLog().info(
          "      Final modifiers to generate classes for: " + modifiersForCode);

      if (modifiersForCode.size() > 0) {

        // Loop through all modifiers identified as applying to this code
        for (final String modifiedByCode : modifiersForCode) {

          // Apply 4th digit modifiers to 3 digit codes (and recursively call)
          // Apply 5th digit modifiers to 4 digit codes (which have length 5 due
          // to .)
          if (codeToModify.length() == 3 && modifiedByCode.endsWith("_4")
              || codeToModify.length() == 5 && modifiedByCode.endsWith("_5")) {

            getLog().info(
                "        Apply modifier " + modifiedByCode + " to "
                    + codeToModify);

            if (modifierMap.containsKey(modifiedByCode)) {
              // for each code on that modifier, create a
              // child and create a relationship
              for (final Map.Entry<String, Concept> mapEntry : modifierMap.get(
                  modifiedByCode).entrySet()) {

                final Concept modConcept =
                    modifierMap.get(modifiedByCode).get(mapEntry.getKey());

                // handle case where a _5 modifier is defined with a code having
                // .*
                String childCode = null;
                if (modifiedByCode.endsWith("_5")
                    && mapEntry.getKey().startsWith("."))
                  childCode =
                      conceptMap.get(codeToModify).getTerminologyId()
                          + mapEntry.getKey().substring(1);
                else
                  childCode =
                      conceptMap.get(codeToModify).getTerminologyId()
                          + mapEntry.getKey();
                createChildConcept(childCode, conceptMap.get(codeToModify),
                    modConcept, relIdCounter++);

                // Recursively call for 5th digit modifiers on generated classes
                if (codeToModify.length() == 3 && modifiedByCode.endsWith("_4")) {
                  modifierHelper(childCode);
                }
              }

            } else {
              throw new Exception("modifiedByCode not in map " + modifiedByCode);
            }

          }

          // Handle case of 3 digit code with a _5 modifier without any children
          else if (codeToModify.length() == 3
              && parChildrenMap.get(codeToModify) == null
              && modifiedByCode.endsWith("_5")) {

            final Concept conceptToModify = conceptMap.get(codeToModify);
            getLog().info(
                "        Creating placeholder concept "
                    + conceptToModify.getTerminologyId() + ".X");
            final Concept placeholderConcept = new ConceptJpa();
            placeholderConcept
                .setDefaultPreferredName(" - PLACEHOLDER 4th digit");

            // Recursively call for 5th digit modifiers where there are no
            // child
            // concepts and the code is only 3 digits, fill in with X
            // create intermediate layer with X
            createChildConcept(conceptToModify.getTerminologyId() + ".X",
                conceptToModify, placeholderConcept, relIdCounter++);

            modifierHelper(conceptMap.get(codeToModify).getTerminologyId()
                + ".X");
          } else {
            getLog().info(
                "        SKIPPING modifier " + modifiedByCode + " for "
                    + codeToModify);
          }

        }

      }
    }

    /**
     * Override exclusions in certain cases.
     * 
     * @param code the code
     * @param modifier the modifier
     * @return true, if successful
     */
    private boolean overrideExclusion(String code, String modifier) {
      if (code.contains("-")) {
        return false;
      }
      final String cmpCode = code.substring(0, 3);
      getLog().info(
          "    CHECK OVERRIDE " + code + ", " + cmpCode + ", " + modifier);

      final Set<String> overrideCodes =
          new HashSet<>(Arrays.asList(new String[] {
              // 4TH AND 5TH
              "V09", "V19", "V29", "V39", "V49", "V59", "V69", "V79", "V80",
              "V81", "V82", "V83", "V84", "V85", "V86", "V87", "V88", "V89",
              "V95", "V96", "V97", "W00", "W01", "W02", "W03", "W04", "W05",
              "W06", "W07", "W08", "W09", "W10", "W11", "W12", "W13", "W14",
              "W15", "W16", "W17", "W18", "W19", "W20", "W21", "W22", "W23",
              "W24", "W25", "W26", "W27", "W28", "W29", "W30", "W31", "W32",
              "W33", "W34", "W35", "W36", "W37", "W38", "W39", "W40", "W41",
              "W42", "W43", "W44", "W45", "W46", "W49", "W50", "W51", "W52",
              "W53", "W54", "W55", "W56", "W57", "W58", "W59", "W60", "W64",
              "W65", "W66", "W67", "W68", "W69", "W70", "W73", "W74", "W75",
              "W76", "W77", "W78", "W79", "W80", "W81", "W83", "W84", "W85",
              "W86", "W87", "W88", "W89", "W90", "W91", "W92", "W93", "W94",
              "W99", "X00", "X01", "X02", "X03", "X04", "X05", "X06", "X08",
              "X09", "X10", "X11", "X12", "X13", "X14", "X15", "X16", "X17",
              "X18", "X19", "X20", "X21", "X22", "X23", "X24", "X25", "X26",
              "X27", "X28", "X29", "X30", "X31", "X32", "X33", "X34", "X35",
              "X36", "X37", "X38", "X39", "X40", "X41", "X42", "X43", "X44",
              "X45", "X46", "X47", "X48", "X49", "X50", "X51", "X52", "X53",
              "X54", "X57", "X58", "X59", "Y06", "Y07", "Y35", "Y36", "Y40",
              "Y41", "Y42", "Y43", "Y44", "Y45", "Y46", "Y47", "Y48", "Y49",
              "Y50", "Y51", "Y52", "Y53", "Y54", "Y55", "Y56", "Y57", "Y58",
              "Y59", "Y63", "Y64", "Y65", "Y83", "Y84", "Y85", "Y87", "Y88",
              "Y89", "Y90", "Y91"
          }));

      // Override excludes for the code list above for S20W00_4
      if (overrideCodes.contains(cmpCode) && modifier.equals("S20W00_4")
          && !parChildrenMap.containsKey(cmpCode))
        return true;

      /**
       * Based on NIN feedback - don't have 5th digits in these cases //
       * Override excludes for the code list above for S20V01T_5 if
       * (overrideCodes.contains(cmpCode) && modifier.equals("S20V01T_5"))
       * return true;
       **/
      return false;
    }

    /**
     * Indicates whether or not descendant code is a descendant of the ancestor
     * code.
     * 
     * @param desc the descendant code
     * @param anc the candidate ancestor code
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean isDescendantCode(String desc, String anc) {
      String currentCode = desc;
      while (TerminologyClamlLoaderMojo.this.chdParMap.get(currentCode) != null) {
        final String parent =
            TerminologyClamlLoaderMojo.this.chdParMap.get(currentCode);
        if (parent.equals(anc)) {
          return true;
        }
      }
      return false;
    }

    /**
     * Creates the child concept.
     * 
     * @param childCode the child code
     * @param parentConcept the concept
     * @param modConcept the mod concept
     * @param relId the rel id
     * @return the concept
     * @throws Exception the exception
     */
    private Concept createChildConcept(String childCode, Concept parentConcept,
      Concept modConcept, int relId) throws Exception {
      getLog().info(
          "        Creating concept " + childCode + " from "
              + parentConcept.getTerminologyId());
      Concept childConcept = new ConceptJpa();
      childConcept =
          helper.createNewActiveConcept(childCode, terminology,
              terminologyVersion, parentConcept.getDefaultPreferredName() + " "
                  + modConcept.getDefaultPreferredName(), effectiveTime);
      if (conceptMap.containsKey(childConcept.getTerminologyId()))
        throw new IllegalStateException("ALERT2!  "
            + childConcept.getTerminologyId() + " already in map");

      conceptMap.put(childConcept.getTerminologyId(), childConcept);
      contentService.addConcept(childConcept);

      // ADD mod concept asterisk stuff
      for (final SimpleRefSetMember member : modConcept
          .getSimpleRefSetMembers()) {
        final SimpleRefSetMember copy = new SimpleRefSetMemberJpa();
        copy.setActive(member.isActive());
        copy.setConcept(childConcept);
        copy.setEffectiveTime(member.getEffectiveTime());
        copy.setModuleId(member.getModuleId());
        copy.setRefSetId(member.getRefSetId());
        copy.setTerminology(member.getTerminology());
        copy.setTerminologyVersion(member.getTerminologyVersion());
        copy.setTerminologyId(member.getTerminologyId());
        contentService.addSimpleRefSetMember(copy);
        // a little different, but does occupy the spot
        conceptUsageMap.put(childConcept.getTerminologyId(),
            member.getRefSetId());
        System.out.println("Y = " + childConcept.getTerminologyId() + ", "
            + classUsage);
      }

      // If child doesn't have usage but parent does,
      // Copy parents here
      if (!conceptUsageMap.containsKey(childConcept.getTerminologyId())
          && conceptUsageMap.containsKey(parentConcept.getTerminologyId())) {
        final String usage =
            conceptUsageMap.get(parentConcept.getTerminologyId());
        System.out.println("Z = " + childConcept.getTerminologyId() + ", "
            + usage);
        final SimpleRefSetMember member = new SimpleRefSetMemberJpa();
        member.setConcept(childConcept);
        member.setActive(true);
        member.setEffectiveTime(dateFormat.parse(effectiveTime));
        member.setModuleId(new Long(conceptMap.get("defaultModule")
            .getTerminologyId()));
        member.setTerminology(terminology);
        member.setTerminologyId(new Integer(refSetMemberCounter++).toString());
        member.setTerminologyVersion(terminologyVersion);
        member.setRefSetId(conceptMap.get(usage).getTerminologyId());
        childConcept.addSimpleRefSetMember(member);
        contentService.addSimpleRefSetMember(member);
        conceptUsageMap.put(childConcept.getTerminologyId(), usage);
      }

      // add relationship
      helper.createIsaRelationship(parentConcept, childConcept, ("" + relId),
          terminology, terminologyVersion, effectiveTime);
      chdParMap.put(childConcept.getTerminologyId(),
          parentConcept.getTerminologyId());
      parChildrenMap.put(parentConcept.getTerminologyId(), true);
      return childConcept;

    }
  }

  /**
   * Find version.
   * @param inputFile
   * 
   * @throws Exception the exception
   */
  public void findVersion(String inputFile) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(inputFile));
    String line = null;
    while ((line = br.readLine()) != null) {
      if (line.contains("<Title")) {
        int versionIndex = line.indexOf("version=");
        if (line.contains("></Title>"))
          terminologyVersion =
              line.substring(versionIndex + 9, line.indexOf("></Title>") - 1);
        else
          terminologyVersion =
              line.substring(versionIndex + 9, versionIndex + 13);
        effectiveTime = terminologyVersion + "0101";
        break;
      }
    }
    br.close();
    // Override terminology version with parameter
    terminologyVersion = version;
    getLog().info("terminologyVersion: " + terminologyVersion);
    getLog().info("effectiveTime: " + effectiveTime);
  }
}