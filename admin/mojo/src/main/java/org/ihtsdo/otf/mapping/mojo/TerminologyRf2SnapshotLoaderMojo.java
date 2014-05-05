package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.FileSorter;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.rf2.AttributeValueRefSetMember;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.Description;
import org.ihtsdo.otf.mapping.rf2.LanguageRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Relationship;
import org.ihtsdo.otf.mapping.rf2.SimpleMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.SimpleRefSetMember;
import org.ihtsdo.otf.mapping.rf2.jpa.AttributeValueRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.ConceptJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.DescriptionJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.LanguageRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.RelationshipJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.SimpleMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.rf2.jpa.SimpleRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;

import com.google.common.io.Files;

/**
 * Goal which loads an RF2 Snapshot of SNOMED CT data into a database.
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
 *          <scope>system</scope>
 *            <systemPath>${project.build.directory}/mapping-admin-loader-${project.version}.jar</systemPath>
 *         </dependency>
 *       </dependencies>
 *       <executions>
 *         <execution>
 *           <id>load-rf2-snapshot</id>
 *           <phase>package</phase>
 *           <goals>
 *             <goal>load-rf2-snapshot</goal>
 *           </goals>
 *           <configuration>
 *             <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *             <terminology>SNOMEDCT</terminology>
 *           </configuration>
 *         </execution>
 *       </executions>
 *     </plugin>
 * </pre>
 * 
 * @goal load-rf2-snapshot
 * 
 * @phase package
 */
public class TerminologyRf2SnapshotLoaderMojo extends AbstractMojo {

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

  /** The date format. */
  private final SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

  /* buffered readers for sorted files. */
  private BufferedReader conceptsByConcept, descriptionsByDescription,
      relationshipsBySourceConcept, languageRefsetsByDescription,
      attributeRefsetsByDescription, simpleRefsetsByConcept,
      simpleMapRefsetsByConcept, complexMapRefsetsByConcept,
      extendedMapRefsetsByConcept;

  /** The version. */
  private String version = null;

  /** the defaultPreferredNames values. */
  private Long dpnTypeId;

  /** The dpn ref set id. */
  private Long dpnRefSetId;

  /** The dpn acceptability id. */
  private Long dpnAcceptabilityId;

  /** hash sets for retrieving concepts. */
  private Map<String, Concept> conceptCache = new HashMap<>(); // used to

  /** hash set for storing default preferred names. */
  Map<Long, String> defaultPreferredNames = new HashMap<Long, String>();

  /** counter for objects created, reset in each load section */
  int objectCt; //

  /** the number of objects to create before committing. */
  int commitCt = 200;

  /** The factory. */
  EntityManagerFactory factory = null;
  
  /**
   * Instantiates a {@link TerminologyRf2SnapshotLoaderMojo} from the specified
   * parameters.
   * 
   */
  public TerminologyRf2SnapshotLoaderMojo() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting loading RF2 data ...");

