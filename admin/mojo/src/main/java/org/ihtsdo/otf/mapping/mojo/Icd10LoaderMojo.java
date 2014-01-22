package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Converts Icd10 xml data to RF2 objects.
 * 
 * @goal load-icd10
 * @phase process-resources
 */
public class Icd10LoaderMojo extends AbstractMojo {

	/** The dt. */
	private SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

	/** The input XML (Claml) file. */
	private String inputFile;

	/** The effective time. */
	private String effectiveTime;

	/** The terminology. */
	private String terminology;

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

	/** The manager. */
	private EntityManager manager;

	// Metadata data structures

	/** The concept map. */
	private Map<String, Concept> conceptMap;

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

			getLog().info("Start loading ICD10 data ...");

			// load Properties file
			Properties properties = new Properties();
			propertiesInputStream = new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);
			propertiesInputStream.close();

			// set the input directory
			inputFile = properties.getProperty("loader.icd10.input.data");
			if (!new File(inputFile).exists()) {
				throw new MojoFailureException(
						"Specified loader.icd10.input.data directory does not exist: "
								+ inputFile);
			}
			effectiveTime = properties.getProperty("loader.icd10.effective.time");
			terminology = properties.getProperty("loader.icd10.terminology");
			terminologyVersion =
					properties.getProperty("loader.icd10.terminology.version");

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
					"Conversion of XML to RF2 objects failed", e);
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

		String modifierCode = "";

		String modifiedByCode = "";
		
		String excludeModifierCode = "";

		String modifier = "";
		
		List<String> fragmentList = new ArrayList<String>();

		/** The reference indicating a non-isa relationship. */
		String reference = "";

		Set<String> currentSubClasses = new HashSet<String>();

		Map<String, List<String>> subClassToModifierMap =
				new HashMap<String, List<String>>();

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
			if (qName.equalsIgnoreCase("class") || qName.equalsIgnoreCase("modifier"))
				code = attributes.getValue("code");

			if (qName.equalsIgnoreCase("modifierclass")) {
				modifier = attributes.getValue("modifier");
				modifierCode = attributes.getValue("code");
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
				if (qName.equalsIgnoreCase("label") && !modifierCode.equals("")
						&& rubricKind.equals("preferred")) {
					addModifierClass();
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
						else
							concept.setDefaultPreferredName(chars.toString().trim());
						if (conceptMap.containsKey(code))
							Logger.getLogger(this.getClass()).info("ALERT1!  " + code + " already in map");
						
						conceptMap.put(code, concept);


						manager.persist(concept);	
					}

					// add description to concept
					concept = conceptMap.get(code);
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
					/**Set<Description> descriptions = concept.getDescriptions();
					if (descriptions.size() > 1)
					  Logger.getLogger(this.getClass()).info("concept " + concept.getTerminologyId() + " description size " + descriptions.size());
					descriptions.add(desc);
					concept.setDescriptions(descriptions);*/
					concept.addDescription(desc);
					//manager.persist(desc);
				}

				//if (qName.equalsIgnoreCase("reference")) {
				//	Logger.getLogger(this.getClass()).info(concept.getTerminologyId() + " chars in end Element " + chars.toString());
				//}
				
				// end of reference tag
				/**if (qName.equalsIgnoreCase("reference")) {
					// relationships for this concept will be added at endDocument(),
					// save relevant data now in relsMap
					reference = chars.toString();
					Set<Concept> concepts = new HashSet<Concept>();
					// check if this reference already has a relationship
					if (relsMap.containsKey(reference + ":" + rubricKind)) {
						concepts = relsMap.get(reference + ":" + rubricKind);
					}
					concepts.add(concept);
					relsMap.put(reference + ":" + rubricKind, concepts);
				}*/

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
		
		public String flattenFragmentList() {
			StringBuffer sb = new StringBuffer();
			for (String s : fragmentList) {
				sb.append(s);
			}
			return sb.toString();
		}
		
		public void addModifierClass() throws Exception {

			// create modifier concept
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
		
		public void modifierHelper() throws Exception {
			if (!modifiedByCode.equals("") && currentSubClasses.size() == 0) {
				if (modifierMap.containsKey(modifiedByCode)) {
					// for each code on that modifier, create a child and create a
					// relationship
					for (Map.Entry<String, Concept> mapEntry : modifierMap.get(
							modifiedByCode).entrySet()) {
						Concept modConcept =
								modifierMap.get(modifiedByCode).get(mapEntry.getKey());
						Concept childConcept = new ConceptJpa();
						childConcept =
								helper.createNewActiveConcept(
										concept.getTerminologyId() + mapEntry.getKey(),
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
					Logger.getLogger(this.getClass()).info(
							"modifiedByCode not in map " + modifiedByCode);
				}
			}
			// before we close class, check subClassToModifierMap to see if
			// modifiers need to be created for this concept
			if (subClassToModifierMap.containsKey(concept.getTerminologyId())) {
				Set<String> newChildIds = new HashSet<String>();
				Logger.getLogger(this.getClass()).info(
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


}