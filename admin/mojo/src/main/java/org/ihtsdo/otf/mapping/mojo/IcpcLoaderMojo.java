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
import java.util.HashMap;
import java.util.HashSet;
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
 * Converts Icpc data to objects.
 *
 * @goal load-icpc
 * @phase process-resources
 */
public class IcpcLoaderMojo extends AbstractMojo {

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


	/**
	 * Executes the plugin.
	 * 
	 * @throws MojoExecutionException the mojo execution exception
	 */
	@Override
	public void execute() throws MojoExecutionException {

		FileInputStream propertiesInputStream = null;
		try {

			getLog().info("Start loading ICPC data ...");

			// load Properties file
			Properties properties = new Properties();
			propertiesInputStream = new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);
			propertiesInputStream.close();

			// set the input directory
			inputFile = properties.getProperty("loader.icpc.input.data");
			if (!new File(inputFile).exists()) {
				throw new MojoFailureException(
						"Specified loader.icpc.input.data directory does not exist: "
								+ inputFile);
			}
			effectiveTime = properties.getProperty("loader.icpc.effective.time");
			terminology = properties.getProperty("loader.icpc.terminology");
			terminologyVersion =
					properties.getProperty("loader.icpc.terminology.version");

			// create Entitymanager
			EntityManagerFactory emFactory =
					Persistence.createEntityManagerFactory("MappingServiceDS");
			manager = emFactory.createEntityManager();

			// create Metadata
			CreateMetadataHelper metadataHelper = new CreateMetadataHelper(
					terminology, terminologyVersion, effectiveTime, manager);
			conceptMap = metadataHelper.createMetadata();

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
			throw new MojoExecutionException("Conversion of XML to RF2 objects failed", e);
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
	/**
	 * @author Deborah
	 *
	 */
	class LocalHandler extends DefaultHandler {

		/** The chars. */
		StringBuffer chars = null;

		/** The rubric kind. */
		String rubricKind = "";

		/** The rubric id. */
		String rubricId = "";

		/** The code. */
		String code = "";
		
		/** The parent code. */
		String parentCode = "";
		
		/** The reference indicating a non-isa relationship. */
		String reference = "";
		
		/** The rels map for holding data for relationships that will be
		 *  built after all concepts are created. */
		Map<String, Set<Concept>> relsMap = new HashMap<String, Set<Concept>>();
		
		/** Indicates rels are needed as a result of the SuperClass tag. */
		boolean relsNeeded = false;
		
		/** The concept that is currently being built from the contents of a Class tag. */
		Concept concept = new ConceptJpa();
		
		/** The rel id counter. */
		int relIdCounter = 100;
		

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

			// reset chars buffer
			chars = new StringBuffer();

			// used to make a concept, code -> terminologyId
			if (qName.equalsIgnoreCase("class"))
				code = attributes.getValue("code");
			
			// used for isa relationships
			if (qName.equalsIgnoreCase("superclass")) {
				parentCode = attributes.getValue("code");
				relsNeeded = true;
			}
			
			// used to get description data
			if (qName.equalsIgnoreCase("rubric")) {
				rubricKind = attributes.getValue("kind");
				rubricId = attributes.getValue("id");
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

				if (qName.equalsIgnoreCase("label")) {

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
						concept.setDefaultPreferredName(chars.toString());
						conceptMap.put(code, concept);

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
					desc.setTerm(chars.toString());
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
					Set<Concept> concepts = new HashSet<Concept>();
					// check if this reference already has a relationship
					if (relsMap.containsKey(reference + ":" + rubricKind)) {
						concepts = relsMap.get(reference + ":" + rubricKind);
					}
					concepts.add(concept);
					relsMap.put(reference + ":" + rubricKind, concepts);
				}  
				

				// end of concept
				if (qName.equalsIgnoreCase("class")) {
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
					// reset variables
					code = "";
					parentCode = "";
					rubricKind = "";
					rubricId = "";
					concept = new ConceptJpa();
				}
			} catch (ParseException e) {
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

		/* (non-Javadoc)
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
							relationship.setTerminologyId(new Integer(relIdCounter++).toString());  
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
	}


}