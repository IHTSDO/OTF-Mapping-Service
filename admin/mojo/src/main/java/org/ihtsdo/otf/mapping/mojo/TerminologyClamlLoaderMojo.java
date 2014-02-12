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
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.SimpleRefSetMemberJpa;
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

	/** The helper. */
	ClamlMetadataHelper helper;

	/**
	 * Executes the plugin.
	 * @throws MojoExecutionException the mojo execution exception
	 */
	@Override
	public void execute() throws MojoExecutionException {

		FileInputStream propertiesInputStream = null;
		FileInputStream fis = null;
		InputStream inputStream = null;
		Reader reader = null;
		try {

			getLog().info("Start loading " + terminology + " data ...");

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
		StringBuffer chars = new StringBuffer();

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

		/** The modified by code. */
		String modifiedByCode = null;

		/** The exclude modifier code. */
		String excludeModifierCode = null;

		/** The modifier. */
		String modifier = null;

		/** The class usage. */
		String classUsage = null;

		/** The reference usage. */
		String referenceUsage = null;

		/** The para chars. */
		String paraChars = null;

		/** The ref set member counter. */
		int refSetMemberCounter = 1;

		/** The fragment list. */
		List<String> fragmentList = new ArrayList<String>();

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

			tagStack.push(qName.toLowerCase());

			// used to make a concept, code -> terminologyId
			if (qName.equalsIgnoreCase("class") || qName.equalsIgnoreCase("modifier")) {
				code = attributes.getValue("code");
				classUsage = attributes.getValue("usage");
				getLog().info(
						"  Encountered class " + code + " "
								+ (classUsage == null ? "" : "(" + classUsage + ")"));
			}

			if (qName.equalsIgnoreCase("modifierclass")) {
				modifier = attributes.getValue("modifier");
				modifierCode = attributes.getValue("code");
				classUsage = attributes.getValue("usage");
				getLog().info(
						"  Encountered modifierClass " + modifierCode + " for " + modifier
								+ " " + (classUsage == null ? "" : "(" + classUsage + ")"));
			}

			if (qName.equalsIgnoreCase("modifiedby")) {
				modifiedByCode = attributes.getValue("code");
				getLog().info("  Class " + code + " modified by " + modifiedByCode);
			}

			if (qName.equalsIgnoreCase("excludemodifier")) {
				excludeModifierCode = attributes.getValue("code");
				getLog().info(
						"  Class " + code + " excludes modifier " + excludeModifierCode);
			}

			// used for isa relationships
			if (qName.equalsIgnoreCase("superclass")) {
				parentCode = attributes.getValue("code");
				relsNeeded = true;
				getLog().info("  Class " + code + " has parent " + parentCode);
			}

			if (qName.equalsIgnoreCase("subclass")) {
				String childCode = attributes.getValue("code");
				currentSubClasses.add(childCode);
				getLog().info("  Class " + code + " has child " + childCode);
			}

			// add to subClassToModifierMap to keep track of later classes
			// that will need modifiers applied
			if (qName.equalsIgnoreCase("modifiedby")) {
				getLog().info("  Class " + code + " modified by " + modifiedByCode);
				List<String> currentModifiers = new ArrayList<String>();
				if (classToModifierMap.containsKey(code)) {
					currentModifiers = classToModifierMap.get(code);
				}
				currentModifiers.add(modifiedByCode);
				classToModifierMap.put(code, currentModifiers);
			}

			if (qName.equalsIgnoreCase("excludemodifier")) {
				getLog().info(
						"  Class and subclasses of " + code + " exclude modifier "
								+ excludeModifierCode);
				List<String> currentModifiers = new ArrayList<String>();
				if (classToExcludedModifierMap.containsKey(code)) {
					currentModifiers = classToExcludedModifierMap.get(code);
				}
				currentModifiers.add(excludeModifierCode);
				classToExcludedModifierMap.put(code, currentModifiers);

				// If it contains a - we need to generate entries
				if (code.indexOf("-") != -1) {
					String[] startEnd = code.split("-");
					char letterStart = startEnd[0].charAt(0);
					char letterEnd = startEnd[1].charAt(0);
					int start = Integer.parseInt(startEnd[0].substring(1));
					int end = Integer.parseInt(startEnd[1].substring(1));
					for (char c = letterStart; c <= letterEnd; c++) {
						for (int i = start; i <= end; i++) {
							String padI = "0000000000" + i;
							String code = c + padI.substring(padI.length() - startEnd[0].length() + 1, padI.length());
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

			// used to get description data
			if (qName.equalsIgnoreCase("rubric")) {
				rubricKind = attributes.getValue("kind");
				rubricId = attributes.getValue("id");
				getLog().info(
						"  Class " + code + " has rubric " + rubricKind + ", " + rubricId);
			}

			if (qName.equalsIgnoreCase("reference")) {
				fragmentList.add(chars.toString().trim());
				chars = new StringBuffer();
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

				// adding concept/preferred description for modifier class
				// NOTE: non-preferred descriptions still need to be added
				// to modified classes
				if (qName.equalsIgnoreCase("label") && modifierCode != null) {
					// prepare the modifier template class while handling ModifierClass
					// still
					addModifierClass();
				}

				// para tags are used in icd9cm descriptions
				if (qName.equalsIgnoreCase("para")) {
					paraChars = chars.toString().trim();
				}

				// adding concept/descriptions for regular class
				if (qName.equalsIgnoreCase("label") && tagStack.contains("class")) {

					// create concept if it doesn't exist
					if (!conceptMap.containsKey(code)) {
						getLog().info("  Instantiate Class " + code);
						concept.setTerminologyId(code);
						concept.setEffectiveTime(dt.parse(effectiveTime));
						concept.setActive(true);
						concept.setModuleId(new Long(conceptMap.get("defaultModule")
								.getTerminologyId()));
						concept.setDefinitionStatusId(new Long(conceptMap.get(
								"defaultDefinitionStatus").getTerminologyId()));
						concept.setTerminology(terminology);
						concept.setTerminologyVersion(terminologyVersion);
						if (flattenFragmentList().length() != 0)
							concept.setDefaultPreferredName(flattenFragmentList());
						else if (chars.toString().trim().equals(""))
							concept.setDefaultPreferredName(paraChars);
						else
							concept.setDefaultPreferredName(chars.toString().trim());
						if (conceptMap.containsKey(code))
							throw new IllegalStateException("ALERT1!  " + code
									+ " already in map");

						conceptMap.put(code, concept);

						getLog().debug(
								"  Add concept " + concept.getTerminologyId() + " "
										+ concept.getDefaultPreferredName());
						// persist now, but commit at the end
						manager.persist(concept);
					}

					// add description to concept for this rubric
					Description desc = new DescriptionJpa();
					desc.setTerminologyId(rubricId);
					desc.setEffectiveTime(dt.parse(effectiveTime));
					desc.setActive(true);
					desc.setModuleId(new Long(conceptMap.get("defaultModule")
							.getTerminologyId()));
					desc.setTerminology(terminology);
					desc.setTerminologyVersion(terminologyVersion);
					if (flattenFragmentList().length() != 0)
						desc.setTerm(flattenFragmentList());
					else if (chars.toString().trim().equals(""))
						desc.setTerm(paraChars);
					else
						desc.setTerm(chars.toString().trim());
					getLog().info(
							"  Instantiate Description for class " + code + " - "
									+ rubricKind + " - " + (desc.getTerm().replaceAll("\r","").replaceAll("\n", "")));
					desc.setConcept(concept);
					desc.setCaseSignificanceId(new Long(conceptMap.get(
							"defaultCaseSignificance").getTerminologyId()));
					desc.setLanguageCode("en");
					if (conceptMap.containsKey(rubricKind))
						desc.setTypeId(new Long(conceptMap.get(rubricKind)
								.getTerminologyId()));
					else {
						throw new IllegalStateException("rubricKind not in metadata "
								+ rubricKind);
					}

					concept.addDescription(desc);

					tagStack.pop();
				}

				// end of reference tag
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

				// if ending modifierclass, add to modifierMap which is used later
				// when classes have modifiedBy tags
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

				if (qName.equalsIgnoreCase("fragment")) {
					fragmentList.add(chars.toString().trim());
				}

				// end of concept
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
						relsNeeded = false;
					}

					// if concept indicates modifiedby tag, add related children
					// also check subClassToModifierMap to see if
					// modifiers need to be created for this concept
					if (qName.equalsIgnoreCase("class") && code.indexOf("-") == -1) {
						modifierHelper(code);
					}

					// this dagger/asterisk info is being recorded for display purposes
					// later
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

					// reset variables
					chars = new StringBuffer();
					code = null;
					parentCode = null;
					modifierCode = null;
					modifier = null;
					modifiedByCode = null;
					rubricKind = null;
					rubricId = null;
					concept = new ConceptJpa();
					currentSubClasses = new HashSet<String>();
					classUsage = null;
					referenceUsage = null;
					paraChars = null;
				}

				if (qName.equalsIgnoreCase("label")) {
					fragmentList = new ArrayList<String>();
				}

			} catch (Exception e) {
				e.printStackTrace();
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

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#endDocument()
		 */
		@Override
		public void endDocument() throws SAXException {
			// add relationships now that all concepts have been created
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
				e.printStackTrace();
			}
		}

		/**
		 * Flatten fragment list.
		 * 
		 * @return the string
		 */
		public String flattenFragmentList() {
			StringBuffer sb = new StringBuffer();
			for (String s : fragmentList) {
				sb.append(s);
			}
			return sb.toString();
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
			while (cmpCode.length() > 2) {
				getLog().info("    Determine if " + cmpCode + " has modifiers");

				// If a matching modifier is found for this or any ancestor code
				// add it
				if (classToModifierMap.containsKey(cmpCode)) {
					for (String modifier : classToModifierMap.get(cmpCode)) {
						modifiersToMatchedCodeMap.put(modifier, codeToModify);
						getLog().info("      Use modifier " + modifier + " for " + cmpCode);
					}
				}

				// If a matching exclusion of a modifier is found and there
				// is not an explicit modifier that is more specific, remove it
				if (classToExcludedModifierMap.containsKey(cmpCode)) {
					for (String modifier : classToExcludedModifierMap.get(cmpCode)) {
						if (modifiersToMatchedCodeMap.containsKey(modifier)
								&& modifiersToMatchedCodeMap.get(modifier).length() <= cmpCode
										.length()) {
							getLog().info(
									"      Excludemodifier " + modifier + " for "
											+ modifiersToMatchedCodeMap.get(modifier) + " due to "
											+ cmpCode);
							modifiersToMatchedCodeMap.remove(modifier);
						}
					}
				}

				cmpCode = cmpCode.substring(0, cmpCode.length() - 1);
				if (cmpCode.endsWith(".")) {
					cmpCode = cmpCode.substring(0, cmpCode.length() - 1);
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

						if (modifierMap.containsKey(modifiedByCode)) {
							// for each code on that modifier, create a
							// child and create a relationship
							for (Map.Entry<String, Concept> mapEntry : modifierMap.get(
									modifiedByCode).entrySet()) {
								Concept modConcept =
										modifierMap.get(modifiedByCode).get(mapEntry.getKey());
								Concept childConcept = new ConceptJpa();
								String childCode =
										concept.getTerminologyId() + mapEntry.getKey();
								getLog().info(
										"      Create child code " + childCode + " for "
												+ modifiedByCode);
								childConcept =
										helper.createNewActiveConcept(childCode, terminology,
												terminologyVersion, concept.getDefaultPreferredName()
														+ " " + modConcept.getDefaultPreferredName(),
												effectiveTime);
								if (conceptMap.containsKey(childConcept.getTerminologyId()))
									throw new IllegalStateException("ALERT2!  "
											+ childConcept.getTerminologyId() + " already in map");

								conceptMap.put(childConcept.getTerminologyId(), childConcept);
								manager.persist(childConcept);
								// add relationship
								helper.createIsaRelationship(concept, childConcept,
										new Integer(relIdCounter++).toString(), terminology,
										terminologyVersion, effectiveTime);

								// Recursively call for 5th digit modifiers on generated classes
								if (codeToModify.length() == 3 && modifiedByCode.endsWith("_4")) {
									if (!childCode.startsWith("W19"))
										modifierHelper(childCode);
								}
							}

						} else {
							throw new Exception("modifiedByCode not in map " + modifiedByCode);
						}
					}

				}

			}
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