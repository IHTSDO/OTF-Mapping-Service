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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
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
 * Sample execution:
 * 
 * <pre>
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <dependencies>
 *         <dependency>
 *           <groupId>org.ihtsdo.otf.mapping</groupId>
 *           <artifactId>mapping-admin-loader-config</artifactId>
 *           <version>${project.version}</version>
 *           <scope>system</scope>
 *           <systemPath>${project.build.directory}/mapping-admin-loader-${project.version}.jar</systemPath>
 *         </dependency>
 *       </dependencies>
 *       <executions>
 *         <execution>
 *           <id>load-claml</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>load-claml</goal>
 *           </goals>
 *           <configuration>
 *             <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *             <terminology>ICD10</terminology>
 *           </configuration>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * @goal load-claml
 * @phase package
 */
public class TerminologyClamlLoaderMojo extends AbstractMojo {

  final SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

  /**
   * Properties file.
   * 
   * @parameter 
   *            expression="${project.build.directory}/generated-sources/org/ihtsdo"
   * @required
   */
  private File propertiesFile;

  /**
   * Name of terminology to be loaded.
   * @parameter
   * @required
   */
  String terminology;

  // NOTE: default visibility is used instead of private
  // so that the inner class parser does not require
  // the use of synthetic accessors

  /** The effective time. */
  String effectiveTime;

  /** The terminology version. */
  String terminologyVersion;

  /** The manager. */
  EntityManager manager;

  /** The concept map. */
  Map<String, Concept> conceptMap;

  /**
   * child to parent code map NOTE: this assumes a single superclass
   **/
  Map<String, String> childToParentCodeMap;

  /**
   * Indicates subclass relationships NOTE: this assumes a single superclass
   **/
  Map<String, Boolean> parentCodeHasChildrenMap;

  /** The helper. */
  ClamlMetadataHelper helper;

  /**
   * Executes the plugin.
   * @throws MojoExecutionException the mojo execution exception
   */
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting loading " + terminology + " data ...");

