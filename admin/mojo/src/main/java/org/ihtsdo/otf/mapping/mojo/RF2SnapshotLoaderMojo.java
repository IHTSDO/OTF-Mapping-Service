/**
 * Copyright (c) 2012 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.AttributeValueRefSetMemberJpa;
import org.ihtsdo.otf.mapping.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.jpa.LanguageRefSetMemberJpa;
import org.ihtsdo.otf.mapping.jpa.RelationshipJpa;
import org.ihtsdo.otf.mapping.jpa.SimpleMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.jpa.SimpleRefSetMemberJpa;
import org.ihtsdo.otf.mapping.model.AttributeValueRefSetMember;
import org.ihtsdo.otf.mapping.model.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.model.Concept;
import org.ihtsdo.otf.mapping.model.Description;
import org.ihtsdo.otf.mapping.model.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.model.Relationship;
import org.ihtsdo.otf.mapping.model.SimpleMapRefSetMember;
import org.ihtsdo.otf.mapping.model.SimpleRefSetMember;

/**
 * Goal which loads an RF2 Snapshot of SNOMED CT data into a database.
 * 
 * <pre>
 *   <plugin>
 *      <groupId>org.ihtsdo.otf.mapping</groupId>
 *      <artifactId>mapping-admin-mojo</artifactId>
 *      <version>${project.version}</version>
 *      <dependencies>
 *        <dependency>
 *          <groupId>org.ihtsdo.otf.mapping</groupId>
 *          <artifactId>mapping-admin-loader-config</artifactId>
 *          <version>${project.version}</version>
 *          <scope>system</scope>
 *          <systemPath>${project.build.directory}/mapping-admin-loader-${project.version}.jar</systemPath>
 *        </dependency>
 *      </dependencies>
 *      <executions>
 *        <execution>
 *          <id>load-rf2-snapshot</id>
 *          <phase>package</phase>
 *          <goals>
 *            <goal>load-rf2-snapshot</goal>
 *          </goals>
 *          <configuration>
 *            <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *          </configuration>
 *        </execution>
 *      </executions>
 *    </plugin>
 * </pre>
 * 
 * @goal load-rf2-snapshot
 * 
 * @phase process-resources
 */
public class RF2SnapshotLoaderMojo extends AbstractMojo {

	/**
	 * Properties file.
	 * 
	 * @parameter 
	 *            expression="${project.build.directory}/generated-sources/org/ihtsdo"
	 * @required
	 */
	private File propertiesFile;

	/** Core input directory. */
	private File coreInputDir;

	/** The core rel input file. */
	private File coreRelInputFile = null;

	/** The core stated rel input file. */
	private File coreStatedRelInputFile = null;

	/** The core concept input file. */
	private File coreConceptInputFile = null;

	/** The core description input file. */
	private File coreDescriptionInputFile = null;

	/** The core simple refset input file. */
	private File coreSimpleRefsetInputFile = null;

	/** The core association reference input file. */
	private File coreAssociationReferenceInputFile = null;

	/** The core attribute value input file. */
	private File coreAttributeValueInputFile = null;

	/** The core complex map input file. */
	private File coreComplexMapInputFile = null;

	/** The core simple map input file. */
	private File coreSimpleMapInputFile = null;

	/** The core language input file. */
	private File coreLanguageInputFile = null;

	/** The core identifier input file. */
	private File coreIdentifierInputFile = null;

	/** The core text definition input file. */
	private File coreTextDefinitionInputFile = null;

	/** The core rel input. */
	private BufferedReader coreRelInput = null;

	/** The core stated rel input. */
	private BufferedReader coreStatedRelInput = null;

	/** The core concept input. */
	private BufferedReader coreConceptInput = null;

	/** The core desc input. */
	private BufferedReader coreDescInput = null;

	/** The core simple refset input. */
	private BufferedReader coreSimpleRefsetInput = null;

	/** The core association reference input. */
	private BufferedReader coreAssociationReferenceInput = null;

