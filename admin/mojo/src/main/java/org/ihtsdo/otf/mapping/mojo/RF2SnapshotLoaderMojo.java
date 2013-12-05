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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
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
 *     <plugin>
 *       <groupId>org.ihtsdo.otf.mapping</groupId>
 *       <artifactId>mapping-admin-mojo</artifactId>
 *       <version>${project.version}</version>
 *       <executions>
 *         <execution>
 *           <id>load-rf2-snapshot</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>load-rf2-snapshot</goal>
 *           </goals>
 *           <configuration>
 *             <propertiesFile>${project.build.directory}/generated-sources/org/ihtsdo</propertiesFile>
 *           </configuration>
 *         </execution>
 *       </executions>
 *     </plugin>
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
	
	/** String for core input directory */
	private String coreInputDirString;
	
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
	
	/** The core complex map input file. */
	private File coreExtendedMapInputFile = null;

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
	
	/** The core extended map input. */
	private BufferedReader coreExtendedMapInput = null;

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
	
	// directory for sorted files
	private File sorted_files;
	
	// filenames for sorted files
	private File concepts_by_concept_file,
		descriptions_by_concept_file, descriptions_by_description_file,
		relationships_by_source_concept_file, relationships_by_dest_concept_file,
		language_refsets_by_description_file,
		attribute_refsets_by_concept_file,
		simple_refsets_by_concept_file, 
		simple_map_refsets_by_concept_file, 
		complex_map_refsets_by_concept_file,
		extended_map_refsets_by_concept_file;
	
	// buffered readers for sorted files
	private BufferedReader concepts_by_concept,
		descriptions_by_description,
		relationships_by_source_concept,
		language_refsets_by_description,
		attribute_refsets_by_concept,
		simple_refsets_by_concept, 
		simple_map_refsets_by_concept, 
		complex_map_refsets_by_concept,
		extended_map_refsets_by_concept;
	
	// hashmap used for determioning defaultPreferredNames during file loads
	Map<String, String> defaultPreferredNames = new HashMap<String, String>();

	/** The date format. */
	private SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");
	
	/** The version. */
	private String version = "";
	
	/** the defaultPreferredNames values */
	private Long dpnTypeId;
	private Long dpnRefSetId;
	private Long dpnAcceptabilityId;

	/** The manager. */
	private EntityManager manager;
	
	/** The envers audit manager */
	private AuditReader reader;
	
	// TODO: caching for older version, replace with manager.find
    private Map<String, Concept> conceptCache = new HashMap<>(); // used to speed Concept assignment to ConceptRefSetMembers
	private Map<String, Long> descriptionCache = new HashMap<>(); // speeds Description assignment to DescriptionRefSetMembers
	

	/** Efficiency testing */
	private long startTime, startTimeOrig;
	int i;
	
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
	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoFailureException {
		try {
			
			startTimeOrig = System.nanoTime();

			getLog().info("  In RF2SnapshotLoader.java");
			
					
		    // load Properties file
			Properties  properties = new Properties();
			FileInputStream propertiesInputStream = new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);
		     
			// set the input directory
			coreInputDirString = properties.getProperty("loader.input.data");
			coreInputDir = new File(coreInputDirString);
		     
			// set the parameters for determining defaultPreferredNames
			dpnTypeId = Long.valueOf(properties.getProperty("loader.defaultPreferredNames.typeId"));      
			dpnRefSetId = Long.valueOf(properties.getProperty("loader.defaultPreferredNames.refSetId"));
			dpnAcceptabilityId = Long.valueOf(properties.getProperty("loader.defaultPreferredNames.acceptabilityId"));

			// close the Properties file
			propertiesInputStream.close();
			      
      	    // create Entitymanager
			EntityManagerFactory factory =
					Persistence.createEntityManagerFactory("MappingServiceDS");
			manager = factory.createEntityManager();
			
			// create the audit reader
			reader = AuditReaderFactory.get( manager );

			// Preparation
			openInputFiles();	
			getVersion();
			
			// Prepare sorted input files
			getLog().info("Sorting Files...");
			startTime = System.nanoTime();
			prepareSortedFiles();
			getLog().info("Files sorted in " + getElapsedTime() + "s");
			
			closeAllInputFiles();
			
			
			EntityTransaction tx = manager.getTransaction();
			try {
				
				// truncate all the tables that we are going to use first
				tx.begin();				
				
				// truncate RefSets
				Query query = manager.createQuery("DELETE From SimpleRefSetMemberJpa rs");
				int deleteRecords=query.executeUpdate();
				getLog().info("simple_ref_set records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From SimpleMapRefSetMemberJpa rs");
				deleteRecords=query.executeUpdate();
				getLog().info("simple_map_ref_set records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From ComplexMapRefSetMemberJpa rs");
				deleteRecords=query.executeUpdate();
				getLog().info("complex_map_ref_set records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From AttributeValueRefSetMemberJpa rs");
				deleteRecords=query.executeUpdate();
				getLog().info("attribute_value_ref_set records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From LanguageRefSetMemberJpa rs");
				deleteRecords=query.executeUpdate();
				getLog().info("language_ref_set records deleted: " + deleteRecords);
				
				// Truncate Terminology Elements
				query = manager.createQuery("DELETE From DescriptionJpa d");
				deleteRecords=query.executeUpdate();
				getLog().info("description records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From RelationshipJpa r");
				deleteRecords=query.executeUpdate();
				getLog().info("relationship records deleted: " + deleteRecords);
				query = manager.createQuery("DELETE From ConceptJpa c");
				deleteRecords=query.executeUpdate();
				getLog().info("concept records deleted: " + deleteRecords);
				
				tx.commit();
	
				// load Concepts
				if (concepts_by_concept != null) {
					startTime = System.nanoTime();
					getLog().info("Loading Concepts...");
					tx.begin();
					loadConcepts();
					tx.commit();
					getLog().info(Integer.toString(i) + " Concepts loaded in " + getElapsedTime() + "s");
				}

				//List<Number> revNumbers = reader.getRevisions(ConceptJpa.class, testconcept.getId());
				//getLog().info("concept: " + testconcept.getTerminologyId() + " - Versions: " + revNumbers.toString());
						
				// load Descriptions
				if (descriptions_by_description != null) {
					getLog().info("Loading Descriptions...");
					startTime = System.nanoTime();
					tx.begin();
					loadDescriptions();
					tx.commit(); 
					getLog().info(Integer.toString(i) + " Descriptions loaded in " + getElapsedTime().toString() + "s");
				}

				// load Language RefSet (Language)	
				if (language_refsets_by_description != null) {
					getLog().info("Loading Language RefSets...");
					startTime = System.nanoTime();
					tx.begin();
					loadLanguageRefSets();
					tx.commit();
					getLog().info(Integer.toString(i) + " Language RefSets loaded in " + getElapsedTime().toString() + "s");
				}
				
				 // load Relationships
				if (relationships_by_source_concept != null) {
					getLog().info("Loading Relationships...");
					startTime = System.nanoTime();
					tx.begin();
					loadRelationships();
					tx.commit();
					getLog().info(Integer.toString(i) + " Relationships loaded in " + getElapsedTime().toString() + "s");
				}

				// load Simple RefSets (Content)
				if (simple_refsets_by_concept != null) {
					getLog().info("Loading Simple RefSets...");
					startTime = System.nanoTime();
					tx.begin();
					loadSimpleRefSets();
					tx.commit();
					getLog().info(Integer.toString(i) + " Simple RefSets loaded in " + getElapsedTime().toString() + "s");
				}
				
				// load SimpleMapRefSets
				if (simple_map_refsets_by_concept != null) {
					getLog().info("Loading SimpleMap RefSets...");
					startTime = System.nanoTime();
					tx.begin();
					loadSimpleMapRefSets();
					tx.commit();
					getLog().info(Integer.toString(i) + " SimpleMap RefSets loaded in " + getElapsedTime().toString() + "s");
				}
				
				// load ComplexMapRefSets
				if (complex_map_refsets_by_concept != null) {
					getLog().info("Loading ComplexMap RefSets...");
					startTime = System.nanoTime();
					tx.begin();
					loadComplexMapRefSets();
					tx.commit();
					getLog().info(Integer.toString(i) + " ComplexMap RefSets loaded in " + getElapsedTime().toString() + "s");
				}
				
				// load ExtendedMapRefSets
				if (extended_map_refsets_by_concept != null) {
					getLog().info("Loading ExtendedMap RefSets...");
					startTime = System.nanoTime();
					tx.begin();
					loadExtendedMapRefSets();
					tx.commit();
					getLog().info(Integer.toString(i) + " ExtendedMap RefSets loaded in " + getElapsedTime().toString() + "s");
				}
				
				// load AttributeValue RefSets (Content)
				if (attribute_refsets_by_concept != null) {
					getLog().info("Loading AttributeValue RefSets...");	
					startTime = System.nanoTime();
					tx.begin();
					loadAttributeValueRefSets();
					tx.commit();
					getLog().info(Integer.toString(i) + " AttributeValue RefSets loaded in " + getElapsedTime().toString() + "s");
				}
				
				getLog().info("Total elapsed time for run: " + getTotalElapsedTimeStr());
				
			} catch (Exception e) {
				e.printStackTrace();
				tx.rollback();
			}

			// Clean-up
			manager.close();
			factory.close();
			closeAllSortedFiles();

		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}
	}
	/**
	 * 
	 * @param file
	 */
	public static void deleteSortedFiles(File file) {
	
		// Check if file is directory/folder
		if(file.isDirectory()) {
			// Get all files in the folder
			File[] files=file.listFiles();
		
			for(int i=0;i<files.length;i++) {
				// Delete each file in the folder
				deleteSortedFiles(files[i]);
			}
			// Delete the folder
			file.delete();
		}
		else {
			// Delete the file if it is not a folder
			file.delete();
		}
	}
	
	// Used for debugging/efficiency monitoring
	private Long getElapsedTime() {
		return (System.nanoTime() - startTime) / 1000000000;
	}
	
	private String getTotalElapsedTimeStr() {
	
		Long resultnum = (System.nanoTime() - startTimeOrig) / 1000000000;
		String result = resultnum.toString() + "s";
		resultnum = resultnum / 60;
		result = result + " / " + resultnum.toString() + "m";
		resultnum = resultnum / 60;
		result = result + " / " + resultnum.toString() + "h";
		return result;
		
	}
	
	/**
	 * File management for sorted files; calls sort_RF2_Files
	 * 
	 * @throws Exception the exception
	 */
	// ************************************************************************************* //
	// 
	// ************************************************************************************* //
	private void prepareSortedFiles() throws Exception {
		
		// ******************** //
		// Initial file set up  //
		// ******************** //
		
		sorted_files 							= new File(coreInputDir, "/RF2-sorted-temp/");
		
		// delete any existing temporary files
		deleteSortedFiles(sorted_files);
		
		// test whether file/folder still exists (i.e. delete error)
		if ( sorted_files.exists()) {
			throw new MojoFailureException("Could not delete existing sorted files folder " + sorted_files.toString());
		}
		
		// attempt to make sorted files directory
		if (sorted_files.mkdir()) {
			getLog().info("Creating new sorted files folder " + sorted_files.toString());
		} else {
			throw new MojoFailureException("Could not create temporary sorted file folder " + sorted_files.toString());
		}
		
		concepts_by_concept_file 				= new File(sorted_files, "concepts_by_concept.sort");
		descriptions_by_concept_file 			= new File(sorted_files, "descriptions_by_concept.sort");
		descriptions_by_description_file 		= new File(sorted_files, "descriptions_by_description.sort");
		relationships_by_source_concept_file	= new File(sorted_files, "relationship_by_source_concept.sort");
		relationships_by_dest_concept_file 	    = new File(sorted_files, "relationship_by_dest_concept.sort");
		language_refsets_by_description_file	= new File(sorted_files, "language_refsets_by_description.sort");
		attribute_refsets_by_concept_file		= new File(sorted_files, "attribute_refsets_by_concept.sort");
		simple_refsets_by_concept_file			= new File(sorted_files, "simple_refsets_by_concept.sort");
		simple_map_refsets_by_concept_file		= new File(sorted_files, "simple_map_refsets_by_concept.sort");
		complex_map_refsets_by_concept_file		= new File(sorted_files, "complex_map_refsets_by_concept.sort");
		extended_map_refsets_by_concept_file    = new File(sorted_files, "extended_map_refsets_by_concept.sort");
		
		
		// **************** //
		// Initial sorting  //
		// **************** //
		
		sort_RF2_files();	
		
		// **************** //
		// Open files       //
		// **************** //
		
		// Concepts
		try {
			concepts_by_concept				= new BufferedReader(new FileReader(concepts_by_concept_file));
		} catch (Exception e) {
			getLog().info("Could not open " + concepts_by_concept_file.toString());
			concepts_by_concept_file = null;
		}
		
		// Relationships by source concept
		try {
			relationships_by_source_concept				= new BufferedReader(new FileReader(relationships_by_source_concept_file));
		} catch (Exception e) {
			getLog().info("Could not open " + relationships_by_source_concept_file.toString());
			relationships_by_source_concept_file = null;
		}
		
		
		// Attenpt to open files and load first Description / RefSet Member
		try {
			descriptions_by_description					= new BufferedReader(new FileReader(descriptions_by_description_file));
		} catch (Exception e) {
			getLog().info("Could not open " + descriptions_by_description_file.toString());
		}
		
		try {
			language_refsets_by_description				= new BufferedReader(new FileReader(language_refsets_by_description_file));
		} catch (Exception e) {
			getLog().info("Could not open " + language_refsets_by_description_file.toString());
		}
		
		
		// ******************************************************* //
		// Component RefSet Members                                //
		// ******************************************************* //
		
		// Attribute Value
		try {
			attribute_refsets_by_concept 				= new BufferedReader(new FileReader(attribute_refsets_by_concept_file));
		} catch(Exception e) {
			getLog().info("Could not open " + attribute_refsets_by_concept_file.toString());
		}
		
		// Simple
		try {
			simple_refsets_by_concept 					= new BufferedReader(new FileReader(simple_refsets_by_concept_file));
		} catch(Exception e) {
			getLog().info("Could not open " + simple_refsets_by_concept_file.toString());
		}
			
		// Simple Map
		try {
			simple_map_refsets_by_concept 				= new BufferedReader(new FileReader(simple_map_refsets_by_concept_file));
		} catch(Exception e) {
			getLog().info("Could not open " + simple_map_refsets_by_concept_file.toString());
		}
		
		// Complex map
		try {
			complex_map_refsets_by_concept 				= new BufferedReader(new FileReader(complex_map_refsets_by_concept_file));
		} catch (Exception e) {
			getLog().info("Could not open " + complex_map_refsets_by_concept_file.toString());
		}
		
		// Extended map
		try {
			extended_map_refsets_by_concept				= new BufferedReader(new FileReader(extended_map_refsets_by_concept_file));
		} catch (Exception e) {
			getLog().info("Could not open " + extended_map_refsets_by_concept_file.toString());
		}
	}
	/**
	 * Sorts all files by concept or referencedComponentId
	 * 
	 * @throws Exception the exception
	 */
	private void sort_RF2_files() throws Exception {
		
		Comparator<String> comp;
		
		// ****************//
		// Components      //
		// ****************//
		
		// Concepts sorted on Id, first field
		comp = new Comparator<String>() {
				public int compare(String s1, String s2) {
					String v1[] = s1.split("\t");
					String v2[] = s2.split("\t"); 
					return v1[0].compareTo(v2[0]);
				}
		};	
		getLog().info("Sorting " + coreConceptInputFile.toString() + " by concept into " + concepts_by_concept_file.toString());
		sort(coreConceptInputFile.toString(), concepts_by_concept_file.toString(), comp);
		
		// Descriptions sorted on Id, first field
		comp = new Comparator<String>() {
			public int compare(String s1, String s2) {
				String v1[] = s1.split("\t");
				String v2[] = s2.split("\t"); 
				return v1[0].compareTo(v2[0]);
			}
		};
		getLog().info("Sorting " + coreDescriptionInputFile.toString() + " by description into " + descriptions_by_description_file.toString());
		sort(coreDescriptionInputFile.toString(), descriptions_by_description_file.toString(), comp);
		
		// Descriptions sorted on conceptId, fifth field
		comp = new Comparator<String>() {
			public int compare(String s1, String s2) {
				String v1[] = s1.split("\t");
				String v2[] = s2.split("\t"); 
				return v1[4].compareTo(v2[4]);
			}
		};
		getLog().info("Sorting " + coreDescriptionInputFile.toString() + " by concept into " + descriptions_by_concept_file.toString());
		sort(coreDescriptionInputFile.toString(), descriptions_by_concept_file.toString(), comp);
				
		// Relationships sorted by source conceptId, fifth field
		comp = new Comparator<String>() {
			public int compare(String s1, String s2) {
				String v1[] = s1.split("\t");
				String v2[] = s2.split("\t"); 
				return v1[4].compareTo(v2[4]);
			}
		};
		sort(coreRelInputFile.toString(), relationships_by_source_concept_file.toString(), comp);
		
		
		 // Relationships sorted by destination conceptId, sixth field
		comp = new Comparator<String>() {
			public int compare(String s1, String s2) {
				String v1[] = s1.split("\t");
				String v2[] = s2.split("\t"); 
				return v1[5].compareTo(v2[5]);
			}
		};
		sort(coreRelInputFile.toString(), relationships_by_dest_concept_file.toString(), comp);
		
		
		// ******************//
		// Component RefSets //
		// ******************//
		// Attribute value refsets are sorted on referencedComponentId, sixth field
		comp = new Comparator<String>() {
				public int compare(String s1, String s2) {
					String v1[] = s1.split("\t");
					String v2[] = s2.split("\t"); 
					return v1[5].compareTo(v2[5]);
				}
		};
		getLog().info("Sorting " + coreAttributeValueInputFile.toString() + " by concept into " + attribute_refsets_by_concept_file.toString());
		sort(coreAttributeValueInputFile.toString(), attribute_refsets_by_concept_file.toString(), comp);
		
		// Simple refsets are sorted on referencedComponentId, sixth field
		comp = new Comparator<String>() {
				public int compare(String s1, String s2) {
					String v1[] = s1.split("\t");
					String v2[] = s2.split("\t"); 
					return v1[5].compareTo(v2[5]);
				}
		};
		getLog().info("Sorting " + coreSimpleRefsetInputFile.toString() + " by concept into " + simple_refsets_by_concept_file.toString());
		sort(coreSimpleRefsetInputFile.toString(), simple_refsets_by_concept_file.toString(), comp);
		
		// Simple map refsets are sorted on referencedComponentId, sixth field
		comp = new Comparator<String>() {
				public int compare(String s1, String s2) {
					String v1[] = s1.split("\t");
					String v2[] = s2.split("\t"); 
					return v1[5].compareTo(v2[5]);
				}
		};
		getLog().info("Sorting " + coreSimpleMapInputFile.toString() + " by concept into " + simple_map_refsets_by_concept_file.toString());
		sort(coreSimpleMapInputFile.toString(), simple_map_refsets_by_concept_file.toString(), comp);
		
		// Complex map refsets are sorted on referencedComponentId, sixth field
		comp = new Comparator<String>() {
				public int compare(String s1, String s2) {
					String v1[] = s1.split("\t");
					String v2[] = s2.split("\t"); 
					return v1[5].compareTo(v2[5]);
				}
		};
		getLog().info("Sorting " + coreComplexMapInputFile.toString() + " by concept into " + complex_map_refsets_by_concept_file.toString());
		sort(coreComplexMapInputFile.toString(), complex_map_refsets_by_concept_file.toString(), comp);
		
		// Extended map refsets are sorted on referencedComponentId, sixth field
		comp = new Comparator<String>() {
				public int compare(String s1, String s2) {
					String v1[] = s1.split("\t");
					String v2[] = s2.split("\t"); 
					return v1[5].compareTo(v2[5]);
				}
		};
		getLog().info("Sorting " + coreExtendedMapInputFile.toString() + " by concept into " + extended_map_refsets_by_concept_file.toString());
		sort(coreExtendedMapInputFile.toString(), extended_map_refsets_by_concept_file.toString(), comp);
	
		// *********************//
		// Description RefSets  //
		// *********************//
		
		// Language RefSets sorted on DescriptionId
		comp = new Comparator<String>() {
			public int compare(String s1, String s2) {
				String v1[] = s1.split("\t");
				String v2[] = s2.split("\t"); 
				return v1[4].compareTo(v2[4]);
			}
		};
		sort(coreLanguageInputFile.toString(), language_refsets_by_description_file.toString(), comp);
		
	}
	
	
	
	/**
	 * Returns the concept.
	 * 
	 * @param conceptId the concept id
	 * @return the concept
	 */
	private Concept getConcept(String terminologyId, String terminology, String terminologyVersion ) throws Exception {
		
		if (conceptCache.containsKey(terminologyId + terminology + terminologyVersion)) {
			
			// uses hibernate first-level cache
			return conceptCache.get(terminologyId + terminology + terminologyVersion);
		}
		
		Query query = manager.createQuery("select c from ConceptJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		
		
		//  Try to retrieve the single expected result
		//  If zero or more than one result are returned, log error and set result to null
		 
		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);

			Concept c = (Concept) query.getSingleResult();

			conceptCache.put(terminologyId + terminology + terminologyVersion, c);
			
			return c;
			
		} catch (NoResultException e) {
			getLog().info("Concept query for terminologyId = " + terminologyId + ", terminology = " + terminology + ", terminologyVersion = " + terminologyVersion + " returned no results!");
			return null;		
		} catch (NonUniqueResultException e) {
			getLog().info("Concept query for terminologyId = " + terminologyId + ", terminology = " + terminology + ", terminologyVersion = " + terminologyVersion + " returned multiple results!");
			return null;
		}	
			
	}
	
	/**
	 * Returns the description.
	 * 
	 * @param descriptionId the description id
	 * @return the description
	 */
	private Description getDescription(String terminologyId, String terminology, String terminologyVersion ) throws Exception {
		
		if (descriptionCache.containsKey(terminologyId + terminology + terminologyVersion)) {
			// uses hibernate first-level cache
			return manager.find(DescriptionJpa.class, descriptionCache.get(terminologyId + terminology + terminologyVersion));
		}
		
		Query query = manager.createQuery("select d from DescriptionJpa d where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");
		
		
		try {
			query.setParameter("terminologyId", terminologyId);
			query.setParameter("terminology", terminology);
			query.setParameter("terminologyVersion", terminologyVersion);

			Description d = (Description) query.getSingleResult();

			descriptionCache.put(terminologyId + terminology + terminologyVersion, d.getId());
			
			return d;
			
		} catch (NoResultException e) {
			getLog().info("Description query for terminologyId = " + terminologyId + ", terminology = " + terminology + ", terminologyVersion = " + terminologyVersion + " returned no results!");
			return null;		
		} catch (NonUniqueResultException e) {
			getLog().info("Description query for terminologyId = " + terminologyId + ", terminology = " + terminology + ", terminologyVersion = " + terminologyVersion + " returned multiple results!");
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

		// Choose map folder based on mini or full dataset
		// -> RF2Release: the full RF2 release -> map content is in "Map"
		// -> usext-mini-data: the truncated dataset -> map content is in "Crossmap"

		if (coreInputDirString.contains("mini-data")) {
			coreCrossmapInputDir = new File(coreRefsetInputDir, "/Crossmap/");
		} else if (coreInputDirString.contains("RF2Release")) {
			coreCrossmapInputDir = new File(coreRefsetInputDir, "/Map/");
		} else {
			throw new MojoFailureException("Cannot identify crossmap folder from dataset file structure!");
		}
		
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
			if (f.getName().contains("ExtendedMap")) {
				if (coreExtendedMapInputFile != null)
					throw new MojoFailureException("Multiple Extended Map Files!");
				coreExtendedMapInputFile = f;
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
		if (coreExtendedMapInputFile != null)
			coreExtendedMapInput =
					new BufferedReader(new FileReader(coreExtendedMapInputFile));
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
	private void closeAllInputFiles() throws Exception {
		coreRelInput.close();
		coreStatedRelInput.close();
		coreConceptInput.close();
		coreDescInput.close();
		coreSimpleRefsetInput.close();
		coreAssociationReferenceInput.close();
		coreAttributeValueInput.close();
		if (coreComplexMapInput != null)
			coreComplexMapInput.close();
		if (coreExtendedMapInput != null)
			coreComplexMapInput.close();
		coreSimpleMapInput.close();
		coreLanguageInput.close();
		coreIdentifierInput.close();
		if (coreTextDefinitionInput != null)
			coreTextDefinitionInput.close();
	}
	
	/**
	 * Closes all sorted temporary files.
	 * @throws Exception if something goes wrong
	 */	
	private void closeAllSortedFiles() throws Exception {
		 concepts_by_concept.close();
		descriptions_by_description.close();
		relationships_by_source_concept.close();
		language_refsets_by_description.close();
		attribute_refsets_by_concept.close();
		simple_refsets_by_concept.close(); 
		simple_map_refsets_by_concept.close(); 
		complex_map_refsets_by_concept.close();
		extended_map_refsets_by_concept.close();
	}
	
	/**

	   * Sort the specified file using the specified {@link Comparator} and
	   * optionally sort uniquely.
	   * @param filename the file to sort
	   * @param fileout the destination sorted file
	   * @param comp the {@link Comparator}
	   * @throws IOException if failed to sort
	   */

	  public static void sort(String filename, String fileout, Comparator<String> comp) throws IOException {

	    //
	    // Vars
	    //
	    List<String> lines = null;
	    List<File> files1 = new ArrayList<File>();
	    List<File> files2 = new ArrayList<File>();
	    String line;
	    final File orig_file = new File(filename).getAbsoluteFile();
	    final File dest_file = new File(fileout).getAbsoluteFile();
	    final File sortdir = new File(dest_file.getParent());
	  
	    //
	    // Open file
	    //
	    final BufferedReader in = new BufferedReader(new FileReader(orig_file));
	    //
	    
	    // Break input file into files with max size of 16MB and then sort it
	    //

	    int size_so_far = 0;
	    final int segment_size = 32*1024*1024;
	    
	    // get and save header
	    String header_line = in.readLine(); //
	    
	    while ( (line = in.readLine()) != null) {

	      if (size_so_far == 0) {

	        lines = new ArrayList<String>(10000);

	      }

	      lines.add(line);
	      size_so_far += line.length();

	      if (size_so_far > segment_size) {
	    	  
	    	 

	        sortHelper( (String[]) lines.toArray(new String[0]), files1, comp, sortdir);

	        size_so_far = 0;
	      }
	    }

	    //
	    // If there are left-over lines, create final tmp file
	    //
	    if (lines != null && lines.size() != 0 && size_so_far <= segment_size) {
	      sortHelper( (String[]) lines.toArray(new String[0]), files1, comp, sortdir);
	    }
	    

	    

	    //
	    // Calculations for pm
	    //
	    int total_files = files1.size();
	    int tmp = total_files;

	    while (tmp > 1) {

	      tmp = (int) Math.ceil(tmp / 2.0);
	      total_files += tmp;

	    }
	    //
	    // Merge sorted files
	    //
	    tmp = 0;
	    
	    while (files1.size() > 1) {

	      for (int i = 0; i < files1.size(); i += 2) {

	        tmp += 2;


	        if (files1.size() == i + 1) {
	          files2.add(files1.get(i));
	          break;
	        } else {
	          final File f = mergeSortedFiles( (File) files1.get(i),
	                                    (File) files1.get(i + 1),
	                                    comp, sortdir, header_line);

	          files2.add(f);

	          ( (File) files1.get(i)).delete();
	          ( (File) files1.get(i + 1)).delete();
	        }
	      }

	      files1 = new ArrayList<File>(files2);
	      files2.clear();
	    }

	    // rename file

	    if (files1.size() > 0) {
	      ( (File) files1.get(0)).renameTo(dest_file);
	    }
	    
	    // close input file
	    in.close();



	  }



	  /**
	   * Helper function to perform sort operations.
	   * @param lines the lines to sort
	   * @param all_tmp_files the list of files
	   * @param comp the comparator
	   * @param sortdir the sort dir
	   * @param unique whether or not to sort unique
	   * @param bom_present indicates if a Byte Order Mark was present on file
	   * @throws IOException
	   */
	  private static void sortHelper(String[] lines, List<File> all_tmp_files, Comparator<String> comp, File sortdir) throws
	      IOException {

	    //
	    // Create temp file
	    //
	    final File f = File.createTempFile("t+~", ".tmp", sortdir);

	    //
	    // Sort data for this segment
	    //
	    Arrays.sort(lines, comp);

	    //
	    // Write lines to file f
	    //
	    final BufferedWriter out =
	        new BufferedWriter(new FileWriter(f));

	    for (int i = 0; i < lines.length; i++) {
	      final String line = lines[i];
	        out.write(line);
	        out.newLine();
	        //out.flush();

	    }

	    out.flush();
	    out.close();
	    all_tmp_files.add(f);
	  }



	  /**
	   * Merge-sort two files.
	   * @return the sorted {@link File}
	   * @param files1 the first set of files
	   * @param files2 the second set of files
	   * @param comp the comparator
	   * @param dir the sort dir
	   * @param unique whether or not to sort unique
	   * @param bom_present indicates if a Byte Order Mark was present on file
	   * @throws IOException
	   */
	  private static File mergeSortedFiles(File files1, File files2,
	                                       Comparator<String> comp, File dir,
	                                       String header_line
	                                       ) throws IOException {
	    final BufferedReader in1 =
	        new BufferedReader(new FileReader(files1));

	    final BufferedReader in2 =
	        new BufferedReader(new FileReader(files2));

	    final File out_file = File.createTempFile("t+~", ".tmp", dir);

	    final BufferedWriter out =
	        new BufferedWriter(new FileWriter(out_file));

	 
	    
	    String line1 = in1.readLine();
	    String line2 = in2.readLine();
	    String line = null;
	    
	    if (!header_line.isEmpty()) {
	    	line = header_line;
	    	out.write(line);
	    	out.newLine();
	    	System.out.println("Wrote header line: " + line);
	    } {
	    	System.out.println("No header line: " + line);
	    }
	   

	    while (line1 != null || line2 != null) {

	      if (line1 == null) {
	        line = line2;
	        line2 = in2.readLine();

	      } else if (line2 == null) {

	        line = line1;
	        line1 = in1.readLine();

	      } else if (comp.compare(line1, line2) < 0) {

	        line = line1;
	        line1 = in1.readLine();

	      } else {

	        line = line2;
	        line2 = in2.readLine();

	      }

          out.write(line);
          out.newLine();

	    }

	    out.flush();
	    out.close();
	    in1.close();
	    in2.close();

	    return out_file;

	  }
	  
	/**
	 * Sorting tool: Currently unused
	 * @param file_in the input file
	 * @param column (unused)
	 * @return file_in the input file
	 */
	  public String sort_entries(String file_in, int column) {
			
			return file_in;
		}


/**
 * Load concepts.
 * 
 * @throws Exception the exception
 */
private void loadConcepts() throws Exception {

	String line = "";
	i = 0;
	
	while ((line = concepts_by_concept.readLine()) != null ) {
		
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
			concept.setDefaultPreferredName("null");

			manager.persist(concept);
			
			conceptCache.put(new String(firstToken  + concept.getTerminology() + concept.getTerminologyVersion()), concept);
			
			i++;
		}
	}	
}


/**
 * Load relationships.
 * 
 * @throws Exception the exception
 */
private void loadRelationships() throws Exception {

	String line = "";
	i = 0;
	
	while ((line = relationships_by_source_concept.readLine()) != null) {
		
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
			
			relationship.setSourceConcept(getConcept(source, relationship.getTerminology(), relationship.getTerminologyVersion()));
			relationship.setDestinationConcept(getConcept(target, relationship.getTerminology(), relationship.getTerminologyVersion()));
		
			manager.persist(relationship);
			
			i++;
		}
	}
}




/**
 * Load descriptions.
 * 
 * @throws Exception the exception
 */
private void loadDescriptions() throws Exception {
	
	
	String line = "";
	i = 0;
	Concept concept; 
	
	// keep concepts from extension descriptions
	while ((line = descriptions_by_description.readLine()) != null) {
		
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
			
			
			concept = getConcept(conceptId, description.getTerminology(), description.getTerminologyVersion());
			description.setConcept(concept);
			
			manager.persist(description);
			
			// add to index
			descriptionCache.put(firstToken + description.getTerminology() + description.getTerminologyVersion(), description.getId()); 
		
			i++;
		}
	}
}