    FileInputStream propertiesInputStream = null;
    try {

      // Track system level information
      long startTimeOrig = System.nanoTime();

      // load Properties file
      Properties properties = new Properties();
      propertiesInputStream = new FileInputStream(propertiesFile);
      properties.load(propertiesInputStream);
      propertiesInputStream.close();

      // set the input directory
      String coreInputDirString =
          properties.getProperty("loader." + terminology + ".input.data");
      File coreInputDir = new File(coreInputDirString);
      if (!coreInputDir.exists()) {
        throw new MojoFailureException("Specified loader." + terminology
            + ".input.data directory does not exist: " + coreInputDirString);
      }
      // set the parameters for determining defaultPreferredNames
      dpnTypeId =
          Long.valueOf(properties
              .getProperty("loader.defaultPreferredNames.typeId"));
      dpnRefSetId =
          Long.valueOf(properties
              .getProperty("loader.defaultPreferredNames.refSetId"));
      dpnAcceptabilityId =
          Long.valueOf(properties
              .getProperty("loader.defaultPreferredNames.acceptabilityId"));

      //
      // Determine version
      //
      File coreConceptInputFile = null;
      File coreTerminologyInputDir = new File(coreInputDir, "/Terminology/");
      for (File f : coreTerminologyInputDir.listFiles()) {
        if (f.getName().contains("sct2_Concept_")) {
          if (coreConceptInputFile != null)
            throw new MojoFailureException("Multiple Concept Files!");
          coreConceptInputFile = f;
        }
      }
      if (coreConceptInputFile != null) {
        int index = coreConceptInputFile.getName().indexOf(".txt");
        version = coreConceptInputFile.getName().substring(index - 8, index);
        getLog().info("Version " + version);
      } else {
        throw new MojoFailureException(
            "Could not find concept file to determine version");
      }

      // output relevant properties/settings to console
      getLog().info("Default preferred name settings:");
      getLog().info(" typeId:          " + dpnTypeId);
      getLog().info(" refSetId:        " + dpnRefSetId);
      getLog().info(" acceptabilityId: " + dpnAcceptabilityId);
      getLog().info(
          "Commit settings: Objects committed in blocks of "
              + Integer.toString(commitCt));

      // create Entitymanager
      factory =
          Persistence.createEntityManagerFactory("MappingServiceDS");

      Runtime runtime = Runtime.getRuntime();
      getLog().info("MEMORY USAGE:");
      getLog().info(" Total: " + runtime.totalMemory());
      getLog().info(" Free:  " + runtime.freeMemory());
      getLog().info(" Max:   " + runtime.maxMemory());

      SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss a"); // format for
      // logging
      try {

      // Prepare sorted input files
      File sortedFileDir = new File(coreInputDir, "/RF2-sorted-temp/");

      getLog().info("Preparing input files...");
      long startTime = System.nanoTime();
      sortRf2Files(coreInputDir, sortedFileDir);
      getLog()
          .info(
              "    File preparation complete in " + getElapsedTime(startTime)
                  + "s");

      openSortedFileReaders(sortedFileDir);

        // load Concepts
        if (conceptsByConcept != null) {
          getLog().info("    Loading Concepts...");
          startTime = System.nanoTime();
          loadConcepts();
          getLog().info(
              "      " + Integer.toString(objectCt) + " Concepts loaded in "
                  + getElapsedTime(startTime) + "s" + " (Ended at "
                  + ft.format(new Date()) + ")");
        }

        // load Descriptions and Language Ref Set Members
        if (descriptionsByDescription != null
            && languageRefsetsByDescription != null) {
          getLog().info("    Loading Descriptions and LanguageRefSets...");
          startTime = System.nanoTime();
          loadDescriptionsAndLanguageRefSets();
          getLog().info(
              "      "
                  + " Descriptions and language ref set members loaded in "
                  + getElapsedTime(startTime) + "s" + " (Ended at "
                  + ft.format(new Date()) + ")");

          // set default preferred names
          getLog().info(" Setting default preferred names for all concepts...");
          startTime = System.nanoTime();
          setDefaultPreferredNames();
          getLog().info(
              "      " + "Names set in " + getElapsedTime(startTime).toString()
                  + "s");

        }

        // load Relationships
        if (relationshipsBySourceConcept != null) {
          getLog().info("    Loading Relationships...");
          startTime = System.nanoTime();
          loadRelationships();
          getLog().info(
              "      " + Integer.toString(objectCt) + " Concepts loaded in "
                  + getElapsedTime(startTime) + "s" + " (Ended at "
                  + ft.format(new Date()) + ")");
        }

        // load Simple RefSets (Content)
        if (simpleRefsetsByConcept != null) {
          getLog().info("    Loading Simple RefSets...");
          startTime = System.nanoTime();
          loadSimpleRefSets();
          getLog().info(
              "      " + Integer.toString(objectCt)
                  + " Simple Refsets loaded in " + getElapsedTime(startTime)
                  + "s" + " (Ended at " + ft.format(new Date()) + ")");
        }

        // load SimpleMapRefSets
        if (simpleMapRefsetsByConcept != null) {
          getLog().info("    Loading SimpleMap RefSets...");
          startTime = System.nanoTime();
          loadSimpleMapRefSets();
          getLog().info(
              "      " + Integer.toString(objectCt)
                  + " SimpleMap RefSets loaded in " + getElapsedTime(startTime)
                  + "s" + " (Ended at " + ft.format(new Date()) + ")");
        }

        // load ComplexMapRefSets
        if (complexMapRefsetsByConcept != null) {
          getLog().info("    Loading ComplexMap RefSets...");
          startTime = System.nanoTime();
          loadComplexMapRefSets();
          getLog().info(
              "      " + Integer.toString(objectCt)
                  + " ComplexMap RefSets loaded in "
                  + getElapsedTime(startTime) + "s" + " (Ended at "
                  + ft.format(new Date()) + ")");
        }

        // load ExtendedMapRefSets
        if (extendedMapRefsetsByConcept != null) {
          getLog().info("    Loading ExtendedMap RefSets...");
          startTime = System.nanoTime();
          loadExtendedMapRefSets();
          getLog().info(
              "      " + Integer.toString(objectCt)
                  + " ExtendedMap RefSets loaded in "
                  + getElapsedTime(startTime) + "s" + " (Ended at "
                  + ft.format(new Date()) + ")");
        }

        // load AttributeValue RefSets (Content)
        if (attributeRefsetsByDescription != null) {
          getLog().info("    Loading AttributeValue RefSets...");
          startTime = System.nanoTime();
          loadAttributeValueRefSets();
          getLog().info(
              "      " + Integer.toString(objectCt)
                  + " AttributeValue RefSets loaded in "
                  + getElapsedTime(startTime).toString() + "s" + " (Ended at "
                  + ft.format(new Date()) + ")");
        }

        conceptCache.clear();
        closeAllSortedFiles();
        
        // creating tree positions
        // first get isaRelType from metadata
        MetadataService metadataService = new MetadataServiceJpa();
        Map<String, String> hierRelTypeMap =
            metadataService.getHierarchicalRelationshipTypes(terminology,
                version);
        String isaRelType =
            hierRelTypeMap.keySet().iterator().next().toString();
        metadataService.close();

        ContentService contentService = new ContentServiceJpa();
        getLog().info("Start creating tree positions.");

        // Walk up tree to the root 
        // ASSUMPTION: single root
        String conceptId = isaRelType;
        String rootId = null;
        OUTER:
        while (true) {
          getLog().info("  Walk up tree from " + conceptId);
          Concept c = contentService.getConcept(conceptId, terminology, version);
          for (Relationship r : c.getRelationships()) {
            if (r.isActive() && r.getTypeId().equals(Long.valueOf(isaRelType))) {
              conceptId = r.getDestinationConcept().getTerminologyId();
              continue OUTER;
            }              
          }
          rootId = conceptId;
          break;
        }
        getLog().info("  Compute tree from rootId " + conceptId);
        contentService.computeTreePositions(terminology, version, isaRelType,
            rootId);

        contentService.close();

        // Final logging messages
        getLog().info(
            "    Total elapsed time for run: "
                + getTotalElapsedTimeStr(startTimeOrig));
        getLog().info("done ...");

      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }

      // Clean-up
      factory.close();

    } catch (Throwable e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    } finally {
      try {
        propertiesInputStream.close();
      } catch (IOException e) {
        // do nothing
      }
    }
  }

  /**
   * Opens sorted data fiels.
   * @param outputDir
   */
  private void openSortedFileReaders(File outputDir) throws IOException {
    File conceptsByConceptsFile =
        new File(outputDir, "concepts_by_concept.sort");
    File descriptionsByDescriptionFile =
        new File(outputDir, "descriptions_by_description.sort");
    // File descriptions_core_by_description_file =
    // new File(outputDir, "descriptions_core_by_description.sort");
    // File descriptions_text_by_description_file =
    // new File(outputDir, "descriptions_text_by_description.sort");
    File relationshipsBySourceConceptFile =
        new File(outputDir, "relationship_by_source_concept.sort");
    // File relationships_by_dest_concept_file =
    // new File(outputDir, "relationship_by_dest_concept.sort");
    File languageRefsetsByDescriptionsFile =
        new File(outputDir, "language_refsets_by_description.sort");
    File attributeRefsetsByConceptFile =
        new File(outputDir, "attribute_refsets_by_concept.sort");
    File simpleRefsetsByConceptFile =
        new File(outputDir, "simple_refsets_by_concept.sort");
    File simpleMapRefsetsByConceptFile =
        new File(outputDir, "simple_map_refsets_by_concept.sort");
    File complexMapRefsetsByConceptFile =
        new File(outputDir, "complex_map_refsets_by_concept.sort");
    File extendedMapRefsetsByConceptsFile =
        new File(outputDir, "extended_map_refsets_by_concept.sort");
    // Concepts
    conceptsByConcept =
        new BufferedReader(new FileReader(conceptsByConceptsFile));

    // Relationships by source concept
    relationshipsBySourceConcept =
        new BufferedReader(new FileReader(relationshipsBySourceConceptFile));

    // Descriptions by description id
    descriptionsByDescription =
        new BufferedReader(new FileReader(descriptionsByDescriptionFile));

    // Language RefSets by description id
    languageRefsetsByDescription =
        new BufferedReader(new FileReader(languageRefsetsByDescriptionsFile));

    // ******************************************************* //
    // Component RefSet Members //
    // ******************************************************* //

    // Attribute Value
    attributeRefsetsByDescription =
        new BufferedReader(new FileReader(attributeRefsetsByConceptFile));

    // Simple
    simpleRefsetsByConcept =
        new BufferedReader(new FileReader(simpleRefsetsByConceptFile));

    // Simple Map
    simpleMapRefsetsByConcept =
        new BufferedReader(new FileReader(simpleMapRefsetsByConceptFile));

    // Complex map
    complexMapRefsetsByConcept =
        new BufferedReader(new FileReader(complexMapRefsetsByConceptFile));

    // Extended map
    extendedMapRefsetsByConcept =
        new BufferedReader(new FileReader(extendedMapRefsetsByConceptsFile));

  }

  // Used for debugging/efficiency monitoring
  /**
   * Returns the elapsed time.
   * 
   * @return the elapsed time
   */
  @SuppressWarnings("boxing")
  private static Long getElapsedTime(long time) {
    return (System.nanoTime() - time) / 1000000000;
  }

  /**
   * Returns the total elapsed time str.
   * 
   * @return the total elapsed time str
   */
  @SuppressWarnings("boxing")
  private static String getTotalElapsedTimeStr(long time) {

    Long resultnum = (System.nanoTime() - time) / 1000000000;
    String result = resultnum.toString() + "s";
    resultnum = resultnum / 60;
    result = result + " / " + resultnum.toString() + "m";
    resultnum = resultnum / 60;
    result = result + " / " + resultnum.toString() + "h";
    return result;

  }

  /**
   * Returns the last modified.
   * 
   * @param directory the directory
   * @return the last modified
   */
  private long getLastModified(File directory) {
    File[] files = directory.listFiles();
    long lastModified = 0;

    for (int j = 0; j < files.length; j++) {
      if (files[j].isDirectory()) {
        long tempLastModified = getLastModified(files[j]);
        if (lastModified < tempLastModified) {
          lastModified = tempLastModified;
        }
      } else if (lastModified < files[j].lastModified()) {
        lastModified = files[j].lastModified();
      }
    }

    return lastModified;
  }

  /**
   * Sorts all files by concept or referencedComponentId.
   * 
   * @throws Exception the exception
   */
  private void sortRf2Files(File coreInputDir, File outputDir) throws Exception {

    // Check expectations and pre-conditions
    if (!outputDir.exists()
        || getLastModified(outputDir) < getLastModified(coreInputDir)) {

      // log reason for sort
      if (!outputDir.exists()) {
        getLog().info("     No sorted files exist -- sorting RF2 files");
      } else if (getLastModified(outputDir) < getLastModified(coreInputDir)) {
        getLog().info(
            "     Sorted files older than input files -- sorting RF2 files");
      }

      // delete any existing temporary files
      FileSorter.deleteSortedFiles(outputDir);

      // test whether file/folder still exists (i.e. delete error)
      if (outputDir.exists()) {
        throw new MojoFailureException(
            "Could not delete existing sorted files folder "
                + outputDir.toString());
      }

      // attempt to make sorted files directory
      if (outputDir.mkdir()) {
        getLog().info(
            " Creating new sorted files folder " + outputDir.toString());
      } else {
        throw new MojoFailureException(
            " Could not create temporary sorted file folder "
                + outputDir.toString());
      }

    } else {
      getLog().info(
          "    Sorted files exist and are up to date.  No sorting required");
      return;
    }

    //
    // Set files
    //
    File coreRelInputFile = null;
    File coreStatedRelInputFile = null;
    File coreConceptInputFile = null;
    File coreDescriptionInputFile = null;
    File coreSimpleRefsetInputFile = null;
    File coreAssociationReferenceInputFile = null;
    File coreAttributeValueInputFile = null;
    File coreComplexMapInputFile = null;
    File coreExtendedMapInputFile = null;
    File coreSimpleMapInputFile = null;
    File coreLanguageInputFile = null;
    File coreIdentifierInputFile = null;
    File coreTextDefinitionInputFile = null;

    // CORE
    File coreTerminologyInputDir = new File(coreInputDir, "/Terminology/");
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

    File coreRefsetInputDir = new File(coreInputDir, "/Refset/");
    File coreContentInputDir = new File(coreRefsetInputDir, "/Content/");
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
      if (f.getName().contains("AssociationReference")) {
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
      if (f.getName().contains("AttributeValue")) {
        if (coreAttributeValueInputFile != null)
          throw new MojoFailureException("Multiple Attribute Value Files!");
        coreAttributeValueInputFile = f;
      }
    }
    getLog().info(
        "  Core Attribute Value Input File = "
            + coreAttributeValueInputFile.toString() + " "
            + coreAttributeValueInputFile.exists());

    File coreCrossmapInputDir = new File(coreRefsetInputDir, "/Map/");
    getLog().info(
        "  Core Crossmap Input Dir = " + coreCrossmapInputDir.toString() + " "
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

    File coreLanguageInputDir = new File(coreRefsetInputDir, "/Language/");
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

    File coreMetadataInputDir = new File(coreRefsetInputDir, "/Metadata/");
    getLog().info(
        "  Core Metadata Input Dir = " + coreMetadataInputDir.toString() + " "
            + coreMetadataInputDir.exists());

    //
    // Initialize files
    //

    File conceptsByConceptFile =
        new File(outputDir, "concepts_by_concept.sort");
    File descriptionsByDescriptionFile =
        new File(outputDir, "descriptions_by_description.sort");
    File descriptionsCoreByDescriptionFile =
        new File(outputDir, "descriptions_core_by_description.sort");
    File descriptionsTextByDescriptionFile =
        new File(outputDir, "descriptions_text_by_description.sort");
    File relationshipsBySourceConceptFile =
        new File(outputDir, "relationship_by_source_concept.sort");
    File relationshipsByDestinationConceptFile =
        new File(outputDir, "relationship_by_dest_concept.sort");
    File languageRefsetsByDescriptionFile =
        new File(outputDir, "language_refsets_by_description.sort");
    File attributeRefsetsByConceptFile =
        new File(outputDir, "attribute_refsets_by_concept.sort");
    File simpleRefsetsByConceptFile =
        new File(outputDir, "simple_refsets_by_concept.sort");
    File simpleMapRefsetsByConceptFile =
        new File(outputDir, "simple_map_refsets_by_concept.sort");
    File comlpexMapRefsetsByConceptFile =
        new File(outputDir, "complex_map_refsets_by_concept.sort");
    File extendedMapRefsetsByConceptsFile =
        new File(outputDir, "extended_map_refsets_by_concept.sort");

    // ******************************************************* //
    // Log file
    // ******************************************************* //

    // ****************//
    // Components //
    // ****************//

    sortRf2File(coreConceptInputFile, conceptsByConceptFile, 0);

    // core descriptions by description
    sortRf2File(coreDescriptionInputFile, descriptionsCoreByDescriptionFile, 0);

    // if text descriptions file exists, sort and merge
    if (coreTextDefinitionInputFile != null) {

      // sort the text definition file
      sortRf2File(coreTextDefinitionInputFile,
          descriptionsTextByDescriptionFile, 0);

      // merge the two description files
      getLog().info("        Merging description files...");
      File mergedDesc =
          mergeSortedFiles(descriptionsTextByDescriptionFile,
              descriptionsCoreByDescriptionFile, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                  String v1[] = s1.split("\t");
                  String v2[] = s2.split("\t");
                  return v1[0].compareTo(v2[0]);
                }
              }, outputDir, ""); // header line

      // rename the temporary file
      Files.move(mergedDesc, descriptionsByDescriptionFile);

    } else {
      // copy the core descriptions file
      Files.copy(descriptionsCoreByDescriptionFile,
          descriptionsByDescriptionFile);
    }

    sortRf2File(coreRelInputFile, relationshipsBySourceConceptFile, 4);
    sortRf2File(coreRelInputFile, relationshipsByDestinationConceptFile, 5);

    // ****************//
    // RefSets //
    // ****************//
    sortRf2File(coreAttributeValueInputFile, attributeRefsetsByConceptFile, 5);
    sortRf2File(coreSimpleRefsetInputFile, simpleRefsetsByConceptFile, 5);
    sortRf2File(coreSimpleMapInputFile, simpleMapRefsetsByConceptFile, 5);
    sortRf2File(coreComplexMapInputFile, comlpexMapRefsetsByConceptFile, 5);
    sortRf2File(coreExtendedMapInputFile, extendedMapRefsetsByConceptsFile, 5);
    sortRf2File(coreLanguageInputFile, languageRefsetsByDescriptionFile, 5);

    // Concepts
    conceptsByConcept =
        new BufferedReader(new FileReader(conceptsByConceptFile));

    // Relationships by source concept
    relationshipsBySourceConcept =
        new BufferedReader(new FileReader(relationshipsBySourceConceptFile));

    // Descriptions by description id
    descriptionsByDescription =
        new BufferedReader(new FileReader(descriptionsByDescriptionFile));

    // Language RefSets by description id
    languageRefsetsByDescription =
        new BufferedReader(new FileReader(languageRefsetsByDescriptionFile));

    // ******************************************************* //
    // Component RefSet Members //
    // ******************************************************* //

    // Attribute Value
    attributeRefsetsByDescription =
        new BufferedReader(new FileReader(attributeRefsetsByConceptFile));

    // Simple
    simpleRefsetsByConcept =
        new BufferedReader(new FileReader(simpleRefsetsByConceptFile));

    // Simple Map
    simpleMapRefsetsByConcept =
        new BufferedReader(new FileReader(simpleMapRefsetsByConceptFile));

    // Complex map
    complexMapRefsetsByConcept =
        new BufferedReader(new FileReader(comlpexMapRefsetsByConceptFile));

    // Extended map
    extendedMapRefsetsByConcept =
        new BufferedReader(new FileReader(extendedMapRefsetsByConceptsFile));

  }

  /**
   * Helper function for sorting an individual file with colum comparator.
   * 
   * @param fileIn the input file to be sorted
   * @param fileOut the resulting sorted file
   * @param sortColumn the column ([0, 1, ...] to compare by
   * @throws Exception the exception
   */
  private void sortRf2File(File fileIn, File fileOut, final int sortColumn)
    throws Exception {

    Comparator<String> comp;

    comp = new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        String v1[] = s1.split("\t");
        String v2[] = s2.split("\t");
        return v1[sortColumn].compareTo(v2[sortColumn]);
      }
    };

    getLog().info(
        " Sorting " + fileIn.toString() + "  into " + fileOut.toString()
            + " by column " + Integer.toString(sortColumn));
    FileSorter.sortFile(fileIn.toString(), fileOut.toString(), comp);

  }

  // /////////////////////////////
  // / OLDER SORT FUNCTIONS
  // /////////////////////////////

  /**
   * Merge-sort two files.
   * 
   * @param files1 the first set of files
   * @param files2 the second set of files
   * @param comp the comparator
   * @param dir the sort dir
   * @param headerLine the header_line
   * @return the sorted {@link File}
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private File mergeSortedFiles(File files1, File files2,
    Comparator<String> comp, File dir, String headerLine) throws IOException {

    final BufferedReader in1 = new BufferedReader(new FileReader(files1));
    final BufferedReader in2 = new BufferedReader(new FileReader(files2));
    final File outFile = File.createTempFile("t+~", ".tmp", dir);
    final BufferedWriter out = new BufferedWriter(new FileWriter(outFile));

    getLog().info(
        "Merging files: " + files1.getName() + " - " + files2.getName()
            + " into " + outFile.getName());

    String line1 = in1.readLine();
    String line2 = in2.readLine();
    String line = null;

    if (!headerLine.isEmpty()) {
      line = headerLine;
      out.write(line);
      out.newLine();
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

      // if a header line, do not write
      if (!line.startsWith("id")) {

        out.write(line);
        out.newLine();
      }
    }

    out.flush();
    out.close();
    in1.close();
    in2.close();

    return outFile;

  }

  /**
   * Returns the concept.
   * 
   * @param terminologyId the terminology id
   * @param terminology the terminology
   * @param terminologyVersion the terminology version
   * @return the concept
   * @throws Exception the exception
   */
  private Concept getConcept(String terminologyId, String terminology,
    String terminologyVersion, EntityManager manager) throws Exception {

    if (conceptCache.containsKey(terminologyId + terminology
        + terminologyVersion)) {

      // uses hibernate first-level cache
      return conceptCache.get(terminologyId + terminology + terminologyVersion);
    }

    Query query =
        manager
            .createQuery("select c from ConceptJpa c where terminologyId = :terminologyId and terminologyVersion = :terminologyVersion and terminology = :terminology");

    // Try to retrieve the single expected result
    // If zero or more than one result are returned, log error and set
    // result to null

    try {
      query.setParameter("terminologyId", terminologyId);
      query.setParameter("terminology", terminology);
      query.setParameter("terminologyVersion", terminologyVersion);

      Concept c = (Concept) query.getSingleResult();

      conceptCache.put(terminologyId + terminology + terminologyVersion, c);

      return c;

    } catch (NoResultException e) {
      // Log and return null if there are no releases
      getLog().debug(
          "Concept query for terminologyId = " + terminologyId
              + ", terminology = " + terminology + ", terminologyVersion = "
              + terminologyVersion + " returned no results!");
      return null;
    }

  }

  /**
   * Closes all sorted temporary files.
   * 
   * @throws Exception if something goes wrong
   */
  private void closeAllSortedFiles() throws Exception {
    if (conceptsByConcept != null) {
      conceptsByConcept.close();
    }
    if (descriptionsByDescription != null) {
      descriptionsByDescription.close();
    }
    if (relationshipsBySourceConcept != null) {
      relationshipsBySourceConcept.close();
    }
    if (languageRefsetsByDescription != null) {
      languageRefsetsByDescription.close();
    }
    if (attributeRefsetsByDescription != null) {
      attributeRefsetsByDescription.close();
    }
    if (simpleRefsetsByConcept != null) {
      simpleRefsetsByConcept.close();
    }
    if (simpleMapRefsetsByConcept != null) {
      simpleMapRefsetsByConcept.close();
    }
    if (complexMapRefsetsByConcept != null) {
      complexMapRefsetsByConcept.close();
    }
    if (extendedMapRefsetsByConcept != null) {
      extendedMapRefsetsByConcept.close();
    }
  }

  /**
   * Load concepts.
   * 
   * @throws Exception the exception
   */
  private void loadConcepts() throws Exception {

    String line = "";
    objectCt = 0;

    // begin transaction
    EntityManager manager = factory.createEntityManager();
    EntityTransaction tx = manager.getTransaction();
    tx.begin();

    while ((line = conceptsByConcept.readLine()) != null) {

      String fields[] = line.split("\t");
      Concept concept = new ConceptJpa();

      if (!fields[0].equals("id")) { // header

        concept.setTerminologyId(fields[0]);
        concept.setEffectiveTime(dt.parse(fields[1]));
        concept.setActive(fields[2].equals("1") ? true : false);
        concept.setModuleId(Long.valueOf(fields[3]));
        concept.setDefinitionStatusId(Long.valueOf(fields[4]));
        concept.setTerminology(terminology);
        concept.setTerminologyVersion(version);
        concept.setDefaultPreferredName("null");

        getLog().debug(
            "  Add concept " + concept.getTerminologyId() + " "
                + concept.getDefaultPreferredName());
        manager.persist(concept);

        conceptCache.put(new String(fields[0] + concept.getTerminology()
            + concept.getTerminologyVersion()), concept);

        // regularly commit at intervals
        if (++objectCt % commitCt == 0) {

          tx.commit();
          manager.clear();
          tx.begin();
        }
      }
    }

    // commit any remaining objects
    tx.commit();
    manager.clear();
    manager.close();
    defaultPreferredNames.clear();

    // print memory information
    Runtime runtime = Runtime.getRuntime();
    getLog().info("MEMORY USAGE:");
    getLog().info(" Total: " + runtime.totalMemory());
    getLog().info(" Free:  " + runtime.freeMemory());
    getLog().info(" Max:   " + runtime.maxMemory());


  }

  /**
   * Load relationships.
   * 
   * @throws Exception the exception
   */
  private void loadRelationships() throws Exception {

    String line = "";
    objectCt = 0;

    // begin transaction
    EntityManager manager = factory.createEntityManager();
    EntityTransaction tx = manager.getTransaction();
    tx.begin();

    while ((line = relationshipsBySourceConcept.readLine()) != null) {

      String fields[] = line.split("\t");
      Relationship relationship = new RelationshipJpa();

      if (!fields[0].equals("id")) { // header
        relationship.setTerminologyId(fields[0]);
        relationship.setEffectiveTime(dt.parse(fields[1]));
        relationship.setActive(fields[2].equals("1") ? true : false); // active
        relationship.setModuleId(Long.valueOf(fields[3])); // moduleId

        relationship.setRelationshipGroup(Integer.valueOf(fields[6])); // relationshipGroup
        relationship.setTypeId(Long.valueOf(fields[7])); // typeId
        relationship.setCharacteristicTypeId(Long.valueOf(fields[8])); // characteristicTypeId
        relationship.setTerminology(terminology);
        relationship.setTerminologyVersion(version);
        relationship.setModifierId(Long.valueOf(fields[9]));

        Concept sourceConcept =
            getConcept(fields[4], relationship.getTerminology(),
                relationship.getTerminologyVersion(), manager);
        Concept destinationConcept =
            getConcept(fields[5], relationship.getTerminology(),
                relationship.getTerminologyVersion(), manager);

        if (sourceConcept != null && destinationConcept != null) {
          relationship.setSourceConcept(sourceConcept);
          relationship.setDestinationConcept(destinationConcept);

          manager.persist(relationship);

          // regularly commit at intervals
          if (++objectCt % commitCt == 0) {
            tx.commit();
            manager.clear();
            tx.begin();
          }
        } else {
          if (sourceConcept == null) {
            getLog().info(
                "Relationship " + relationship.getTerminologyId()
                    + " references non-existent source concept " + fields[4]);
          }
          if (destinationConcept == null) {
            getLog().info(
                "Relationship " + relationship.getTerminologyId()
                    + " references non-existent destination concept "
                    + fields[5]);
          }

        }
      }
    }

    // commit any remaining objects
    tx.commit();
    manager.clear();
    manager.close();
    
    // print memory information
    Runtime runtime = Runtime.getRuntime();
    getLog().info("MEMORY USAGE:");
    getLog().info(" Total: " + runtime.totalMemory());
    getLog().info(" Free:  " + runtime.freeMemory());
    getLog().info(" Max:   " + runtime.maxMemory());
  }

  /**
   * Load descriptions.
   * 
   * @throws Exception the exception
   */
  private void loadDescriptionsAndLanguageRefSets() throws Exception {

    Concept concept;
    Description description;
    LanguageRefSetMember language;
    int descCt = 0; // counter for descriptions
    int langCt = 0; // counter for language ref set members
    int skipCt = 0; // counter for number of language ref set members skipped

    // begin transaction
    EntityManager manager = factory.createEntityManager();
    EntityTransaction tx = manager.getTransaction();
    tx.begin();

    // load and persist first description
    description = getNextDescription(manager);

    // load first language ref set member
    language = getNextLanguage();

    // cycle over descriptions
    while (!description.getTerminologyId().equals("-1")) { // getNextDescription
                                                           // sets this to -1
                                                           // if null line

      // if current language ref set references a lexicographically "lower"
      // String terminologyId, SKIP: description is not in data set
      while (language.getDescription().getTerminologyId()
          .compareTo(description.getTerminologyId()) < 0
          && !language.getTerminologyId().equals("-1")) {

        getLog().info(
            "     " + "Language Ref Set " + language.getTerminologyId()
                + " references non-existent description "
                + language.getDescription().getTerminologyId());
        language = getNextLanguage();
        skipCt++;
      }

      // cycle over language ref sets until new description id found or end of
      // language ref sets found
      while (language.getDescription().getTerminologyId()
          .equals(description.getTerminologyId())
          && !language.getTerminologyId().equals("-1")) {

        // set the description
        language.setDescription(description);
        description.addLanguageRefSetMember(language);
        langCt++;

        // check if this language refset and description form the
        // defaultPreferredName
        if (description.isActive() && description.getTypeId().equals(dpnTypeId)
            && new Long(language.getRefSetId()).equals(dpnRefSetId)
            && language.isActive()
            && language.getAcceptabilityId().equals(dpnAcceptabilityId)) {

          // retrieve the concept for this description
          concept = description.getConcept();
          if (defaultPreferredNames.get(concept.getId()) != null) {
            getLog().info(
                "Multiple default preferred names for concept "
                    + concept.getTerminologyId());
            getLog().info(
                "  " + "Existing: "
                    + defaultPreferredNames.get(concept.getId()));
            getLog().info("  " + "Replaced: " + description.getTerm());
          }
          defaultPreferredNames.put(concept.getId(), description.getTerm());

        }

        // / get the next language ref set member
        language = getNextLanguage();
      }

      // persist the description
      manager.persist(description);

      // get the next description
      description = getNextDescription(manager);

      // increment description count
      descCt++;

      if (descCt % 100000 == 0) {
        getLog().info("-> descriptions: " + Integer.toString(descCt));
      }

      // regularly commit at intervals
      if (descCt % commitCt == 0) {
        tx.commit();
        manager.clear();
        tx.begin();
      }

    }

    // commit any remaining objects
    tx.commit();
    manager.clear();
    manager.close();
    
    getLog().info("      " + descCt + " descriptions loaded");
    getLog().info("      " + langCt + " language ref sets loaded");
    getLog().info(
        "      " + skipCt + " language ref sets skipped (no description)");

    // print memory information
    Runtime runtime = Runtime.getRuntime();
    getLog().info("MEMORY USAGE:");
    getLog().info(" Total: " + runtime.totalMemory());
    getLog().info(" Free:  " + runtime.freeMemory());
    getLog().info(" Max:   " + runtime.maxMemory());
  }

  /**
   * Sets the default preferred names.
   * 
   * @throws Exception the exception
   */
  private void setDefaultPreferredNames() throws Exception {

    // Begin transaction
    EntityManager manager = factory.createEntityManager();
    EntityTransaction tx = manager.getTransaction();
    tx.begin();

    Iterator<Concept> conceptIterator = conceptCache.values().iterator();
    objectCt = 0;
    int ct = 0;
    while (conceptIterator.hasNext()) {
      Concept cachedConcept = conceptIterator.next();

      Concept dbConcept = manager.find(ConceptJpa.class, cachedConcept.getId());
      dbConcept.getDescriptions();
      dbConcept.getRelationships();
      if (defaultPreferredNames.get(dbConcept.getId()) != null) {
        dbConcept.setDefaultPreferredName(defaultPreferredNames.get(dbConcept
            .getId()));
      } else {
        dbConcept.setDefaultPreferredName("No default preferred name found");
      }

      manager.merge(dbConcept);

      if (++ct % 50000 == 0) {
        getLog().info(Integer.toString(ct));
      }

      
      if (++objectCt % commitCt == 0) {
        tx.commit();
        manager.clear();
        tx.begin();
      }
    }

    tx.commit();
    manager.clear();
    manager.close();
    
    // print memory information
    Runtime runtime = Runtime.getRuntime();
    getLog().info("MEMORY USAGE:");
    getLog().info(" Total: " + runtime.totalMemory());
    getLog().info(" Free:  " + runtime.freeMemory());
    getLog().info(" Max:   " + runtime.maxMemory());
    
  }

  /**
   * Returns the next description.
   * 
   * @return the next description
   * @throws Exception the exception
   */
  private Description getNextDescription(EntityManager manager) throws Exception {

    String line, fields[];
    Description description = new DescriptionJpa();
    description.setTerminologyId("-1");

    if ((line = descriptionsByDescription.readLine()) != null) {

      line = line.replace("\r", "");
      fields = line.split("\t");

      if (!fields[0].equals("id")) { // header

        description.setTerminologyId(fields[0]);
        description.setEffectiveTime(dt.parse(fields[1]));
        description.setActive(fields[2].equals("1") ? true : false);
        description.setModuleId(Long.valueOf(fields[3]));

        description.setLanguageCode(fields[5]);
        description.setTypeId(Long.valueOf(fields[6]));
        description.setTerm(fields[7]);
        description.setCaseSignificanceId(Long.valueOf(fields[8]));
        description.setTerminology(terminology);
        description.setTerminologyVersion(version);

        // set concept from cache
        Concept concept =
            getConcept(fields[4], description.getTerminology(),
                description.getTerminologyVersion(), manager);

        if (concept != null) {
          description.setConcept(concept);
        } else {
          getLog().info(
              "Description " + description.getTerminologyId()
                  + " references non-existent concept " + fields[4]);
        }
        // otherwise get next line
      } else {
        description = getNextDescription(manager);
      }
    }

    return description;
  }

  /**
   * Utility function to return the next line of language ref set files in
   * object form.
   * 
   * @return a partial language ref set member (lacks full description)
   * @throws Exception the exception
   */
  private LanguageRefSetMember getNextLanguage() throws Exception {

    String line, fields[];
    LanguageRefSetMember languageRefSetMember = new LanguageRefSetMemberJpa();
    languageRefSetMember.setTerminologyId("-1");

    // if non-null
    if ((line = languageRefsetsByDescription.readLine()) != null) {

      line = line.replace("\r", "");

      fields = line.split("\t");

      if (!fields[0].equals("id")) { // header line

        // Universal RefSet attributes
        languageRefSetMember.setTerminologyId(fields[0]);
        languageRefSetMember.setEffectiveTime(dt.parse(fields[1]));
        languageRefSetMember.setActive(fields[2].equals("1") ? true : false);
        languageRefSetMember.setModuleId(Long.valueOf(fields[3]));
        languageRefSetMember.setRefSetId(fields[4]);

        // Language unique attributes
        languageRefSetMember.setAcceptabilityId(Long.valueOf(fields[6]));

        // Terminology attributes
        languageRefSetMember.setTerminology(terminology);
        languageRefSetMember.setTerminologyVersion(version);

        // Set a dummy description with terminology id only
        Description description = new DescriptionJpa();
        description.setTerminologyId(fields[5]);
        languageRefSetMember.setDescription(description);

        // if header line, get next record
      } else {
        languageRefSetMember = getNextLanguage();
      }

      // if null, set a dummy description value to avoid null-pointer exceptions
      // in main loop
    } else {
      Description description = new DescriptionJpa();
      description.setTerminologyId("-1");
      languageRefSetMember.setDescription(description);
    }

    return languageRefSetMember;
  }

  /**
   * Load AttributeRefSets (Content).
   * 
   * @throws Exception the exception
   */

  @SuppressWarnings("boxing")
  private void loadAttributeValueRefSets() throws Exception {

    String line = "";
    objectCt = 0;

    // begin transaction
    EntityManager manager = factory.createEntityManager();
    EntityTransaction tx = manager.getTransaction();
    tx.begin();

    while ((line = attributeRefsetsByDescription.readLine()) != null) {

      line = line.replace("\r", "");
      String fields[] = line.split("\t");
      AttributeValueRefSetMember attributeValueRefSetMember =
          new AttributeValueRefSetMemberJpa();

      if (!fields[0].equals("id")) { // header

        // Universal RefSet attributes
        attributeValueRefSetMember.setTerminologyId(fields[0]);
        attributeValueRefSetMember.setEffectiveTime(dt.parse(fields[1]));
        attributeValueRefSetMember.setActive(fields[2].equals("1") ? true
            : false);
        attributeValueRefSetMember.setModuleId(Long.valueOf(fields[3]));
        attributeValueRefSetMember.setRefSetId(fields[4]);

        // AttributeValueRefSetMember unique attributes
        attributeValueRefSetMember.setValueId(Long.valueOf(fields[6]));

        // Terminology attributes
        attributeValueRefSetMember.setTerminology(terminology);
        attributeValueRefSetMember.setTerminologyVersion(version);

        // Retrieve concept -- firstToken is referencedComponentId
        Concept concept =
            getConcept(fields[5], attributeValueRefSetMember.getTerminology(),
                attributeValueRefSetMember.getTerminologyVersion(), manager);

        if (concept != null) {

          attributeValueRefSetMember.setConcept(concept);
          manager.persist(attributeValueRefSetMember);
          
          // regularly commit at intervals
          if (++objectCt % commitCt == 0) {
            tx.commit();
            manager.clear();
            tx.begin();
          }
        } else {
          getLog().debug(
              "attributeValueRefSetMember "
                  + attributeValueRefSetMember.getTerminologyId()
                  + " references non-existent concept " + fields[5]);
        }
      }
    }

    // commit any remaining objects
    tx.commit();
    manager.clear();
    manager.close();
    
    // print memory information
    Runtime runtime = Runtime.getRuntime();
    getLog().info("MEMORY USAGE:");
    getLog().info(" Total: " + runtime.totalMemory());
    getLog().info(" Free:  " + runtime.freeMemory());
    getLog().info(" Max:   " + runtime.maxMemory());
  }

  /**
   * Load SimpleRefSets (Content).
   * 
   * @throws Exception the exception
   */

  private void loadSimpleRefSets() throws Exception {

    String line = "";
    objectCt = 0;

    // begin transaction
    EntityManager manager = factory.createEntityManager();
    EntityTransaction tx = manager.getTransaction();
    tx.begin();

    while ((line = simpleRefsetsByConcept.readLine()) != null) {

      line = line.replace("\r", "");
      String fields[] = line.split("\t");
      SimpleRefSetMember simpleRefSetMember = new SimpleRefSetMemberJpa();

      if (!fields[0].equals("id")) { // header

        // Universal RefSet attributes
        simpleRefSetMember.setTerminologyId(fields[0]);
        simpleRefSetMember.setEffectiveTime(dt.parse(fields[1]));
        simpleRefSetMember.setActive(fields[2].equals("1") ? true : false);
        simpleRefSetMember.setModuleId(Long.valueOf(fields[3]));
        simpleRefSetMember.setRefSetId(fields[4]);

        // SimpleRefSetMember unique attributes
        // NONE

        // Terminology attributes
        simpleRefSetMember.setTerminology(terminology);
        simpleRefSetMember.setTerminologyVersion(version);

        // Retrieve Concept -- firstToken is referencedComonentId
        Concept concept =
            getConcept(fields[5], simpleRefSetMember.getTerminology(),
                simpleRefSetMember.getTerminologyVersion(), manager);

        if (concept != null) {
          simpleRefSetMember.setConcept(concept);
          manager.persist(simpleRefSetMember);
          
          // regularly commit at intervals
          if (++objectCt % commitCt == 0) {
            tx.commit();
            manager.clear();
            tx.begin();
          }
        } else {
          getLog().info(
              "simpleRefSetMember " + simpleRefSetMember.getTerminologyId()
                  + " references non-existent concept " + fields[5]);
        }
      }
    }

    // commit any remaining objects
    tx.commit();
    manager.clear();
    manager.close();

    // print memory information
    Runtime runtime = Runtime.getRuntime();
    getLog().info("MEMORY USAGE:");
    getLog().info(" Total: " + runtime.totalMemory());
    getLog().info(" Free:  " + runtime.freeMemory());
    getLog().info(" Max:   " + runtime.maxMemory());
}

  /**
   * Load SimpleMapRefSets (Crossmap).
   * 
   * @throws Exception the exception
   */
  private void loadSimpleMapRefSets() throws Exception {

    String line = "";
    objectCt = 0;

    // begin transaction
    EntityManager manager = factory.createEntityManager();
    EntityTransaction tx = manager.getTransaction();
    tx.begin();

    while ((line = simpleMapRefsetsByConcept.readLine()) != null) {

      line = line.replace("\r", "");
      String fields[] = line.split("\t");
      SimpleMapRefSetMember simpleMapRefSetMember =
          new SimpleMapRefSetMemberJpa();

      if (!fields[0].equals("id")) { // header

        // Universal RefSet attributes
        simpleMapRefSetMember.setTerminologyId(fields[0]);
        simpleMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
        simpleMapRefSetMember.setActive(fields[2].equals("1") ? true : false);
        simpleMapRefSetMember.setModuleId(Long.valueOf(fields[3]));
        simpleMapRefSetMember.setRefSetId(fields[4]);

        // SimpleMap unique attributes
        simpleMapRefSetMember.setMapTarget(fields[6]);

        // Terminology attributes
        simpleMapRefSetMember.setTerminology(terminology);
        simpleMapRefSetMember.setTerminologyVersion(version);

        // Retrieve concept -- firstToken is referencedComponentId
        Concept concept =
            getConcept(fields[5], simpleMapRefSetMember.getTerminology(),
                simpleMapRefSetMember.getTerminologyVersion(), manager);

        if (concept != null) {
          simpleMapRefSetMember.setConcept(concept);
          manager.persist(simpleMapRefSetMember);

          // regularly commit at intervals
          if (++objectCt % commitCt == 0) {
            tx.commit();
            manager.clear();
            tx.begin();
          }
        } else {
          getLog().info(
              "simpleMapRefSetMember "
                  + simpleMapRefSetMember.getTerminologyId()
                  + " references non-existent concept " + fields[5]);
        }
      }
    }

    // commit any remaining objects
    tx.commit();
    manager.clear();
    manager.close();
    // print memory information
    Runtime runtime = Runtime.getRuntime();
    getLog().info("MEMORY USAGE:");
    getLog().info(" Total: " + runtime.totalMemory());
    getLog().info(" Free:  " + runtime.freeMemory());
    getLog().info(" Max:   " + runtime.maxMemory());
  }

  /**
   * Load ComplexMapRefSets (Crossmap).
   * 
   * @throws Exception the exception
   */
  private void loadComplexMapRefSets() throws Exception {

    String line = "";
    objectCt = 0;

    // begin transaction
    EntityManager manager = factory.createEntityManager();
    EntityTransaction tx = manager.getTransaction();
    tx.begin();

    while ((line = complexMapRefsetsByConcept.readLine()) != null) {

      line = line.replace("\r", "");
      String fields[] = line.split("\t");
      ComplexMapRefSetMember complexMapRefSetMember =
          new ComplexMapRefSetMemberJpa();

      if (!fields[0].equals("id")) { // header

        complexMapRefSetMember.setTerminologyId(fields[0]);
        complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
        complexMapRefSetMember.setActive(fields[2].equals("1") ? true : false);
        complexMapRefSetMember.setModuleId(Long.valueOf(fields[3]));
        complexMapRefSetMember.setRefSetId(fields[4]);
        // conceptId

        // ComplexMap unique attributes
        complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6]));
        complexMapRefSetMember.setMapPriority(Integer.parseInt(fields[7]));
        complexMapRefSetMember.setMapRule(fields[8]);
        complexMapRefSetMember.setMapAdvice(fields[9]);
        complexMapRefSetMember.setMapTarget(fields[10]);
        complexMapRefSetMember.setMapRelationId(Long.valueOf(fields[11]));

        // ComplexMap unique attributes NOT set by file (mapBlock
        // elements)
        complexMapRefSetMember.setMapBlock(0); // default value
        complexMapRefSetMember.setMapBlockRule(null); // no default
        complexMapRefSetMember.setMapBlockAdvice(null); // no default

        // Terminology attributes
        complexMapRefSetMember.setTerminology(terminology);
        complexMapRefSetMember.setTerminologyVersion(version);

        // set Concept
        Concept concept =
            getConcept(fields[5], complexMapRefSetMember.getTerminology(),
                complexMapRefSetMember.getTerminologyVersion(), manager);

        if (concept != null) {
          complexMapRefSetMember.setConcept(concept);
          manager.persist(complexMapRefSetMember);
          
          // regularly commit at intervals
          if (++objectCt % commitCt == 0) {
            tx.commit();
            manager.clear();
            tx.begin();
          }
        } else {
          getLog().info(
              "complexMapRefSetMember "
                  + complexMapRefSetMember.getTerminologyId()
                  + " references non-existent concept " + fields[5]);
        }

      }
    }

    // commit any remaining objects
    tx.commit();
    manager.clear();
    manager.close();

    // print memory information
    Runtime runtime = Runtime.getRuntime();
    getLog().info("MEMORY USAGE:");
    getLog().info(" Total: " + runtime.totalMemory());
    getLog().info(" Free:  " + runtime.freeMemory());
    getLog().info(" Max:   " + runtime.maxMemory());
  }

  /**
   * Load ExtendedMapRefSets (Crossmap).
   * 
   * @throws Exception the exception
   */

  // NOTE: ExtendedMap RefSets are loaded into ComplexMapRefSetMember
  // where mapRelationId = mapCategoryId
  private void loadExtendedMapRefSets() throws Exception {

    String line = "";
    objectCt = 0;

    // begin transaction
    EntityManager manager = factory.createEntityManager();
    EntityTransaction tx = manager.getTransaction();
    tx.begin();

    while ((line = extendedMapRefsetsByConcept.readLine()) != null) {

      line = line.replace("\r", "");
      String fields[] = line.split("\t");
      ComplexMapRefSetMember complexMapRefSetMember =
          new ComplexMapRefSetMemberJpa();

      if (!fields[0].equals("id")) { // header

        complexMapRefSetMember.setTerminologyId(fields[0]);
        complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
        complexMapRefSetMember.setActive(fields[2].equals("1") ? true : false);
        complexMapRefSetMember.setModuleId(Long.valueOf(fields[3]));
        complexMapRefSetMember.setRefSetId(fields[4]);
        // conceptId

        // ComplexMap unique attributes
        complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6]));
        complexMapRefSetMember.setMapPriority(Integer.parseInt(fields[7]));
        complexMapRefSetMember.setMapRule(fields[8]);
        complexMapRefSetMember.setMapAdvice(fields[9]);
        complexMapRefSetMember.setMapTarget(fields[10]);
        complexMapRefSetMember.setMapRelationId(Long.valueOf(fields[12]));

        // ComplexMap unique attributes NOT set by file (mapBlock
        // elements)
        complexMapRefSetMember.setMapBlock(1); // default value
        complexMapRefSetMember.setMapBlockRule(null); // no default
        complexMapRefSetMember.setMapBlockAdvice(null); // no default

        // Terminology attributes
        complexMapRefSetMember.setTerminology(terminology);
        complexMapRefSetMember.setTerminologyVersion(version);

        // set Concept
        Concept concept =
            getConcept(fields[5], complexMapRefSetMember.getTerminology(),
                complexMapRefSetMember.getTerminologyVersion(), manager);

        if (concept != null) {
          complexMapRefSetMember.setConcept(concept);
          manager.persist(complexMapRefSetMember);

          // regularly commit at intervals
          if (++objectCt % commitCt == 0) {
            tx.commit();
            manager.clear();
            tx.begin();
          }
        } else {
          getLog().info(
              "complexMapRefSetMember "
                  + complexMapRefSetMember.getTerminologyId()
                  + " references non-existent concept " + fields[5]);
        }

      }
    }

    // commit any remaining objects
    tx.commit();
    manager.clear();
    manager.close();

    // print memory information
    Runtime runtime = Runtime.getRuntime();
    getLog().info("MEMORY USAGE:");
    getLog().info(" Total: " + runtime.totalMemory());
    getLog().info(" Free:  " + runtime.freeMemory());
    getLog().info(" Max:   " + runtime.maxMemory());
  }
}
