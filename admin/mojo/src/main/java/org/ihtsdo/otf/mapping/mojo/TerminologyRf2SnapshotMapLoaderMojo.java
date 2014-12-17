package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.FileSorter;

/**
 * Goal which loads an RF2 Snapshot of SNOMED CT data into a database.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal load-rf2-snapshot-map
 * 
 * @phase package
 */
public class TerminologyRf2SnapshotMapLoaderMojo extends AbstractMojo {

  /**
   * The input directory
   * @parameter
   * @required
   */
  private String inputDir;
  /**
   * Name of terminology to be loaded.
   * @parameter
   * @required
   */
  private String terminology;

  /**
   * Name of terminology to be loaded.
   * @parameter
   * @required
   */
  private String version;

  /** The date format. */
  private final SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

  /** The complex map refsets by concept. */
  private BufferedReader complexMapRefsetsByConcept;

  /** The extended map refsets by concept. */
  private BufferedReader extendedMapRefsetsByConcept;

  /** counter for objects created, reset in each load section. */
  int objectCt; //

  /** the number of objects to create before committing. */
  int commitCt = 1000;

  /**
   * Instantiates a {@link TerminologyRf2SnapshotMapLoaderMojo} from the specified
   * parameters.
   * 
   */
  public TerminologyRf2SnapshotMapLoaderMojo() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting loading RF2 data");
    getLog().info("  inputDir = " + inputDir);
    getLog().info("  terminology = " + terminology);

