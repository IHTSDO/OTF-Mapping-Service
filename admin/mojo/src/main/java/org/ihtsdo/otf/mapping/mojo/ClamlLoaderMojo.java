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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.CreateMetadataHelper;
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

// TODO: Auto-generated Javadoc
/**
 * Converts claml data to RF2 objects.
 * 
 * @goal load-claml
 * @phase process-resources
 */
public class ClamlLoaderMojo extends AbstractMojo {

	/** The dt. */
	private SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

	/** The input XML (Claml) file. */
	private String inputFile;

	/** The effective time. */
	private String effectiveTime;

	/** The terminology version. */
	private String terminologyVersion;

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
	private String terminology;

	/** The manager. */
	private EntityManager manager;

	// Metadata data structures

	/** The concept map. */
	private Map<String, Concept> conceptMap;

	/** The helper. */
	private CreateMetadataHelper helper;

	/**
	 * Executes the plugin.
	 * 
	 * @throws MojoExecutionException the mojo execution exception
	 */
	@Override
	public void execute() throws MojoExecutionException {

		FileInputStream propertiesInputStream = null;
		try {

			getLog().info("Start loading " + terminology + " data ...");

			// load Properties file
			Properties properties = new Properties();
			propertiesInputStream = new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);
			propertiesInputStream.close();

			// set the input directory
			inputFile = properties.getProperty("loader." + terminology + ".input.data");
			if (!new File(inputFile).exists()) {
				throw new MojoFailureException(
						"Specified loader." + terminology + ".input.data directory does not exist: "
								+ inputFile);
			}
			Logger.getLogger(this.getClass()).info("inputFile: " + inputFile);

			// open input file and get effective time and version
			findVersion();
			
			// create Entitymanager
			EntityManagerFactory emFactory =
					Persistence.createEntityManagerFactory("MappingServiceDS");
			manager = emFactory.createEntityManager();

			// create Metadata
			helper =
					new CreateMetadataHelper(terminology, terminologyVersion,
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
			InputStream inputStream = checkForUtf8BOM(new FileInputStream(file));
			Reader reader = new InputStreamReader(inputStream, "UTF-8");
			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");
			saxParser.parse(is, handler);

			tx.commit();

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(
					"Conversion of Claml to RF2 objects failed", e);
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
		String rubricKind = "";

		/** The rubric id. */
		String rubricId = "";

		/** The code. */
		String code = "";

		/** The parent code. */
		String parentCode = "";

		/** The modifier code. */
		String modifierCode = "";

		/** The modified by code. */
		String modifiedByCode = "";
		
		/** The exclude modifier code. */
		String excludeModifierCode = "";

		/** The modifier. */
		String modifier = "";
		
		/** The class usage. */
		String classUsage = "";
		
		/** The reference usage. */
		String referenceUsage = "";
		
		/** The para chars. */
		String paraChars = "";
		
		/** The ref set member counter. */
		int refSetMemberCounter = 1;
		
		/** The fragment list. */
		List<String> fragmentList = new ArrayList<String>();

		/** The reference indicating a non-isa relationship. */
		String reference = "";

		/** The current sub classes. */
		Set<String> currentSubClasses = new HashSet<String>();

		/** The sub class to modifier map. */
		Map<String, List<String>> subClassToModifierMap =
				new HashMap<String, List<String>>();

		/** The sub class to exclude modifier map. */
		Map<String, List<String>> subClassToExcludeModifierMap =
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

			// used to make a concept, code -> terminologyId
			if (qName.equalsIgnoreCase("class") || qName.equalsIgnoreCase("modifier")) {
				code = attributes.getValue("code");
				classUsage = attributes.getValue("usage");
			}

			if (qName.equalsIgnoreCase("modifierclass")) {
				modifier = attributes.getValue("modifier");
				modifierCode = attributes.getValue("code");
				classUsage = attributes.getValue("usage");
			}

			if (qName.equalsIgnoreCase("modifiedby")) {
				modifiedByCode = attributes.getValue("code");
			}
			
			if (qName.equalsIgnoreCase("excludemodifier")) {
				excludeModifierCode = attributes.getValue("code");
			}

			// used for isa relationships
			if (qName.equalsIgnoreCase("superclass")) {
				parentCode = attributes.getValue("code");
				relsNeeded = true;
			}

			if (qName.equalsIgnoreCase("subclass")) {
				currentSubClasses.add(attributes.getValue("code"));
			}

			// add to subClassToModifierMap to keep track of later classes
			// that will need modifiers applied
			if (qName.equalsIgnoreCase("modifiedby")) {
				for (String subClass : currentSubClasses) {
					List<String> currentModifiers = new ArrayList<String>();
					if (subClassToModifierMap.containsKey(subClass))
						currentModifiers = subClassToModifierMap.get(subClass);
					currentModifiers.add(modifiedByCode);
					subClassToModifierMap.put(subClass, currentModifiers);
				}
			}
			
			if (qName.equalsIgnoreCase("excludemodifier")) {
				List<String> currentModifiers = new ArrayList<String>();
				for (String subClass : currentSubClasses) {
					currentModifiers = new ArrayList<String>();
					if (subClassToExcludeModifierMap.containsKey(subClass))
						currentModifiers = subClassToExcludeModifierMap.get(subClass);
					currentModifiers.add(excludeModifierCode);
					subClassToExcludeModifierMap.put(subClass, currentModifiers);
				}
				currentModifiers.clear();
				if (subClassToExcludeModifierMap.containsKey(code))
					currentModifiers = subClassToExcludeModifierMap.get(code);
				currentModifiers.add(excludeModifierCode);
				subClassToExcludeModifierMap.put(code, currentModifiers);
			}

			// used to get description data
			if (qName.equalsIgnoreCase("rubric")) {
				rubricKind = attributes.getValue("kind");
				rubricId = attributes.getValue("id");
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
				if (qName.equalsIgnoreCase("label") && !modifierCode.equals("")) {
					addModifierClass();
				}
				
				// para tags are used in icd9cm descriptions
				if (qName.equalsIgnoreCase("para")) {
					paraChars = chars.toString().trim();
				}

				// adding concept/descriptions for regular class
				if (qName.equalsIgnoreCase("label") && modifierCode.equals("")) {

					// create concept if it doesn't exist
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
						if (flattenFragmentList().length() != 0)
							concept.setDefaultPreferredName(flattenFragmentList());
						else if (chars.toString().trim().equals(""))
							concept.setDefaultPreferredName(paraChars);
						else
							concept.setDefaultPreferredName(chars.toString().trim());
						if (conceptMap.containsKey(code))
							Logger.getLogger(this.getClass()).info("ALERT1!  " + code + " already in map");
						
						conceptMap.put(code, concept);

						// TODO: move persistence of all concepts to endDocument loop
						manager.persist(concept);	
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
					if (flattenFragmentList().length() != 0)
						desc.setTerm(flattenFragmentList());
					else if (chars.toString().trim().equals(""))
					  desc.setTerm(paraChars);
					else
						desc.setTerm(chars.toString().trim());
					desc.setConcept(concept);
					desc.setCaseSignificanceId(new Long(conceptMap.get(
							"defaultCaseSignificance").getTerminologyId()));
					desc.setLanguageCode("en");
					if (conceptMap.containsKey(rubricKind))
						desc.setTypeId(new Long(conceptMap.get(rubricKind)
								.getTerminologyId()));
					else {
						desc.setTypeId(new Long("8"));
						Logger.getLogger(this.getClass()).info(
								"rubricKind not in metadata " + rubricKind);
					}
					
					concept.addDescription(desc);
				}

				
				// end of reference tag
				if (qName.equalsIgnoreCase("reference")) {
					// relationships for this concept will be added at endDocument(),
					// save relevant data now in relsMap
					reference = chars.toString();

					if (referenceUsage != null && !referenceUsage.equals("")) { /** && 
							classUsage != null && !classUsage.equals("")) */
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
				}
				
				if (qName.equalsIgnoreCase("fragment")) {
					fragmentList.add(chars.toString().trim());
				}

				chars = new StringBuffer();
				
				// end of concept
				if (qName.equalsIgnoreCase("class")
						|| qName.equalsIgnoreCase("modifier")
						|| qName.equalsIgnoreCase("modifierclass")) {
					
					// if relationships for this concept will be added at endDocument(),
					// save relevant data now in relsMap
					if (relsNeeded) {
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
					modifierHelper();
					
					// this dagger/asterisk info is being recorded for display purposes later
					if (classUsage != null && !classUsage.equals("")) {
						SimpleRefSetMember refSetMember = new SimpleRefSetMemberJpa();
						refSetMember.setConcept(concept);
						refSetMember.setActive(true);
						refSetMember.setEffectiveTime(dt.parse(effectiveTime));
						refSetMember.setModuleId(new Long(conceptMap.get("defaultModule")
								.getTerminologyId()));
						refSetMember.setTerminology(terminology);
						refSetMember.setTerminologyId(new Integer(refSetMemberCounter++).toString());
						refSetMember.setTerminologyVersion(terminologyVersion);
						refSetMember.setRefSetId(conceptMap.get(classUsage).getTerminologyId());
					  concept.addSimpleRefSetMember(refSetMember);
					}

					// reset variables
					code = "";
					parentCode = "";
					modifierCode = "";
					modifier = "";
					modifiedByCode = "";
					rubricKind = "";
					rubricId = "";
					concept = new ConceptJpa();
					currentSubClasses = new HashSet<String>();
					classUsage = "";
					referenceUsage = "";
					paraChars = "";
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
								Logger.getLogger(this.getClass()).info(
										"type not in conceptMap " + type);
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
				// NOTE: we don't persist these modifier classes
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
		 * Modifier helper.
		 *
		 * @throws Exception the exception
		 */
		public void modifierHelper() throws Exception {
			// if there are modifiedBy tags on THIS concept
			if (!modifiedByCode.equals("") && currentSubClasses.size() == 0) {
				if (modifierMap.containsKey(modifiedByCode)) {
					// for each code on that modifier, create a child and create a
					// relationship
					for (Map.Entry<String, Concept> mapEntry : modifierMap.get(
							modifiedByCode).entrySet()) {
						Concept modConcept =
								modifierMap.get(modifiedByCode).get(mapEntry.getKey());
						Concept childConcept = new ConceptJpa();
						String childTerminologyId = concept.getTerminologyId() + mapEntry.getKey();
						// if more than one '.' in the id, get rid of second
						/**if (childTerminologyId.indexOf('.') != childTerminologyId.lastIndexOf('.')) {
							childTerminologyId = childTerminologyId.substring(1,childTerminologyId.lastIndexOf('.') ) + 
							childTerminologyId.substring(childTerminologyId.lastIndexOf('.') + 1 );
						} */
						childConcept =
								helper.createNewActiveConcept(
										childTerminologyId,
										terminology,
										terminologyVersion,
										concept.getDefaultPreferredName() + " "
												+ modConcept.getDefaultPreferredName(),
										effectiveTime);
						if (conceptMap.containsKey(childConcept.getTerminologyId()))
							Logger.getLogger(this.getClass()).info(
									"ALERT2!  " + childConcept.getTerminologyId()
											+ " already in map");

						conceptMap.put(childConcept.getTerminologyId(), childConcept);
						manager.persist(childConcept);
						// add relationship
						helper.createIsaRelationship(concept, childConcept,
								new Integer(relIdCounter++).toString(), terminology,
								terminologyVersion, effectiveTime);
					}
				} else {
					throw new Exception ("modifiedByCode not in map " + modifiedByCode);
				}
			}
			// before we close class, check subClassToModifierMap to see if
			// modifiers need to be created for this concept
			if (subClassToModifierMap.containsKey(concept.getTerminologyId())) {
				Set<String> newChildIds = new HashSet<String>();
				Logger.getLogger(this.getClass()).debug(
						"need to add children for "
								+ concept.getTerminologyId()
								+ " "
								+ subClassToModifierMap.get(concept.getTerminologyId())
										.toString());
				List<String> modifiersRequiringChildren =
						subClassToModifierMap.get(concept.getTerminologyId());
							
				for (int ct = 0; ct < modifiersRequiringChildren.size(); ct++) {
					String modifierRequiringChildren =
							modifiersRequiringChildren.get(ct);
					// make sure modifier is not on the exclude list
					if (subClassToExcludeModifierMap.containsKey(concept.getTerminologyId())) {
						List<String> excludeList = subClassToExcludeModifierMap.get(concept.getTerminologyId());
					  if (excludeList.contains(modifierRequiringChildren))
					  	continue;
					}
					if (modifierMap.containsKey(modifierRequiringChildren)) {
						// for each code on that modifier, create a child and create a
						// relationship
						for (Map.Entry<String, Concept> mapEntry : modifierMap.get(
								modifierRequiringChildren).entrySet()) {
							Concept modConcept = mapEntry.getValue();
							Concept childConcept = new ConceptJpa();
							String key = mapEntry.getKey();
							if (ct == 0) { // adding first modifier's children
								// add leading '.', if not present
								if (key.indexOf(".") == -1)
									key = "." + key;
								childConcept.setTerminologyId(concept.getTerminologyId()
										+ key);
								newChildIds.add(childConcept.getTerminologyId());
								childConcept = helper.createNewActiveConcept(
										childConcept.getTerminologyId(), terminology,
										terminologyVersion, concept.getDefaultPreferredName()
												+ " " + modConcept.getDefaultPreferredName(),
										effectiveTime);
								if (conceptMap.containsKey(childConcept.getTerminologyId()))
									Logger.getLogger(this.getClass()).info("ALERT3!  " + childConcept.getTerminologyId() + " already in map");
								
								conceptMap.put(childConcept.getTerminologyId(),
										childConcept);
								manager.persist(childConcept);
								// add relationship
								helper.createIsaRelationship(concept, childConcept,
										new Integer(relIdCounter++).toString(), terminology,
										terminologyVersion, effectiveTime);
							} else { // adding modifier children for those beyond the first modifier
								for (String newChild : newChildIds) {
									childConcept = new ConceptJpa();
									// get rid of leading '.' if present
									if (key.indexOf(".") != -1) {
										key = key.substring(1);
									}
									childConcept.setTerminologyId(newChild + key);
									String parentDefaultPreferredName = conceptMap.get(newChild).getDefaultPreferredName();
									childConcept = helper.createNewActiveConcept(
											childConcept.getTerminologyId(), terminology,
											terminologyVersion, parentDefaultPreferredName
													+ " " + modConcept.getDefaultPreferredName(),
											effectiveTime);
									if (conceptMap.containsKey(childConcept.getTerminologyId()))
										Logger.getLogger(this.getClass()).info("ALERT4!  " + childConcept.getTerminologyId() + " already in map");
									conceptMap.put(childConcept.getTerminologyId(),
											childConcept);
									manager.persist(childConcept);
									// add relationship
									helper.createIsaRelationship(concept, childConcept,
											new Integer(relIdCounter++).toString(), terminology,
											terminologyVersion, effectiveTime);
								}
							}
						}
					} else {
						Logger.getLogger(this.getClass()).info(
								"modifiedByCode not in map " + modifiedByCode);
					}
				}
			}
		}
	}


  /**
   * Find version.
   *
   * @throws Exception the exception
   */
  public void findVersion() throws Exception {
  	BufferedReader br = new BufferedReader(new FileReader(inputFile));
  	String line = "";
  	while ((line = br.readLine()) != null) {
  	   if (line.contains("<Title")) {
  	  	 int versionIndex = line.indexOf("version=");
  	  	 if (line.contains("></Title>"))
  	  	   terminologyVersion = line.substring(versionIndex + 9, line.indexOf("></Title>") -1);
  	  	 else
  	  		 terminologyVersion = line.substring(versionIndex + 9, versionIndex + 13);
  	  	 effectiveTime = terminologyVersion + "0101";
  	     break;
  	   }
  	}
  	br.close();
  	Logger.getLogger(this.getClass()).info("terminologyVersion: " + terminologyVersion);
  	Logger.getLogger(this.getClass()).info("effectiveTime: " + effectiveTime);
  }
}