/**
 * Load AttributeRefSets (Content)
 * 
 * @throws Exception the exception
 */


private void loadAttributeValueRefSets() throws Exception {
	
	String line = "";
	i = 0;
	String conceptId = "";
	
	while ((line = attribute_refsets_by_concept.readLine()) != null) {
		
		StringTokenizer st = new StringTokenizer(line, "\t");
		AttributeValueRefSetMember attributeValueRefSetMember = new AttributeValueRefSetMemberJpa();
		String firstToken = st.nextToken();
		
		if (!firstToken.equals("id")) { // header
			
			// Universal RefSet attributes
			attributeValueRefSetMember.setTerminologyId(firstToken);
			attributeValueRefSetMember.setEffectiveTime(dt.parse(st.nextToken()));
			attributeValueRefSetMember.setActive(st.nextToken().equals("1") ? true : false);
			attributeValueRefSetMember.setModuleId(Long.valueOf(st.nextToken()));
			attributeValueRefSetMember.setRefSetId(Long.valueOf(st.nextToken()));
			conceptId = st.nextToken(); // referencedComponentId
			
			// AttributeValueRefSetMember unique attributes
			attributeValueRefSetMember.setValueId(Long.valueOf(st.nextToken()));
			
			// Terminology attributes
			attributeValueRefSetMember.setTerminology("SNOMEDCT");
			attributeValueRefSetMember.setTerminologyVersion(version);
			
			// Retrieve concept -- firstToken is referencedComponentId
			attributeValueRefSetMember.setConcept(getConcept(conceptId, attributeValueRefSetMember.getTerminology(), attributeValueRefSetMember.getTerminologyVersion())); 

			manager.persist(attributeValueRefSetMember);
			
			i++;
		}
	}	
}