    try {

      // Track system level information
      long startTimeOrig = System.nanoTime();

      // Set the input directory
      File coreInputDir = new File(inputDir);
      if (!coreInputDir.exists()) {
        throw new MojoFailureException("Specified input dir missing");
      }

      //
      // Determine version
      //

      getLog().info("  terminology = " + terminology);
      getLog().info("  version = " + version);

      // Log memory usage
      Runtime runtime = Runtime.getRuntime();
      getLog().info("MEMORY USAGE:");
      getLog().info(" Total: " + runtime.totalMemory());
      getLog().info(" Free:  " + runtime.freeMemory());
      getLog().info(" Max:   " + runtime.maxMemory());

      SimpleDateFormat ft = new SimpleDateFormat("hh:mm:ss a"); // format for

      try {

        // Prepare sorted input files
        File sortedFileDir = new File(coreInputDir, "/RF2-sorted-temp/");

        getLog().info("  Sorting input files...");
        long startTime = System.nanoTime();
        sortRf2Files(coreInputDir, sortedFileDir);
        getLog().info("      complete in " + getElapsedTime(startTime) + "s");

        // Open readers
        openSortedFileReaders(sortedFileDir);

        // Load ComplexMapRefSets
        if (complexMapRefsetsByConcept != null) {
          getLog().info("  Loading ComplexMap RefSets...");
          startTime = System.nanoTime();
          loadComplexMapRefSets();
          getLog().info(
              "    elapsed time = " + getElapsedTime(startTime) + "s"
                  + " (Ended at " + ft.format(new Date()) + ")");
        }

        // Load ExtendedMapRefSets
        if (extendedMapRefsetsByConcept != null) {
          getLog().info("  Loading ExtendedMap RefSets...");
          startTime = System.nanoTime();
          loadExtendedMapRefSets();
          getLog().info(
              "    elapsed time = " + getElapsedTime(startTime) + "s"
                  + " (Ended at " + ft.format(new Date()) + ")");
        }

        // Close files/readers
        closeAllSortedFiles();

        // Final logging messages
        getLog().info(
            "      elapsed time = " + getTotalElapsedTimeStr(startTimeOrig));
        getLog().info("done ...");

      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }

      // Clean-up
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

  /**
   * Opens sorted data files.
   *
   * @param outputDir the output dir
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void openSortedFileReaders(File outputDir) throws IOException {

    File complexMapRefsetsByConceptFile =
        new File(outputDir, "complex_map_refsets_by_concept.sort");
    File extendedMapRefsetsByConceptsFile =
        new File(outputDir, "extended_map_refsets_by_concept.sort");

    // Complex map reader
    complexMapRefsetsByConcept =
        new BufferedReader(new FileReader(complexMapRefsetsByConceptFile));

    // Extended map reader
    extendedMapRefsetsByConcept =
        new BufferedReader(new FileReader(extendedMapRefsetsByConceptsFile));

  }

  /**
   * Returns the elapsed time.
   *
   * @param time the time
   * @return the elapsed time
   */
  @SuppressWarnings("boxing")
  private static Long getElapsedTime(long time) {
    return (System.nanoTime() - time) / 1000000000;
  }

  /**
   * Returns the total elapsed time str.
   *
   * @param time the time
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
   * @param coreInputDir the core input dir
   * @param outputDir the output dir
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
            "  Creating new sorted files folder " + outputDir.toString());
      } else {
        throw new MojoFailureException(
            "Could not create temporary sorted file folder "
                + outputDir.toString());
      }

    } else {
      getLog().info(
          "    Sorted files exist and are up to date.  No sorting required");
      return;
    }

    //
    // Setup files
    //
    File coreComplexMapInputFile = null;
    File coreExtendedMapInputFile = null;
   

    // Termionlogy dir
    File coreTerminologyInputDir = new File(coreInputDir, "/Terminology/");
    getLog().info(
        "    Terminology dir = " + coreTerminologyInputDir.toString() + " "
            + coreTerminologyInputDir.exists());

    // Refset/Content dir
    File coreRefsetInputDir = new File(coreInputDir, "/Refset/");
    File coreContentInputDir = new File(coreRefsetInputDir, "/Content/");
    getLog().info(
        "    Refset/Content dir = " + coreContentInputDir.toString() + " "
            + coreContentInputDir.exists());

    
    // Refset/Map dir
    File coreCrossmapInputDir = new File(coreRefsetInputDir, "/Map/");
    getLog().info(
        "    Refset/Map dir = " + coreCrossmapInputDir.toString() + " "
            + coreCrossmapInputDir.exists());

    // Complex map file
    for (File f : coreCrossmapInputDir.listFiles()) {
      if (f.getName().contains("ComplexMap")) {
        if (coreComplexMapInputFile != null)
          throw new MojoFailureException("Multiple Complex Map Files!");
        coreComplexMapInputFile = f;
      }
    }
    if (coreComplexMapInputFile != null) {
      getLog().info(
          "        Complex map file = " + coreComplexMapInputFile.toString()
              + " " + coreComplexMapInputFile.exists());
    }

    // Extended map file
    for (File f : coreCrossmapInputDir.listFiles()) {
      if (f.getName().contains("ExtendedMap")) {
        if (coreExtendedMapInputFile != null)
          throw new MojoFailureException("Multiple Extended Map Files!");
        coreExtendedMapInputFile = f;
      }
    }
    if (coreComplexMapInputFile != null) {
      getLog().info(
          "      Extended map file = " + coreComplexMapInputFile.toString()
              + " " + coreComplexMapInputFile.exists());
    }


    File complexMapRefsetsByConceptFile =
        new File(outputDir, "complex_map_refsets_by_concept.sort");
    File extendedMapRefsetsByConceptsFile =
        new File(outputDir, "extended_map_refsets_by_concept.sort");

    getLog().info("      Sort files");

    // Sort complex map file
    sortRf2File(coreComplexMapInputFile, complexMapRefsetsByConceptFile, 5);

    // sort extended map file
    sortRf2File(coreExtendedMapInputFile, extendedMapRefsetsByConceptsFile, 5);

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
    // Comparator to split on \t and sort by sortColumn
    comp = new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        String v1[] = s1.split("\t");
        String v2[] = s2.split("\t");
        return v1[sortColumn].compareTo(v2[sortColumn]);
      }
    };

    getLog().info(
        "        Sorting " + fileIn.toString() + "  into " + fileOut.toString()
            + " by column " + Integer.toString(sortColumn));
    FileSorter.sortFile(fileIn.toString(), fileOut.toString(), comp);

  }

  /**
   * Closes all sorted temporary files.
   * 
   * @throws Exception if something goes wrong
   */
  private void closeAllSortedFiles() throws Exception {
   
    if (complexMapRefsetsByConcept != null) {
      complexMapRefsetsByConcept.close();
    }
    if (extendedMapRefsetsByConcept != null) {
      extendedMapRefsetsByConcept.close();
    }
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
    final ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();

    while ((line = complexMapRefsetsByConcept.readLine()) != null) {

      line = line.replace("\r", "");
      final String fields[] = line.split("\t");

      if (!fields[0].equals("id")) { // header
        final ComplexMapRefSetMember complexMapRefSetMember =
            new ComplexMapRefSetMemberJpa();

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
        final Concept concept = contentService.getConcept(fields[5],terminology,version);

        if (concept != null) {
          complexMapRefSetMember.setConcept(concept);
          contentService.addComplexMapRefSetMember(complexMapRefSetMember);

          // regularly commit at intervals
          if (++objectCt % commitCt == 0) {
            getLog().info("    commit = " + objectCt);
            contentService.commit();
            contentService.beginTransaction();
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
    contentService.commit();
    contentService.close();

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
    final ContentService contentService = new ContentServiceJpa();
    contentService.setTransactionPerOperation(false);
    contentService.beginTransaction();

    while ((line = extendedMapRefsetsByConcept.readLine()) != null) {

      line = line.replace("\r", "");
      final String fields[] = line.split("\t");

      if (!fields[0].equals("id")) { // header
        final ComplexMapRefSetMember complexMapRefSetMember =
            new ComplexMapRefSetMemberJpa();

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
        final Concept concept = contentService.getConcept(fields[5],terminology,version);

        if (concept != null) {
          complexMapRefSetMember.setConcept(concept);
          contentService.addComplexMapRefSetMember(complexMapRefSetMember);

          // regularly commit at intervals
          if (++objectCt % commitCt == 0) {
            getLog().info("    commit = " + objectCt);
            contentService.commit();
            contentService.beginTransaction();
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
    contentService.commit();
    contentService.close();

    // print memory information
    Runtime runtime = Runtime.getRuntime();
    getLog().info("MEMORY USAGE:");
    getLog().info(" Total: " + runtime.totalMemory());
    getLog().info(" Free:  " + runtime.freeMemory());
    getLog().info(" Max:   " + runtime.maxMemory());
  }
}
