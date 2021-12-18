/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.handlers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.ConceptList;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.helpers.TerminologyUtility;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapRelation;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.FileSorter;

/**
 * Implementation for sample allergy mapping project. Require valid codes to be
 * allergies.
 */
public class ICD10NOProjectSpecificAlgorithmHandler extends DefaultProjectSpecificAlgorithmHandler {

  /** The icd10 maps for preloading. */
  private static Map<String, MapRecord> existingIcd10Maps = new HashMap<>();

  /** The UK icd10 maps for preloading. */
  private static Map<String, MapNote> UKIcd10MapsAsNotes = new HashMap<>();

  /* see superclass */
  @Override
  public void initialize() throws Exception {
    Logger.getLogger(getClass()).info("Running initialize for " + getClass().getSimpleName());
    // Populate any project-specific caches.
    cacheExistingMaps();
  }

  /* see superclass */
  @Override
  public ValidationResult validateTargetCodes(MapRecord mapRecord) throws Exception {

    // No current validation restrictions

    final ValidationResult validationResult = new ValidationResultJpa();

    return validationResult;
  }

  /* see superclass */
  @Override
  public MapRecord computeInitialMapRecord(MapRecord mapRecord) throws Exception {

    try {

      if (existingIcd10Maps.isEmpty()) {
        cacheExistingMaps();
      }

      MapRecord existingMapRecord = existingIcd10Maps.get(mapRecord.getConceptId());

      return existingMapRecord;
    } catch (Exception e) {
      throw e;
    } finally {
      // n/a
    }
  }

  /* see superclass */
  @Override
  public ValidationResult validateSemanticChecks(MapRecord mapRecord) throws Exception {
    final ValidationResult result = new ValidationResultJpa();

    // Map record name must share at least one word with target name
    final Set<String> recordWords =
        new HashSet<>(Arrays.asList(mapRecord.getConceptName().toLowerCase().split(" ")));
    final Set<String> entryWords = new HashSet<>();
    for (final MapEntry entry : mapRecord.getMapEntries()) {
      if (entry.getTargetName() != null) {
        entryWords.addAll(Arrays.asList(entry.getTargetName().toLowerCase().split(" ")));
      }
    }
    final Set<String> recordMinusEntry = new HashSet<>(recordWords);
    recordMinusEntry.removeAll(entryWords);

    // If there are entry words and none match, warning
    // if (entryWords.size() > 0 && recordWords.size() ==
    // recordMinusEntry.size()) {
    // result
    // .addWarning("From concept and target code names must share at least one
    // word.");
    // }

    return result;
  }