/**
 * Load SimpleRefSets (Content)
 * 
 * @throws Exception the exception
 */

private void loadSimpleRefSets() throws Exception {
	
	String line = "";
	i = 0;
	String conceptId = "";
	
	
	
	while ((line = simple_refsets_by_concept.readLine()) != null) {
		
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
			conceptId = st.nextToken(); // referencedComponentId
			
			// SimpleRefSetMember unique attributes
			// NONE
			
			// Terminology attributes
			simpleRefSetMember.setTerminology("SNOMEDCT");
			simpleRefSetMember.setTerminologyVersion(version);
			
			// Retrieve Concept -- firstToken is referencedComonentId
			simpleRefSetMember.setConcept(getConcept(conceptId, simpleRefSetMember.getTerminology(), simpleRefSetMember.getTerminologyVersion())); 

			manager.persist(simpleRefSetMember);
			
			i++;
		}
	}
}

/**
 * Load SimpleMapRefSets (Crossmap)
 * 
 * @throws Exception the exception
 */
private void loadSimpleMapRefSets() throws Exception {
	
	String line = "";
	i = 0;
	String conceptId = "";
	
	while ((line = simple_map_refsets_by_concept.readLine()) != null) {
		
		StringTokenizer st = new StringTokenizer(line, "\t");
		SimpleMapRefSetMember simpleMapRefSetMember = new SimpleMapRefSetMemberJpa();
		String firstToken = st.nextToken();
		
		if (!firstToken.equals("id")) { // header
			
			// Universal RefSet attributes
			simpleMapRefSetMember.setTerminologyId(firstToken);
			simpleMapRefSetMember.setEffectiveTime(dt.parse(st.nextToken()));
			simpleMapRefSetMember.setActive(st.nextToken().equals("1") ? true : false);
			simpleMapRefSetMember.setModuleId(Long.valueOf(st.nextToken()));
			simpleMapRefSetMember.setRefSetId(Long.valueOf(st.nextToken()));
			conceptId = st.nextToken(); // referencedComponentId
			
			// SimpleMap unique attributes
			simpleMapRefSetMember.setMapTarget(st.nextToken());
			
			// Terminology attributes
			simpleMapRefSetMember.setTerminology("SNOMEDCT");
			simpleMapRefSetMember.setTerminologyVersion(version);
			
			// Retrieve concept	 -- firstToken is referencedComponentId
			simpleMapRefSetMember.setConcept(getConcept(conceptId, simpleMapRefSetMember.getTerminology(), simpleMapRefSetMember.getTerminologyVersion())); 
			
			manager.persist(simpleMapRefSetMember);	
			
			i++;
		}
	}
}