	/** The core attribute value input. */
	private BufferedReader coreAttributeValueInput = null;

	/** The core complex map input. */
	private BufferedReader coreComplexMapInput = null;

	/** The core simple map input. */
	private BufferedReader coreSimpleMapInput = null;

	/** The core language input. */
	private BufferedReader coreLanguageInput = null;

	/** The core identifier input. */
	private BufferedReader coreIdentifierInput = null;

	/** The core text definition input. */
	private BufferedReader coreTextDefinitionInput = null;

	/** The core refset input dir. */
	private File coreRefsetInputDir = null;

	/** The core terminology input dir. */
	private File coreTerminologyInputDir = null;

	/** The core content input dir. */
	private File coreContentInputDir = null;

	/** The core crossmap input dir. */
	private File coreCrossmapInputDir = null;

	/** The core language input dir. */
	private File coreLanguageInputDir = null;

	/** The core metadata input dir. */
	private File coreMetadataInputDir = null;

	/** The date format. */
	private SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

	/** The version. */
	private String version = "";

	/** The manager. */
	private EntityManager manager;

	/** The concept cache. */
	private Map<String, Concept> conceptCache = new HashMap<>();

	/** The description cache. */
	private Map<String, Description> descriptionCache = new HashMap<>();

	/**
	 * Instantiates a {@link RF2SnapshotLoaderMojo} from the specified parameters.
	 * 
	 */
	public RF2SnapshotLoaderMojo() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoFailureException {
		try {

			getLog().info("  In RF2SnapshotLoader.java");

			Properties properties = new Properties();
			FileInputStream propertiesInputStream =
					new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);
			String coreInputDirString = properties.getProperty("loader.input.data");
			propertiesInputStream.close();
			coreInputDir = new File(coreInputDirString);

			EntityManagerFactory factory =
					Persistence.createEntityManagerFactory("MappingServiceDS");
			manager = factory.createEntityManager();

			openInputFiles();

			getVersion();

			EntityTransaction tx = manager.getTransaction();
			try {

				// truncate all the tables that we are going to use first
				tx.begin();

				// truncate RefSets
				Query query =
						manager.createQuery("DELETE From SimpleRefSetMemberJpa rs");
				int deleteRecords = query.executeUpdate();
				getLog().info("simple_ref_set records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From SimpleMapRefSetMemberJpa rs");
				deleteRecords = query.executeUpdate();
				getLog().info("simple_map_ref_set records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From ComplexMapRefSetMemberJpa rs");
				deleteRecords = query.executeUpdate();
				getLog().info("complex_map_ref_set records deleted: " + deleteRecords);
				query =
						manager.createQuery("DELETE From AttributeValueRefSetMemberJpa rs");
				deleteRecords = query.executeUpdate();
				getLog().info(
						"attribute_value_ref_set records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From LanguageRefSetMemberJpa rs");
				deleteRecords = query.executeUpdate();
				getLog().info("language_ref_set records deleted: " + deleteRecords);

				// Truncate Terminology Elements
				query = manager.createQuery("DELETE From DescriptionJpa d");
				deleteRecords = query.executeUpdate();
				getLog().info("description records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From RelationshipJpa r");
				deleteRecords = query.executeUpdate();
				getLog().info("relationship records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From ConceptJpa c");
				deleteRecords = query.executeUpdate();
				getLog().info("concept records deleted: " + deleteRecords);

				tx.commit();

				tx.begin();
				loadConcepts();
				tx.commit();

				tx.begin();
				loadComponents();
				tx.commit();

				tx.begin();
				loadRefSets();
				tx.commit();

			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			}

			manager.close();
			factory.close();

			closeAllFiles();

		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}
	}

	/**
	 * Load concepts.
	 * 
	 * @throws Exception the exception
	 */
	private void loadConcepts() throws Exception {

		String line = "";
		int i = 0;

		getLog().info("Loading Concepts...");

		while ((line = coreConceptInput.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, "\t");
			Concept concept = new ConceptJpa();
			String firstToken = st.nextToken();
			if (!firstToken.equals("id")) { // header
				concept.setTerminologyId(firstToken);
				concept.setEffectiveTime(dt.parse(st.nextToken()));
				concept.setActive(st.nextToken().equals("1") ? true : false);
				concept.setModuleId(Long.valueOf(st.nextToken()));
				concept.setDefinitionStatusId(Long.valueOf(st.nextToken()));
				concept.setTerminology("SNOMEDCT");
				concept.setTerminologyVersion(version);
				concept.setDefaultPreferredName("testDefaultPreferredName");

				manager.persist(concept);
				conceptCache.put(
						firstToken + concept.getTerminology()
								+ concept.getTerminologyVersion(), concept);

				// if (++i%1000 == 0) {getLog().info(".");}
				i++;
			}
		}
		getLog().info(Integer.toString(i) + " Concepts loaded.");
	}

	/**
	 * Load components.
	 * 
	 * @throws Exception the exception
	 */
	private void loadComponents() throws Exception {

		String line = "";

		getLog().info("Loading Relationships...");
		int i = 0;
		while ((line = coreRelInput.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, "\t");
			Relationship relationship = new RelationshipJpa();
			String firstToken = st.nextToken();
			if (!firstToken.equals("id")) { // header
				relationship.setTerminologyId(firstToken);
				relationship.setEffectiveTime(dt.parse(st.nextToken()));
				relationship.setActive(st.nextToken().equals("1") ? true : false); // active
				relationship.setModuleId(Long.valueOf(st.nextToken())); // moduleId
				String source = st.nextToken(); // sourceId
				String target = st.nextToken(); // destinationId
				relationship.setRelationshipGroup(Integer.valueOf(st.nextToken())); // relationshipGroup
				relationship.setTypeId(Long.valueOf(st.nextToken())); // typeId
				relationship.setCharacteristicTypeId(Long.valueOf("1")); // characteristicTypeId
				relationship.setTerminology("SNOMEDCT");
				relationship.setTerminologyVersion(version);
				relationship.setModifierId(Long.valueOf("2"));

				relationship
						.setSourceConcept(getConcept(source, relationship.getTerminology(),
								relationship.getTerminologyVersion()));
				relationship
						.setDestinationConcept(getConcept(target,
								relationship.getTerminology(),
								relationship.getTerminologyVersion()));

				manager.persist(relationship);

				// if (++i%1000 == 0) {getLog().info("."); }
				i++;
			}
		}
		getLog().info(Integer.toString(i) + " Relationships loaded.");

		getLog().info("Loading Descriptions...");
		i = 0;

		// keep concepts from extension descriptions
		while ((line = coreDescInput.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, "\t");
			Description description = new DescriptionJpa();
			String firstToken = st.nextToken();
			if (!firstToken.equals("id")) { // header
				description.setTerminologyId(firstToken);
				description.setEffectiveTime(dt.parse(st.nextToken()));
				description.setActive(st.nextToken().equals("1") ? true : false);
				description.setModuleId(Long.valueOf(st.nextToken()));
				String conceptId = st.nextToken(); // conceptId

				description.setLanguageCode(st.nextToken());
				description.setTypeId(Long.valueOf(st.nextToken()));
				description.setTerm(st.nextToken());
				description.setCaseSignificanceId(Long.valueOf(st.nextToken()));
				description.setTerminology("SNOMEDCT");
				description.setTerminologyVersion(version);

				description.setConcept(getConcept(conceptId,
						description.getTerminology(), description.getTerminologyVersion()));

				manager.persist(description);
				descriptionCache.put(firstToken + description.getTerminology()
						+ description.getTerminologyVersion(), description);

				// if (++i%1000 == 0) {getLog().info("."); }
				i++;
			}
		}
		getLog().info(Integer.toString(i) + " Descriptions loaded.");
	}

	/**
	 * Load RefSets: simple, simple_map, complex_map, attribute_value, language
	 * 
	 * @throws Exception the exception
	 */
	private void loadRefSets() throws Exception {

		String line = "";
		int i = 0;

		// load Simple RefSets (Content)
		getLog().info("Loading Simple RefSets...");
		while ((line = coreSimpleRefsetInput.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, "\t");
			SimpleRefSetMember simpleRefSetMember = new SimpleRefSetMemberJpa();
			String firstToken = st.nextToken();
			if (!firstToken.equals("id")) { // header

				// Universal RefSet attributes
				simpleRefSetMember.setTerminologyId(firstToken);
				simpleRefSetMember.setEffectiveTime(dt.parse(st.nextToken()));
				simpleRefSetMember.setActive(st.nextToken().equals("1") ? true : false);
				simpleRefSetMember.setModuleId(Long.valueOf(st.nextToken()));
				simpleRefSetMember.setRefSetId(Long.valueOf(st.nextToken()));
				firstToken = st.nextToken(); // referencedComponentId

				// SimpleRefSetMember unique attributes
				// NONE

				// Terminology attributes
				simpleRefSetMember.setTerminology("SNOMEDCT");
				simpleRefSetMember.setTerminologyVersion(version);

				// Retrieve Concept
				simpleRefSetMember.setConcept(getConcept(firstToken,
						simpleRefSetMember.getTerminology(),
						simpleRefSetMember.getTerminologyVersion()));

				manager.persist(simpleRefSetMember);

				i++;
			}
		}

		getLog().info(Integer.toString(i) + " Simple RefSets loaded.");

		// load AttributeValue RefSets (Content)
		getLog().info("Loading AttributeValue RefSets...");

		while ((line = coreAttributeValueInput.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, "\t");
			AttributeValueRefSetMember attributeValueRefSetMember =
					new AttributeValueRefSetMemberJpa();
			String firstToken = st.nextToken();
			if (!firstToken.equals("id")) { // header

				// Universal RefSet attributes
				attributeValueRefSetMember.setTerminologyId(firstToken);
				attributeValueRefSetMember.setEffectiveTime(dt.parse(st.nextToken()));
				attributeValueRefSetMember.setActive(st.nextToken().equals("1") ? true
						: false);
				attributeValueRefSetMember.setModuleId(Long.valueOf(st.nextToken()));
				attributeValueRefSetMember.setRefSetId(Long.valueOf(st.nextToken()));
				firstToken = st.nextToken(); // referencedComponentId

				// AttributeValueRefSetMember unique attributes
				attributeValueRefSetMember.setValueId(Long.valueOf(st.nextToken()));

				// Terminology attributes
				attributeValueRefSetMember.setTerminology("SNOMEDCT");
				attributeValueRefSetMember.setTerminologyVersion(version);

				// Retrieve concept
				attributeValueRefSetMember.setConcept(getConcept(firstToken,
						attributeValueRefSetMember.getTerminology(),
						attributeValueRefSetMember.getTerminologyVersion()));

				manager.persist(attributeValueRefSetMember);

				i++;
			}
		}
		getLog().info(Integer.toString(i) + " AttributeValue RefSets loaded.");

		// load SimpleMap RefSets (Crossmap)
		getLog().info("Loading SimpleMap RefSets...");
		i = 0;

		while ((line = coreSimpleMapInput.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, "\t");
			SimpleMapRefSetMember simpleMapRefSetMember =
					new SimpleMapRefSetMemberJpa();
			String firstToken = st.nextToken();
			if (!firstToken.equals("id")) { // header

				// Universal RefSet attributes
				simpleMapRefSetMember.setTerminologyId(firstToken);
				simpleMapRefSetMember.setEffectiveTime(dt.parse(st.nextToken()));
				simpleMapRefSetMember.setActive(st.nextToken().equals("1") ? true
						: false);
				simpleMapRefSetMember.setModuleId(Long.valueOf(st.nextToken()));
				simpleMapRefSetMember.setRefSetId(Long.valueOf(st.nextToken()));
				firstToken = st.nextToken(); // referencedComponentId

				// SimpleMap unique attributes
				simpleMapRefSetMember.setMapTarget(st.nextToken());

				// Terminology attributes
				simpleMapRefSetMember.setTerminology("SNOMEDCT");
				simpleMapRefSetMember.setTerminologyVersion(version);

				// Retrieve concept
				simpleMapRefSetMember.setConcept(getConcept(firstToken,
						simpleMapRefSetMember.getTerminology(),
						simpleMapRefSetMember.getTerminologyVersion()));

				manager.persist(simpleMapRefSetMember);

				i++;
			}
		}

		getLog().info(Integer.toString(i) + " SimpleMap RefSets loaded.");

		// load ComplexMap RefSets (Crossmap)
		getLog().info("Loading ComplexMap RefSets...");
		i = 0;

		while ((line = coreComplexMapInput.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, "\t");
			ComplexMapRefSetMember complexMapRefSetMember =
					new ComplexMapRefSetMemberJpa();
			String firstToken = st.nextToken();
			if (!firstToken.equals("id")) { // header

				// Universal RefSet attributes
				complexMapRefSetMember.setTerminologyId(firstToken);
				complexMapRefSetMember.setEffectiveTime(dt.parse(st.nextToken()));
				complexMapRefSetMember.setActive(st.nextToken().equals("1") ? true
						: false);
				complexMapRefSetMember.setModuleId(Long.valueOf(st.nextToken()));
				complexMapRefSetMember.setRefSetId(Long.valueOf(st.nextToken())); // conceptId
				firstToken = st.nextToken(); // referencedComponentId

				// ComplexMap unique attributes
				complexMapRefSetMember.setMapGroup(Integer.parseInt(st.nextToken()));
				complexMapRefSetMember.setMapPriority(Integer.parseInt(st.nextToken()));
				complexMapRefSetMember.setMapRule(st.nextToken());
				complexMapRefSetMember.setMapAdvice(st.nextToken());
				complexMapRefSetMember.setMapTarget(st.nextToken());
				complexMapRefSetMember.setMapRelationId(Long.valueOf(st.nextToken()));

				// ComplexMap unique attributes NOT set by file (mapBlock elements)
				complexMapRefSetMember.setMapBlock(1); // default value
				complexMapRefSetMember.setMapBlockRule(null); // no default
				complexMapRefSetMember.setMapBlockAdvice(null); // no default

				// Terminology attributes
				complexMapRefSetMember.setTerminology("SNOMEDCT");
				complexMapRefSetMember.setTerminologyVersion(version);

				// Retrieve Concept
				complexMapRefSetMember.setConcept(getConcept(firstToken,
						complexMapRefSetMember.getTerminology(),
						complexMapRefSetMember.getTerminologyVersion()));

				manager.persist(complexMapRefSetMember);

				i++;
			}
		}
		getLog().info(Integer.toString(i) + " ComplexMap RefSets loaded.");

		// load Language RefSet (Language)
		getLog().info("Loading Language RefSets...");
		i = 0;

		while ((line = coreLanguageInput.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, "\t");
			LanguageRefSetMember languageRefSetMember = new LanguageRefSetMemberJpa();
			String firstToken = st.nextToken();
			if (!firstToken.equals("id")) { // header

				// Universal RefSet attributes
				languageRefSetMember.setTerminologyId(firstToken);
				languageRefSetMember.setEffectiveTime(dt.parse(st.nextToken()));
				languageRefSetMember.setActive(st.nextToken().equals("1") ? true
						: false);
				languageRefSetMember.setModuleId(Long.valueOf(st.nextToken()));
				languageRefSetMember.setRefSetId(Long.valueOf(st.nextToken()));
				firstToken = st.nextToken(); // referencedComponentId

				// Language unique attributes
				languageRefSetMember.setAcceptabilityId(Long.valueOf(st.nextToken()));

				// Terminology attributes
				languageRefSetMember.setTerminology("SNOMEDCT");
				languageRefSetMember.setTerminologyVersion(version);

				// Retrieve concept
				languageRefSetMember.setDescription(getDescription(firstToken,
						languageRefSetMember.getTerminology(),
						languageRefSetMember.getTerminologyVersion()));

				manager.persist(languageRefSetMember);

				// if (++i%1000 == 0) {getLog().info("."); }
				i++;
			}
		}
		getLog().info(Integer.toString(i) + " Language RefSets loaded.");
	}

	/**
	 * Returns the concept.
	 * 
	 * @param conceptId the concept id
	 * @return the concept
	 */

	/**
	 * Returns the Concept associated with a RefSetMember's referencedComponentId
	 * @param referencedComponentId
	 * @return the Concept associated with a RefSetMember's referenceComponentId
	 * @throws Exception
	 */
	private Concept getConcept(String terminologyId, String terminology,
		String terminologyVersion) throws Exception {

		if (conceptCache.containsKey(terminologyId + terminology
				+ terminologyVersion)) {
			return conceptCache.get(terminologyId + terminology + terminologyVersion);
		}

		Query query =
				manager
						.createQuery("select c from ConceptJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");

		/*
		 * Try to retrieve the single expected result If zero or more than one
		 * result are returned, log error and set result to null
		 */
		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);

			Concept c = (Concept) query.getSingleResult();

			conceptCache.put(terminologyId + terminology + terminologyVersion, c);

			return c;

		} catch (NoResultException e) {
			getLog().info(
					"Concept query for terminologyId = " + terminologyId
							+ ", terminology = " + terminology + ", terminologyVersion = "
							+ terminologyVersion + " returned no results!");
			return null;
		} catch (NonUniqueResultException e) {
			getLog().info(
					"Concept query for terminologyId = " + terminologyId
							+ ", terminology = " + terminology + ", terminologyVersion = "
							+ terminologyVersion + " returned multiple results!");
			return null;
		}
	}

	private Description getDescription(String terminologyId, String terminology,
		String terminologyVersion) throws Exception {

		if (descriptionCache.containsKey(terminologyId + terminology
				+ terminologyVersion)) {
			return descriptionCache.get(terminologyId + terminology
					+ terminologyVersion);
		}

		Query query =
				manager
						.createQuery("select d from DescriptionJpa d where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");

		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);

			Description d = (Description) query.getSingleResult();

			descriptionCache.put(terminologyId + terminology + terminologyVersion, d);

			return d;

		} catch (NoResultException e) {
			getLog().info(
					"Description query for terminologyId = " + terminologyId
							+ ", terminology = " + terminology + ", terminologyVersion = "
							+ terminologyVersion + " returned no results!");
			return null;
		} catch (NonUniqueResultException e) {
			getLog().info(
					"Description query for terminologyId = " + terminologyId
							+ ", terminology = " + terminology + ", terminologyVersion = "
							+ terminologyVersion + " returned multiple results!");
			return null;
		}
	}

	/**
	 * Opens input files.
	 * 
	 * @throws Exception if something goes wrong
	 */
	private void openInputFiles() throws Exception {

		// CORE
		coreTerminologyInputDir = new File(coreInputDir, "/Terminology/");
		getLog().info(
				"  Core Input Dir = " + coreTerminologyInputDir.toString() + " "
						+ coreTerminologyInputDir.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Relationship_")) {
				if (coreRelInputFile != null)
					throw new MojoFailureException("Multiple Relationships Files!");
				coreRelInputFile = f;
			}
		}
		getLog().info(
				"  Core Rel Input File = " + coreRelInputFile.toString() + " "
						+ coreRelInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_StatedRelationship_")) {
				if (coreStatedRelInputFile != null)
					throw new MojoFailureException("Multiple Stated Relationships Files!");
				coreStatedRelInputFile = f;
			}
		}
		getLog().info(
				"  Core Stated Rel Input File = " + coreStatedRelInputFile.toString()
						+ " " + coreStatedRelInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Concept_")) {
				if (coreConceptInputFile != null)
					throw new MojoFailureException("Multiple Concept Files!");
				coreConceptInputFile = f;
			}
		}
		getLog().info(
				"  Core Concept Input File = " + coreConceptInputFile.toString() + " "
						+ coreConceptInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Description_")) {
				if (coreDescriptionInputFile != null)
					throw new MojoFailureException("Multiple Description Files!");
				coreDescriptionInputFile = f;
			}
		}
		getLog().info(
				"  Core Description Input File = "
						+ coreDescriptionInputFile.toString() + " "
						+ coreDescriptionInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_Identifier_")) {
				if (coreIdentifierInputFile != null)
					throw new MojoFailureException("Multiple Identifier Files!");
				coreIdentifierInputFile = f;
			}
		}
		getLog().info(
				"  Core Identifier Input File = " + coreIdentifierInputFile.toString()
						+ " " + coreIdentifierInputFile.exists());

		for (File f : coreTerminologyInputDir.listFiles()) {
			if (f.getName().contains("sct2_TextDefinition_")) {
				if (coreTextDefinitionInputFile != null)
					throw new MojoFailureException("Multiple TextDefinition Files!");
				coreTextDefinitionInputFile = f;
			}
		}
		if (coreTextDefinitionInputFile != null) {
			getLog().info(
					"  Core Text Definition Input File = "
							+ coreTextDefinitionInputFile.toString() + " "
							+ coreTextDefinitionInputFile.exists());
		}
		coreRefsetInputDir = new File(coreInputDir, "/Refset/");
		coreContentInputDir = new File(coreRefsetInputDir, "/Content/");
		getLog().info(
				"  Core Input Dir = " + coreContentInputDir.toString() + " "
						+ coreContentInputDir.exists());

		for (File f : coreContentInputDir.listFiles()) {
			if (f.getName().contains("Refset_Simple")) {
				if (coreSimpleRefsetInputFile != null)
					throw new MojoFailureException("Multiple Simple Refset Files!");
				coreSimpleRefsetInputFile = f;
			}
		}
		getLog().info(
				"  Core Simple Refset Input File = "
						+ coreSimpleRefsetInputFile.toString() + " "
						+ coreSimpleRefsetInputFile.exists());

		for (File f : coreContentInputDir.listFiles()) {
			if (f.getName().contains("Refset_Association")) {
				if (coreAssociationReferenceInputFile != null)
					throw new MojoFailureException(
							"Multiple Association Reference Files!");
				coreAssociationReferenceInputFile = f;
			}
		}
		getLog().info(
				"  Core Association Reference Input File = "
						+ coreAssociationReferenceInputFile.toString() + " "
						+ coreAssociationReferenceInputFile.exists());

		for (File f : coreContentInputDir.listFiles()) {
			if (f.getName().contains("Refset_Attribute")) {
				if (coreAttributeValueInputFile != null)
					throw new MojoFailureException("Multiple Attribute Value Files!");
				coreAttributeValueInputFile = f;
			}
		}
		getLog().info(
				"  Core Attribute Value Input File = "
						+ coreAttributeValueInputFile.toString() + " "
						+ coreAttributeValueInputFile.exists());

		coreCrossmapInputDir = new File(coreRefsetInputDir, "/Crossmap/");
		getLog().info(
				"  Crossmap Input Dir = " + coreCrossmapInputDir.toString() + " "
						+ coreCrossmapInputDir.exists());

		for (File f : coreCrossmapInputDir.listFiles()) {
			if (f.getName().contains("ComplexMap")) {
				if (coreComplexMapInputFile != null)
					throw new MojoFailureException("Multiple Complex Map Files!");
				coreComplexMapInputFile = f;
			}
		}
		if (coreComplexMapInputFile != null) {
			getLog().info(
					"  Core Complex Map Input File = "
							+ coreComplexMapInputFile.toString() + " "
							+ coreComplexMapInputFile.exists());
		}

		for (File f : coreCrossmapInputDir.listFiles()) {
			if (f.getName().contains("SimpleMap")) {
				if (coreSimpleMapInputFile != null)
					throw new MojoFailureException("Multiple Simple Map Files!");
				coreSimpleMapInputFile = f;
			}
		}
		getLog().info(
				"  Core Simple Map Input File = " + coreSimpleMapInputFile.toString()
						+ " " + coreSimpleMapInputFile.exists());

		coreLanguageInputDir = new File(coreRefsetInputDir, "/Language/");
		getLog().info(
				"  Core Language Input Dir = " + coreLanguageInputDir.toString() + " "
						+ coreLanguageInputDir.exists());

		for (File f : coreLanguageInputDir.listFiles()) {
			if (f.getName().contains("Language")) {
				if (coreLanguageInputFile != null)
					throw new MojoFailureException("Multiple Language Files!");
				coreLanguageInputFile = f;
			}
		}
		getLog().info(
				"  Core Language Input File = " + coreLanguageInputFile.toString()
						+ " " + coreLanguageInputFile.exists());

		coreMetadataInputDir = new File(coreRefsetInputDir, "/Metadata/");
		getLog().info(
				"  Core Metadata Input Dir = " + coreMetadataInputDir.toString() + " "
						+ coreMetadataInputDir.exists());

		coreRelInput = new BufferedReader(new FileReader(coreRelInputFile));
		coreStatedRelInput =
				new BufferedReader(new FileReader(coreStatedRelInputFile));
		coreConceptInput = new BufferedReader(new FileReader(coreConceptInputFile));
		coreDescInput =
				new BufferedReader(new FileReader(coreDescriptionInputFile));
		coreSimpleRefsetInput =
				new BufferedReader(new FileReader(coreSimpleRefsetInputFile));
		coreAssociationReferenceInput =
				new BufferedReader(new FileReader(coreAssociationReferenceInputFile));
		coreAttributeValueInput =
				new BufferedReader(new FileReader(coreAttributeValueInputFile));
		if (coreComplexMapInputFile != null)
			coreComplexMapInput =
					new BufferedReader(new FileReader(coreComplexMapInputFile));
		coreSimpleMapInput =
				new BufferedReader(new FileReader(coreSimpleMapInputFile));
		coreLanguageInput =
				new BufferedReader(new FileReader(coreLanguageInputFile));
		coreIdentifierInput =
				new BufferedReader(new FileReader(coreIdentifierInputFile));
		if (coreTextDefinitionInputFile != null)
			coreTextDefinitionInput =
					new BufferedReader(new FileReader(coreTextDefinitionInputFile));
	}

	/**
	 * Sets and logs the version based on Concept input filename
	 */
	private void getVersion() {
		int index = coreConceptInputFile.getName().indexOf(".txt");
		version = coreConceptInputFile.getName().substring(index - 8, index);
		getLog().info("Version " + version);
	}

	/**
	 * Closes all files.
	 * @throws Exception if something goes wrong
	 */
	private void closeAllFiles() throws Exception {
		coreRelInput.close();
		coreStatedRelInput.close();
		coreConceptInput.close();
		coreDescInput.close();
		coreSimpleRefsetInput.close();
		coreAssociationReferenceInput.close();
		coreAttributeValueInput.close();
		if (coreComplexMapInput != null)
			coreComplexMapInput.close();
		coreSimpleMapInput.close();
		coreLanguageInput.close();
		coreIdentifierInput.close();
		if (coreTextDefinitionInput != null)
			coreTextDefinitionInput.close();
	}
}