    FileInputStream propertiesInputStream = null;
    FileInputStream fis = null;
    InputStream inputStream = null;
    Reader reader = null;
    try {

      // load Properties file
      Properties properties = new Properties();
      propertiesInputStream = new FileInputStream(propertiesFile);
      properties.load(propertiesInputStream);
      propertiesInputStream.close();

      // set the input directory
      String inputFile =
          properties.getProperty("loader." + terminology + ".input.data");
      if (!new File(inputFile).exists()) {
        throw new MojoFailureException("Specified loader." + terminology
            + ".input.data directory does not exist: " + inputFile);
      }
      getLog().info("inputFile: " + inputFile);

      // open input file and get effective time and version
      findVersion(inputFile);

      // create Entitymanager
      EntityManagerFactory emFactory =
          Persistence.createEntityManagerFactory("MappingServiceDS");
      manager = emFactory.createEntityManager();

      // create Metadata
      getLog().info("  Create metadata classes");
      helper =
          new ClamlMetadataHelper(terminology, terminologyVersion,
              effectiveTime, manager);
      conceptMap = helper.createMetadata();

      childToParentCodeMap = new HashMap<String, String>();
      parentCodeHasChildrenMap = new HashMap<String, Boolean>();

      // Prep SAX parser
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      SAXParser saxParser = factory.newSAXParser();
      DefaultHandler handler = new LocalHandler();

      EntityTransaction tx = manager.getTransaction();
      tx.begin();

      // Open XML and begin parsing
      File file = new File(inputFile);
      fis = new FileInputStream(file);
      inputStream = checkForUtf8BOM(fis);
      reader = new InputStreamReader(inputStream, "UTF-8");
      InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");
			saxParser.parse(is, handler);

			tx.commit();

			// creating tree positions
			// first get isaRelType from metadata
			MetadataService metadataService = new MetadataServiceJpa();
			Map<Long, String> hierRelTypeMap = metadataService.getHierarchicalRelationshipTypes(terminology, terminologyVersion);
			String isaRelType = hierRelTypeMap.keySet().iterator().next().toString();
			metadataService.close();
			
			ContentService contentService = new ContentServiceJpa();
			getLog().info("Start creating tree positions.");
			// TODO: don't hardcode root or isa values
			// eg, for ICPC what is the root?
			/**if (terminology.equals("ICPC"))
			  contentService.computeTreePositions(terminology, terminologyVersion,
					isaRelType, "A");
			else */if (terminology.equals("ICD10")) {
				String[] roots = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX", "XXI", "XXII"};
				for (String root : roots) 
				  contentService.computeTreePositions(terminology, terminologyVersion,
						isaRelType, root);
			} else if (terminology.equals("ICD9CM")) {
				contentService.computeTreePositions(terminology, terminologyVersion,
						isaRelType, "001-999.99");			 
				contentService.computeTreePositions(terminology, terminologyVersion,
						isaRelType, "E000-E999.9");
				contentService.computeTreePositions(terminology, terminologyVersion,
						isaRelType, "V01-V91.99");
			}
			contentService.close();

			getLog().info("done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(
          "Conversion of Claml to RF2 objects failed", e);
    } finally {
      try {
        propertiesInputStream.close();
      } catch (IOException e) {
        // do nothing
      }
      try {
        fis.close();
      } catch (IOException e) {
        // do nothing
      }
      try {
        inputStream.close();
      } catch (IOException e) {
        // do nothing
      }
      try {
        reader.close();
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
   * The SAX parser handler.
   * 
   * @author ${author}
   */
  class LocalHandler extends DefaultHandler {

    /** The chars. */
    StringBuilder chars = new StringBuilder();

    /** The label chars - used for description text. */
    StringBuilder labelChars = new StringBuilder();

    /** The rubric kind. */
    String rubricKind = null;

    /** The rubric id. */
    String rubricId = null;

    /** The code. */
    String code = null;

    /** The parent code. */
    String parentCode = null;

    /** The modifier code. */
    String modifierCode = null;

    /** The modifier. */
    String modifier = null;

    /** The class usage. */
    String classUsage = null;

    /** The reference usage. */
    String referenceUsage = null;

    /** The ref set member counter. */
    int refSetMemberCounter = 1;

    /** The reference indicating a non-isa relationship. */
    String reference = null;

    /** The current sub classes. */
    Set<String> currentSubClasses = new HashSet<String>();

    /**
     * This is a code => modifier map. The modifier must then be looked up in
     * modifier map to determine the code extensions and template concepts
     * associated with it.
     */
    Map<String, List<String>> classToModifierMap =
        new HashMap<String, List<String>>();

    /**
     * This is a code => modifier map. If a code is modified but also blocked by
     * an entry in here, do not make children from the template classes.
     */
    Map<String, List<String>> classToExcludedModifierMap =
        new HashMap<String, List<String>>();

    /**
     * The rels map for holding data for relationships that will be built after
     * all concepts are created.
     */
    Map<String, Set<Concept>> relsMap = new HashMap<String, Set<Concept>>();

    /** Indicates rels are needed as a result of the SuperClass tag. */
    boolean relsNeeded = false;

    /**
     * The concept that is currently being built from the contents of a Class
     * tag.
     */
    Concept concept = new ConceptJpa();

    /** The rel id counter. */
    int relIdCounter = 100;

    /** The modifier map. */
    Map<String, Map<String, Concept>> modifierMap =
        new HashMap<String, Map<String, Concept>>();

    /**
     * Tag stack.
     */
    Stack<String> tagStack = new Stack<String>();

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
      Attributes attributes) {

      // add current tag to stack
      tagStack.push(qName.toLowerCase());

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
        relsNeeded = true;
        getLog().info(
            "  Class "
                + (code != null ? code : (modifier + ":" + modifierCode))
                + " has parent " + parentCode);
        parentCodeHasChildrenMap.put(parentCode, true);
      }

      // Encountered "Subclass", save child information
      if (qName.equalsIgnoreCase("subclass")) {
        String childCode = attributes.getValue("code");
        currentSubClasses.add(childCode);
        getLog().info(
            "  Class "
                + (code != null ? code : (modifier + ":" + modifierCode))
                + " has child " + childCode);
        parentCodeHasChildrenMap.put(code, true);
      }

      // Encountered ModifiedBy, save modifier code information
      if (qName.equalsIgnoreCase("modifiedby")) {
        String modifiedByCode = attributes.getValue("code");
        getLog().info("  Class " + code + " modified by " + modifiedByCode);
        List<String> currentModifiers = new ArrayList<String>();
        if (classToModifierMap.containsKey(code)) {
          currentModifiers = classToModifierMap.get(code);
        }
        currentModifiers.add(modifiedByCode);
        classToModifierMap.put(code, currentModifiers);
      }

      // Encountered ExcludeModifier, save excluded modifier code information
      if (qName.equalsIgnoreCase("excludemodifier")) {
        String excludeModifierCode = attributes.getValue("code");
        getLog().info(
            "  Class and subclasses of " + code + " exclude modifier "
                + excludeModifierCode);
        List<String> currentModifiers = new ArrayList<String>();
        if (classToExcludedModifierMap.containsKey(code)) {
          currentModifiers = classToExcludedModifierMap.get(code);
        }
        currentModifiers.add(excludeModifierCode);
        classToExcludedModifierMap.put(code, currentModifiers);

        // If the code contains a dash (-) we need to generate
        // all of the codes in the range
        if (code.indexOf("-") != -1) {
          String[] startEnd = code.split("-");
          char letterStart = startEnd[0].charAt(0);
          char letterEnd = startEnd[1].charAt(0);
          int start = Integer.parseInt(startEnd[0].substring(1));
          int end = Integer.parseInt(startEnd[1].substring(1));
          for (char c = letterStart; c <= letterEnd; c++) {
            for (int i = start; i <= end; i++) {
              String padI = "0000000000" + i;
              String code =
                  c
                      + padI.substring(
                          padI.length() - startEnd[0].length() + 1,
                          padI.length());
              getLog().info(
                  "  Class and subclasses of " + code + " exclude modifier "
                      + excludeModifierCode);
              currentModifiers = new ArrayList<String>();
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
        }
        // Clear "characters"
        chars = new StringBuilder();

        // Save reference usage
        referenceUsage = attributes.getValue("usage");
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
          addModifierClass();
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
            concept.setEffectiveTime(dt.parse(effectiveTime));
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
            manager.persist(concept);
            conceptMap.put(code, concept);
          }

          // Add description to concept for this rubric
          Description desc = new DescriptionJpa();
          desc.setTerminologyId(rubricId);
          desc.setEffectiveTime(dt.parse(effectiveTime));
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

        // Encountered </Reference>, create info for later relationship creation
        if (qName.equalsIgnoreCase("reference")) {
          // relationships for this concept will be added at endDocument(),
          // save relevant data now in relsMap
          reference = chars.toString();
          getLog().info(
              "  Class " + code + " has reference to " + reference + " "
                  + (referenceUsage == null ? "" : "(" + referenceUsage + ")"));

          if (referenceUsage != null) {
            // check if this reference already has a relationship
            Set<Concept> concepts = new HashSet<Concept>();
            if (relsMap.containsKey(reference + ":" + referenceUsage)) {
              concepts = relsMap.get(reference + ":" + referenceUsage);
            }
            concepts.add(concept);
            relsMap.put(reference + ":" + referenceUsage, concepts);
          } else {
            // check if this reference already has a relationship
            Set<Concept> concepts = new HashSet<Concept>();
            if (relsMap.containsKey(reference + ":" + rubricKind)) {
              concepts = relsMap.get(reference + ":" + rubricKind);
            }
            concepts.add(concept);
            relsMap.put(reference + ":" + rubricKind, concepts);
          }
        }

        // Encountered </ModifierClass>
        // Add the template concept to the map for this
        // ModifierClass's code (e.g. ".1" => template concept)
        // Add that to the overall map for the corresponding modifier
        if (qName.equalsIgnoreCase("modifierclass")) {
          Map<String, Concept> modifierCodeToClassMap =
              new HashMap<String, Concept>();
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
          if (relsNeeded) {
            getLog().info("  Class " + code + " has parent " + parentCode);
            Set<Concept> children = new HashSet<Concept>();
            // check if this parentCode already has children
            if (relsMap.containsKey(parentCode + ":" + "isa")) {
              children = relsMap.get(parentCode + ":" + "isa");
            }
            children.add(concept);
            relsMap.put(parentCode + ":" + "isa", children);
            for (Concept child : children) {
              childToParentCodeMap.put(child.getTerminologyId(), parentCode);
            }
            parentCodeHasChildrenMap.put(parentCode, true);

          }

          // If concept indicates modifiedby tag, add related children
          // also check subClassToModifierMap to see if
          // modifiers need to be created for this concept
          if (qName.equalsIgnoreCase("class") && code.indexOf("-") == -1) {
            modifierHelper(code);
          }

          // Record class level dagger/asterisk info as refset member
          if (classUsage != null) {
            getLog().info("  Class " + code + " has usage " + classUsage);
            SimpleRefSetMember refSetMember = new SimpleRefSetMemberJpa();
            refSetMember.setConcept(concept);
            refSetMember.setActive(true);
            refSetMember.setEffectiveTime(dt.parse(effectiveTime));
            refSetMember.setModuleId(new Long(conceptMap.get("defaultModule")
                .getTerminologyId()));
            refSetMember.setTerminology(terminology);
            refSetMember.setTerminologyId(new Integer(refSetMemberCounter++)
                .toString());
            refSetMember.setTerminologyVersion(terminologyVersion);
            refSetMember.setRefSetId(conceptMap.get(classUsage)
                .getTerminologyId());
            concept.addSimpleRefSetMember(refSetMember);
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
          currentSubClasses = new HashSet<String>();
          classUsage = null;
          referenceUsage = null;
          relsNeeded = false;
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
      try {
        for (Map.Entry<String, Set<Concept>> mapEntry : relsMap.entrySet()) {
          String key = mapEntry.getKey();
          String parentCode = key.substring(0, key.indexOf(":"));
          String type = key.substring(key.indexOf(":") + 1);
          if (type.equals("aster"))
            type = "dagger-to-asterisk";
          if (type.equals("dagger"))
            type = "asterisk-to-dagger";
          for (Concept childConcept : mapEntry.getValue()) {
            if (conceptMap.containsKey(parentCode)) {
              Relationship relationship = new RelationshipJpa();
              relationship.setTerminologyId(new Integer(relIdCounter++)
                  .toString());
              relationship.setEffectiveTime(dt.parse(effectiveTime));
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
              Set<Relationship> rels = new HashSet<Relationship>();
              if (childConcept.getRelationships() != null)
                rels = childConcept.getRelationships();
              rels.add(relationship);
              childConcept.setRelationships(rels);

            }
          }
        }
        manager.close();

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
      if (!conceptMap.containsKey(code)) {
        concept.setTerminologyId(modifier + modifierCode);
        concept.setEffectiveTime(dt.parse(effectiveTime));
        concept.setActive(true);
        concept.setModuleId(new Long(conceptMap.get("defaultModule")
            .getTerminologyId()));
        concept.setDefinitionStatusId(new Long(conceptMap.get(
            "defaultDefinitionStatus").getTerminologyId()));
        concept.setTerminology(terminology);
        concept.setTerminologyVersion(terminologyVersion);
        concept.setDefaultPreferredName(chars.toString());
        // NOTE: we don't persist these modifier classes, the
        // classes they generate get added during modifierHelper
        conceptMap.put(code, concept);
      }

      // add description to concept
      Description desc = new DescriptionJpa();
      desc.setTerminologyId(rubricId);
      desc.setEffectiveTime(dt.parse(effectiveTime));
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
      Map<String, String> modifiersToMatchedCodeMap =
          new HashMap<String, String>();
      Map<String, String> excludedModifiersToMatchedCodeMap =
          new HashMap<String, String>();
      while (cmpCode.length() > 2) {
        getLog().info("    Determine if " + cmpCode + " has modifiers");

        // If a matching modifier is found for this or any ancestor code
        // add it
        if (classToModifierMap.containsKey(cmpCode)) {
          // Find and save all modifiers at this level
          for (String modifier : classToModifierMap.get(cmpCode)) {
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
              modifiersToMatchedCodeMap.remove(modifier);
            }
          }
        }

        // If a matching exclusion of a modifier is found and there
        // is not an explicit modifier that is more specific, remove it
        // NOTE: this can go after the earlier section because we'll always
        // find an excluded modifier at a level lower than where it was defined
        if (classToExcludedModifierMap.containsKey(cmpCode)) {
          for (String modifier : classToExcludedModifierMap.get(cmpCode)) {
            excludedModifiersToMatchedCodeMap.put(modifier, cmpCode);
          }
        }

        cmpCode =
            TerminologyClamlLoaderMojo.this.childToParentCodeMap.get(cmpCode);
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
        for (String modifiedByCode : modifiersForCode) {

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
              for (Map.Entry<String, Concept> mapEntry : modifierMap.get(
                  modifiedByCode).entrySet()) {

                Concept modConcept =
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
              && parentCodeHasChildrenMap.get(codeToModify) == null
              && modifiedByCode.endsWith("_5")) {

            Concept conceptToModify = conceptMap.get(codeToModify);
            getLog().info(
                "        Creating placeholder concept "
                    + conceptToModify.getTerminologyId() + ".X");
            Concept placeholderConcept = new ConceptJpa();
            placeholderConcept.setDefaultPreferredName(" - PLACEHOLDER 4th digit");
            
            // Recursively call for 5th digit modifiers where there are no
            // child
            // concepts and the code is only 3 digits, fill in with X
            // create intermediate layer with X
            createChildConcept(conceptToModify.getTerminologyId()
                + ".X", conceptToModify, placeholderConcept,
                relIdCounter++);

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
     * Indicates whether or not descendant code is a descendant of the ancestor
     * code.
     * 
     * @param desc the descendant code
     * @param anc the candidate ancestor code
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean isDescendantCode(String desc, String anc) {
      String currentCode = desc;
      while (TerminologyClamlLoaderMojo.this.childToParentCodeMap
          .get(currentCode) != null) {
        String parent =
            TerminologyClamlLoaderMojo.this.childToParentCodeMap
                .get(currentCode);
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
      manager.persist(childConcept);
      // add relationship
      helper.createIsaRelationship(parentConcept, childConcept, ("" + relId),
          terminology, terminologyVersion, effectiveTime);
      childToParentCodeMap.put(childConcept.getTerminologyId(),
          parentConcept.getTerminologyId());
      parentCodeHasChildrenMap.put(parentConcept.getTerminologyId(), true);
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
    getLog().info("terminologyVersion: " + terminologyVersion);
    getLog().info("effectiveTime: " + effectiveTime);
  }
}