/**
 * Load ComplexMapRefSets (Crossmap)
 * 
 * @throws Exception the exception
 */
private void loadComplexMapRefSets() throws Exception {

	String line = "";
	i = 0;
	String conceptId = "";

	
	while ((line = complex_map_refsets_by_concept.readLine()) != null) {
		
	
		StringTokenizer st = new StringTokenizer(line, "\t");
		ComplexMapRefSetMember complexMapRefSetMember = new ComplexMapRefSetMemberJpa();
		String firstToken = st.nextToken();
		
		if (!firstToken.equals("id")) { // header
			
			// Universal RefSet attributes
			complexMapRefSetMember.setTerminologyId(firstToken);
			complexMapRefSetMember.setEffectiveTime(dt.parse(st.nextToken()));
			complexMapRefSetMember.setActive(st.nextToken().equals("1") ? true : false);
			complexMapRefSetMember.setModuleId(Long.valueOf(st.nextToken()));
			complexMapRefSetMember.setRefSetId(Long.valueOf(st.nextToken()));; // conceptId
			conceptId = st.nextToken(); // referencedComponentId
			
			// ComplexMap unique attributes
			complexMapRefSetMember.setMapGroup(Integer.parseInt(st.nextToken()));
			String s = st.nextToken();
			System.out.println("map priority:" + s);
			complexMapRefSetMember.setMapPriority(Integer.parseInt(s));
			s = st.nextToken();
			System.out.println("map rule:" + s);
			complexMapRefSetMember.setMapRule(s);
			s = st.nextToken();
			System.out.println("map advice:" + s);
			complexMapRefSetMember.setMapAdvice(s);
			s = st.nextToken();
			System.out.println("map target:" + s);
			complexMapRefSetMember.setMapTarget(s);
			complexMapRefSetMember.setMapRelationId(Long.valueOf(st.nextToken()));
			
			// ComplexMap unique attributes NOT set by file (mapBlock elements)
			complexMapRefSetMember.setMapBlock(1); // default value
			complexMapRefSetMember.setMapBlockRule(null); // no default
			complexMapRefSetMember.setMapBlockAdvice(null); // no default
			
			// Terminology attributes
			complexMapRefSetMember.setTerminology("SNOMEDCT");
			complexMapRefSetMember.setTerminologyVersion(version);
			
			// Retrieve Concept
			complexMapRefSetMember.setConcept(getConcept(conceptId, complexMapRefSetMember.getTerminology(), complexMapRefSetMember.getTerminologyVersion())); 

			manager.persist(complexMapRefSetMember);
			
			i++;

		}
	}	
	
}
/**
 * Load ExtendedMapRefSets (Crossmap)
 * 
 * @throws Exception the exception
 */