  /**
   * Cache existing maps.
   *
   * @throws Exception the exception
   */
  private void cacheExistingMaps() throws Exception {
    // Lookup if this concept has an existing ICD10 map record to pre-load
    // Up to date map release file must be saved here:
    // {data.dir}/doc/{projectNumber}/preloadMaps/ExtendedMapSnapshot.txt

    // In addition, this project is loading in the UK edition maps as notes.
    // Up to date UK map release file must be saved here:
    // {data.dir}/doc/{projectNumber}/preloadMaps/ExtendedMapUKCLSnapshot.txt

    final ContentService contentService = new ContentServiceJpa();
    final MappingService mappingService = new MappingServiceJpa();

    Logger.getLogger(ICD10NOProjectSpecificAlgorithmHandler.class)
        .info("Caching the existing ICD10 maps");

    final String dataDir = ConfigUtility.getConfigProperties().getProperty("data.dir");
    if (dataDir == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Check preconditions
    String inputFile =
        dataDir + "/doc/" + mapProject.getId() + "/preloadMaps/ExtendedMapSnapshot.txt";

    if (!new File(inputFile).exists()) {
      throw new Exception("Specified input file missing: " + inputFile);
    }

    String inputFile2 =
        dataDir + "/doc/" + mapProject.getId() + "/preloadMaps/ExtendedMapUKCLSnapshot.txt";

    if (!new File(inputFile2).exists()) {
      throw new Exception("Specified input file missing: " + inputFile2);
    }

    // Preload all concepts and create terminologyId->name maps, to avoid having
    // to do individual lookups later
    ConceptList sourceConcepts = contentService.getAllConcepts(mapProject.getSourceTerminology(),
        mapProject.getSourceTerminologyVersion());
    ConceptList destinationConcepts = contentService.getAllConcepts(
        mapProject.getDestinationTerminology(), mapProject.getDestinationTerminologyVersion());

    Map<String, String> sourceIdToName = new HashMap<>();
    Map<String, String> destinationIdToName = new HashMap<>();

    for (final Concept concept : sourceConcepts.getConcepts()) {
      sourceIdToName.put(concept.getTerminologyId(), concept.getDefaultPreferredName());
    }
    for (final Concept concept : destinationConcepts.getConcepts()) {
      destinationIdToName.put(concept.getTerminologyId(), concept.getDefaultPreferredName());
    }

    // sort input file
    Logger.getLogger(ICD10NOProjectSpecificAlgorithmHandler.class)
        .info("  Sorting the file into " + System.getProperty("java.io.tmpdir"));
    File sortedFile =
        File.createTempFile("ttt", ".sort", new File(System.getProperty("java.io.tmpdir")));
    sortedFile.delete();
    // Sort file according to unix sort
    // -k 5,5 -k 6,6n -k 7,7n -k 8,8n -k 1,4 -k 9,9 -k 10,10 -k 11,11
    // -k 12,12 -k 13,13
    FileSorter.sortFile(inputFile, sortedFile.getPath(), new Comparator<String>() {

      @Override
      public int compare(String o1, String o2) {
        String[] fields1 = o1.split("\t");
        String[] fields2 = o2.split("\t");

        // keep headers at top
        if (o1.startsWith("id")) {
          return 1;
        }

        long i = fields1[4].compareTo(fields2[4]);
        if (i != 0) {
          return (int) i;
        } else {
          i = fields1[5].compareTo(fields2[5]);
          // i = (Long.parseLong(fields1[5]) -
          // Long.parseLong(fields2[5]));
          if (i != 0) {
            return (int) i;
          } else {
            i = Long.parseLong(fields1[6]) - Long.parseLong(fields2[6]);
            if (i != 0) {
              return (int) i;
            } else {
              i = Long.parseLong(fields1[7]) - Long.parseLong(fields2[7]);
              if (i != 0) {
                return (int) i;
              } else {
                i = (fields1[0] + fields1[1] + fields1[2] + fields1[3])
                    .compareTo(fields1[0] + fields1[1] + fields1[2] + fields1[3]);
                if (i != 0) {
                  return (int) i;
                } else {
                  i = fields1[8].compareTo(fields2[8]);
                  if (i != 0) {
                    return (int) i;
                  } else {
                    i = fields1[9].compareTo(fields2[9]);
                    if (i != 0) {
                      return (int) i;
                    } else {
                      i = fields1[10].compareTo(fields2[10]);
                      if (i != 0) {
                        return (int) i;
                      } else {
                        i = fields1[11].compareTo(fields2[11]);
                        if (i != 0) {
                          return (int) i;
                        } else {

                          // complex maps do not have mapCategory field
                          if (fields1.length == 12) {
                            return 0;
                          }

                          // extended maps have extra mapCategory field
                          i = fields1[12].compareTo(fields2[12]);
                          if (i != 0) {
                            return (int) i;
                          } else {
                            return 0;
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    });
    Logger.getLogger(ICD10NOProjectSpecificAlgorithmHandler.class).info("  Done sorting the file ");

    // Open reader and service
    BufferedReader preloadMapReader = new BufferedReader(new FileReader(sortedFile));

    String line = null;

    while ((line = preloadMapReader.readLine()) != null) {
      String fields[] = line.split("\t");

      // Only keep ICD10 maps (refset=447562003)
      if (!fields[4].equals("447562003")) {
        continue;
      }

      // Only keep active maps
      if (!fields[2].equals("1")) {
        continue;
      }

      final String conceptId = fields[5];
      final String mapGroup = fields[6];
      final String mapPriority = fields[7];
      final String mapRule = fields[8];
      final String mapAdviceStr = fields[9];
      final String mapTarget = fields[10];
      final String correlationId = fields[11];
      final String mapCategoryId = fields[12];
      
      // The first time a conceptId is encountered, set up the map (only need
      // very limited information for the purpose of this function
      if (existingIcd10Maps.get(conceptId) == null) {
        MapRecord icd10MapRecord = new MapRecordJpa();
        icd10MapRecord.setConceptId(conceptId);
        String sourceConceptName = sourceIdToName.get(conceptId);
        if (sourceConceptName != null) {
          icd10MapRecord.setConceptName(sourceConceptName);
        } else {
          icd10MapRecord
              .setConceptName("CONCEPT DOES NOT EXIST IN " + mapProject.getSourceTerminology());
        }

        existingIcd10Maps.put(conceptId, icd10MapRecord);
      }
      MapRecord icd10MapRecord = existingIcd10Maps.get(conceptId);

      // For each line, create a map entry, and attach it to the record
      MapEntry mapEntry = new MapEntryJpa();
      mapEntry.setMapGroup(Integer.parseInt(mapGroup));
      mapEntry.setMapPriority(Integer.parseInt(mapPriority));
      mapEntry.setTargetId(mapTarget);
      String targetConceptName = destinationIdToName.get(mapTarget);

      if (targetConceptName != null) {
        mapEntry.setTargetName(targetConceptName);
      } else {
        mapEntry
            .setTargetName("CONCEPT DOES NOT EXIST IN " + mapProject.getDestinationTerminology());
      }

      // Load map rule
      if (mapRule.equals("OTHERWISE TRUE")) {
        mapEntry.setRule("TRUE");
      } else {
        mapEntry.setRule(mapRule);
      }

      // load map relation
      for (final MapRelation relation : mapProject.getMapRelations()) {
        if (relation.getTerminologyId().equals(mapCategoryId)) {
          mapEntry.setMapRelation(relation);
          continue;
        }
      }

      // Load map advices
      final Set<MapAdvice> advices = new HashSet<>();

      String splitAdvices[] = mapAdviceStr.split("\\|");
      for (String advice : splitAdvices) {
        // skip IF/ALWAYS
        if (advice.trim().startsWith("ALWAYS ") || advice.trim().startsWith("IF ")) {
          continue;
        }
        // don't add advices that are duplicates of the map relation
        else if (mapEntry.getMapRelation() != null
            && mapEntry.getMapRelation().getName().equals(advice.trim())) {
          continue;
        }
        // Otherwise, add the advice as-is
        else {
          MapAdvice adviceToAdd = null;
          try {
            adviceToAdd = TerminologyUtility.getAdvice(mapProject, advice.trim());
          } catch (Exception e) {
            Logger.getLogger(ICD10NOProjectSpecificAlgorithmHandler.class)
                .warn("Advice not found: " + advice.trim());
          }
          advices.add(adviceToAdd);
        }

        mapEntry.setMapAdvices(advices);
      }
      
      // Add the entry to the record, and put the updated record in the map
      icd10MapRecord.addMapEntry(mapEntry);
      existingIcd10Maps.put(conceptId, icd10MapRecord);
      
    }

    Logger.getLogger(getClass()).info("Done caching maps");

    Logger.getLogger(ICD10NOProjectSpecificAlgorithmHandler.class)
        .info("Transforming UK maps into notes, and adding to cached maps");

    // sort input file
    Logger.getLogger(ICD10NOProjectSpecificAlgorithmHandler.class)
        .info("  Sorting the file into " + System.getProperty("java.io.tmpdir"));
    File sortedFile2 =
        File.createTempFile("ttt", ".sort", new File(System.getProperty("java.io.tmpdir")));
    sortedFile2.delete();
    // Sort file according to unix sort
    // -k 5,5 -k 6,6n -k 7,7n -k 8,8n -k 1,4 -k 9,9 -k 10,10 -k 11,11
    // -k 12,12 -k 13,13
    FileSorter.sortFile(inputFile2, sortedFile2.getPath(), new Comparator<String>() {

      @Override
      public int compare(String o1, String o2) {
        String[] fields1 = o1.split("\t");
        String[] fields2 = o2.split("\t");

        // keep headers at top
        if (o1.startsWith("id")) {
          return 1;
        }

        long i = fields1[4].compareTo(fields2[4]);
        if (i != 0) {
          return (int) i;
        } else {
          i = fields1[5].compareTo(fields2[5]);
          // i = (Long.parseLong(fields1[5]) -
          // Long.parseLong(fields2[5]));
          if (i != 0) {
            return (int) i;
          } else {
            i = Long.parseLong(fields1[6]) - Long.parseLong(fields2[6]);
            if (i != 0) {
              return (int) i;
            } else {
              i = Long.parseLong(fields1[7]) - Long.parseLong(fields2[7]);
              if (i != 0) {
                return (int) i;
              } else {
                i = (fields1[0] + fields1[1] + fields1[2] + fields1[3])
                    .compareTo(fields1[0] + fields1[1] + fields1[2] + fields1[3]);
                if (i != 0) {
                  return (int) i;
                } else {
                  i = fields1[8].compareTo(fields2[8]);
                  if (i != 0) {
                    return (int) i;
                  } else {
                    i = fields1[9].compareTo(fields2[9]);
                    if (i != 0) {
                      return (int) i;
                    } else {
                      i = fields1[10].compareTo(fields2[10]);
                      if (i != 0) {
                        return (int) i;
                      } else {
                        i = fields1[11].compareTo(fields2[11]);
                        if (i != 0) {
                          return (int) i;
                        } else {

                          // complex maps do not have mapCategory field
                          if (fields1.length == 12) {
                            return 0;
                          }

                          // extended maps have extra mapCategory field
                          i = fields1[12].compareTo(fields2[12]);
                          if (i != 0) {
                            return (int) i;
                          } else {
                            return 0;
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    });
    Logger.getLogger(ICD10NOProjectSpecificAlgorithmHandler.class).info("  Done sorting the file ");

    // Close and re-open reader and service
    preloadMapReader.close();
    preloadMapReader = new BufferedReader(new FileReader(sortedFile2));

    // All notes will belong to the 'loader' user.
    MapUser loaderUser = mappingService.getMapUser("loader");

    line = null;

    while ((line = preloadMapReader.readLine()) != null) {
      String fields2[] = line.split("\t");

      // Only keep ICD10 maps (refset=999002271000000101)
      if (!fields2[4].equals("999002271000000101")) {
        continue;
      }

      // Only keep active maps
      if (!fields2[2].equals("1")) {
        continue;
      }

      final String conceptId2 = fields2[5];
      final String mapGroup2 = fields2[6];
      final String mapPriority2 = fields2[7];
      final String mapRule2 = fields2[8];
      final String mapAdviceStr2 = fields2[9];
      final String mapTarget2 = fields2[10];
      final String correlationId2 = fields2[11];
      final String mapCategoryId2 = fields2[12];

      // The first time a conceptId is encountered, set up the map note.
      // Subsequent times, append to existing note string.
      if (UKIcd10MapsAsNotes.get(conceptId2) == null) {
        MapNote UKIcd10MapAsNote = new MapNoteJpa();
        UKIcd10MapAsNote.setUser(loaderUser);
        StringBuilder noteStringBuilder = new StringBuilder();
        noteStringBuilder.append("UK Map for " + conceptId2 + ":<br><br>");
        noteStringBuilder.append("Map Entries<br>");
        noteStringBuilder
            .append(mapGroup2 + "/" + mapPriority2 + "   " + mapAdviceStr2 + "   " + mapRule2);

        UKIcd10MapAsNote.setNote(noteStringBuilder.toString());

        UKIcd10MapsAsNotes.put(conceptId2, UKIcd10MapAsNote);
      } else {
        MapNote UKIcd10MapAsNote = UKIcd10MapsAsNotes.get(conceptId2);
        StringBuilder noteStringBuilder = new StringBuilder();
        noteStringBuilder.append(UKIcd10MapAsNote.getNote());
        noteStringBuilder.append("<br>");
        noteStringBuilder
            .append(mapGroup2 + "/" + mapPriority2 + "   " + mapAdviceStr2 + "   " + mapRule2);

        UKIcd10MapAsNote.setNote(noteStringBuilder.toString());

        UKIcd10MapsAsNotes.put(conceptId2, UKIcd10MapAsNote);
      }
    }

    // Once all of the notes are constructed, add them to the cached map records
    for (final String mapConceptId : UKIcd10MapsAsNotes.keySet()) {
      MapRecord icd10MapRecord = existingIcd10Maps.get(mapConceptId);
      if (icd10MapRecord != null) {
        icd10MapRecord.addMapNote(UKIcd10MapsAsNotes.get(mapConceptId));
        existingIcd10Maps.put(mapConceptId, icd10MapRecord);
      }
    }

    Logger.getLogger(getClass()).info("Done processing UK maps");

    preloadMapReader.close();
    contentService.close();
    mappingService.close();
  }

}