// NOTE: ExtendedMap RefSets are loaded into ComplexMapRefSetMember
//       where mapRelationId = 	mapCategoryId
private void loadExtendedMapRefSets() throws Exception {
	
	String line = "";
	i = 0;
	String conceptId = "";
	
	while ((line = extended_map_refsets_by_concept.readLine()) != null) {
		

		StringTokenizer st = new StringTokenizer(line, "\t");
		ComplexMapRefSetMember complexMapRefSetMember = new ComplexMapRefSetMemberJpa();
		String firstToken = st.nextToken();
		
		if (!firstToken.equals("id")) { // header
			
			// Universal RefSet attributes
			complexMapRefSetMember.setTerminologyId(firstToken);
			complexMapRefSetMember.setEffectiveTime(dt.parse(st.nextToken()));
			complexMapRefSetMember.setActive(st.nextToken().equals("1") ? true : false);
			complexMapRefSetMember.setModuleId(Long.valueOf(st.nextToken()));
			complexMapRefSetMember.setRefSetId(Long.valueOf(st.nextToken()));; // conceptId
			conceptId = st.nextToken(); // referencedComponentId
			
			// ComplexMap unique attributes
			complexMapRefSetMember.setMapGroup(Integer.parseInt(st.nextToken()));
			complexMapRefSetMember.setMapPriority(Integer.parseInt(st.nextToken()));
			complexMapRefSetMember.setMapRule(st.nextToken());
			complexMapRefSetMember.setMapAdvice(st.nextToken());
			complexMapRefSetMember.setMapTarget(st.nextToken());
			firstToken = st.nextToken(); // unused, this field ignored for ExtendedMap
			complexMapRefSetMember.setMapRelationId(Long.valueOf(st.nextToken()));
			
			// ComplexMap unique attributes NOT set by file (mapBlock elements)
			complexMapRefSetMember.setMapBlock(1); // default value
			complexMapRefSetMember.setMapBlockRule(null); // no default
			complexMapRefSetMember.setMapBlockAdvice(null); // no default
			
			// Terminology attributes
			complexMapRefSetMember.setTerminology("SNOMEDCT");
			complexMapRefSetMember.setTerminologyVersion(version);
			
			// Retrieve Concept
			complexMapRefSetMember.setConcept(getConcept(conceptId, complexMapRefSetMember.getTerminology(), complexMapRefSetMember.getTerminologyVersion())); 

			manager.persist(complexMapRefSetMember);
			
			i++;
		}
	}	
}

private void loadLanguageRefSets() throws Exception {
		
	String line = "";
	i = 0;
	Concept concept;
	Description description = new DescriptionJpa();
	description.setTerminologyId("-1");
		
	while ((line = language_refsets_by_description.readLine()) != null) {
		
		StringTokenizer st = new StringTokenizer(line, "\t");
		LanguageRefSetMember languageRefSetMember = new LanguageRefSetMemberJpa();
		String firstToken = st.nextToken();
		
		if (!firstToken.equals("id")) { // header
			
			// Universal RefSet attributes
			languageRefSetMember.setTerminologyId(firstToken);
			languageRefSetMember.setEffectiveTime(dt.parse(st.nextToken()));
			languageRefSetMember.setActive(st.nextToken().equals("1") ? true : false);
			languageRefSetMember.setModuleId(Long.valueOf(st.nextToken()));
			languageRefSetMember.setRefSetId(Long.valueOf(st.nextToken()));
			firstToken = st.nextToken(); // referencedComponentId
			
			// Language unique attributes
			languageRefSetMember.setAcceptabilityId(Long.valueOf(st.nextToken()));
			
			// Terminology attributes
			languageRefSetMember.setTerminology("SNOMEDCT");
			languageRefSetMember.setTerminologyVersion(version);
			
			// Set the description
			description = getDescription(firstToken, languageRefSetMember.getTerminology(), languageRefSetMember.getTerminologyVersion()); 
			languageRefSetMember.setDescription(description);
			manager.persist(languageRefSetMember);
			
			// check if this language refset and description form the defaultPreferredName
			if(description.getTypeId().equals(dpnTypeId) && 
					languageRefSetMember.getRefSetId().equals(dpnRefSetId) && 
					languageRefSetMember.getAcceptabilityId().equals(dpnAcceptabilityId)) {
				
					concept = description.getConcept();
					
					if (!concept.getDefaultPreferredName().equals("null")) {
						getLog().info("Multiple default preferred names for concept " + concept.getTerminologyId());
					}
					
					concept.setDefaultPreferredName(description.getTerm());
					manager.persist(concept);
			}
			
			i++;
		}
	}
}
}
			
			
	
			
			



