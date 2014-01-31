package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
import org.ihtsdo.otf.mapping.rf2.Concept;
import org.ihtsdo.otf.mapping.rf2.jpa.ComplexMapRefSetMemberJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Loads unpublished complex maps.
 * 
 * @goal load-rf2-complex-map
 * @phase process-resources
 */
public class RF2ComplexMapLoaderMojo extends AbstractMojo {

	/** The dt. */
	private SimpleDateFormat dt = new SimpleDateFormat("yyyymmdd");

	/** The input file. */
	private String inputFile;


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

  private BufferedReader complexMapReader;
  
  private Set<ComplexMapRefSetMember> complexMapRefSetMembers = 
  		new HashSet<ComplexMapRefSetMember>();
  
  private ContentService contentService = new ContentServiceJpa();
  
  private MappingService mappingService = new MappingServiceJpa();
  
  private MapProject mapProject;
  
	/**
	 * Executes the plugin.
	 * 
	 * @throws MojoExecutionException the mojo execution exception
	 */
	@Override
	public void execute() throws MojoExecutionException {

		FileInputStream propertiesInputStream = null;
		try {

			getLog().info("Start loading " + terminology + " complex map data ...");

			// load Properties file
			Properties properties = new Properties();
			propertiesInputStream = new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);
			propertiesInputStream.close();

			// set the input directory
			inputFile = properties.getProperty("loader." + terminology + ".complexmap.input.data");
			if (!new File(inputFile).exists()) {
				throw new MojoFailureException(
						"Specified loader." + terminology + ".complexmap.input.data directory does not exist: "
								+ inputFile);
			}
			Logger.getLogger(this.getClass()).info("inputFile: " + inputFile);
			
			
			// open input file and get MapProject and version
			File file = new File(inputFile);			
			complexMapReader = new BufferedReader(new FileReader(file));
			findMapProject();
			complexMapReader.close();
			
			// load complexMapRefSetMembers from extendedMap file
			complexMapReader = new BufferedReader(new FileReader(file));
			loadExtendedMapRefSets();
			
			// qualify advice fields
			for (ComplexMapRefSetMember refSetMember : complexMapRefSetMembers) {
				
				String mapAdvice = refSetMember.getMapAdvice();
				if (refSetMember.getMapAdvice().startsWith("IF") &&
						!mapAdvice.contains(" | ")) {
					refSetMember.setMapAdvice("");
			  } else if (mapAdvice.startsWith("ALWAYS") &&
						!mapAdvice.contains(" | ")) {
					refSetMember.setMapAdvice("");
				} else if (mapAdvice.startsWith("ALWAYS") &&
						mapAdvice.contains(" | ")) {
					refSetMember.setMapAdvice(mapAdvice.substring(mapAdvice.indexOf("|") + 2));
				} else if (mapAdvice.startsWith("IF") &&
						mapAdvice.contains(" | ")) {
					refSetMember.setMapAdvice(mapAdvice.substring(mapAdvice.indexOf("|") + 2));
				}
			}
			
			// create map records
			mappingService.createMapRecordsForMapProject(mapProject, complexMapRefSetMembers);

			// clean-up
			complexMapReader.close();
			contentService.close();
			mappingService.close();
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(
					"Loading of Unpublished RF2 Complex Maps failed.", e);
		}

	}


	private void loadExtendedMapRefSets() throws Exception {

		String line = "";

		while ((line = complexMapReader.readLine()) != null) {

			String fields[] = line.split("\t");
			ComplexMapRefSetMember complexMapRefSetMember = new ComplexMapRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				complexMapRefSetMember.setTerminologyId(fields[0]);
				complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				complexMapRefSetMember.setActive(fields[2].equals("1") ? true
						: false);
				complexMapRefSetMember.setModuleId(Long.valueOf(fields[3].trim()));
				complexMapRefSetMember.setRefSetId(fields[4]);
				// conceptId

				// ComplexMap unique attributes
				complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6].trim()));
				complexMapRefSetMember.setMapPriority(Integer
						.parseInt(fields[7].trim()));
				complexMapRefSetMember.setMapRule(fields[8]);
				complexMapRefSetMember.setMapAdvice(fields[9]);
				complexMapRefSetMember.setMapTarget(fields[10]);
				complexMapRefSetMember.setMapRelationId(Long
						.valueOf(fields[12].trim()));

				// ComplexMap unique attributes NOT set by file (mapBlock
				// elements)
				complexMapRefSetMember.setMapBlock(1); // default value
				complexMapRefSetMember.setMapBlockRule(null); // no default
				complexMapRefSetMember.setMapBlockAdvice(null); // no default

				// Terminology attributes
				complexMapRefSetMember.setTerminology(mapProject.getSourceTerminology());
				complexMapRefSetMember.setTerminologyVersion(mapProject.getSourceTerminologyVersion());

				// set Concept
				Concept concept = getConcept(
						fields[5], // referencedComponentId
						mapProject.getSourceTerminology(),
						mapProject.getSourceTerminologyVersion());
				
				if (concept != null) {
					complexMapRefSetMember.setConcept(concept);
					// TODO: don't persist, non-published shouldn't be in the db
					complexMapRefSetMembers.add(complexMapRefSetMember);
					
				} else {
					Logger.getLogger(this.getClass()).info("complexMapRefSetMember " + complexMapRefSetMember.getTerminologyId() +
							" references non-existent concept " + fields[5]);
				}
			}
		}
	}
	
	/**public List<MapRecord> createMapRecordsForMapProject(MapProject mapProject) {
		
		Logger.getLogger(MappingServiceJpa.class).info("Creating map records for project" + mapProject.getName());
	
		List<MapRecord> mapRecordResults = new ArrayList<MapRecord>();
		
			
			// instantiate other local variables
			Long prevConceptId = new Long(-1);
			MapRecord mapRecord = null;
			
			for (ComplexMapRefSetMember refSetMember : complexMapRefSetMembers) {
				
				String mapAdvice = refSetMember.getMapAdvice();
				if (refSetMember.getMapAdvice().startsWith("IF") &&
						!mapAdvice.contains(" | ")) {
					refSetMember.setMapAdvice("");
			  } else if (mapAdvice.startsWith("ALWAYS") &&
						!mapAdvice.contains(" | ")) {
					refSetMember.setMapAdvice("");
				} else if (mapAdvice.startsWith("ALWAYS") &&
						mapAdvice.contains(" | ")) {
					refSetMember.setMapAdvice(mapAdvice.substring(mapAdvice.indexOf("|") + 2));
				} else if (mapAdvice.startsWith("IF") &&
						mapAdvice.contains(" | ")) {
					refSetMember.setMapAdvice(mapAdvice.substring(mapAdvice.indexOf("|") + 2));
				}
				
				// retrieve the concept
				Concept concept = refSetMember.getConcept();
				
				// if no concept for this ref set member, skip
				if (concept == null)
					continue;
				
				// if different concept than previous ref set member, create new mapRecord
				if (!concept.getTerminologyId()
						.equals(prevConceptId.toString())) {
					
					if (!prevConceptId.equals(new Long(-1))) {
						mapRecordResults.add(mapRecord);
					}
					
					mapRecord = new MapRecordJpa();
					mapRecord.setConceptId(concept.getTerminologyId());
					mapRecord.setConceptName(concept.getDefaultPreferredName());
					
					// set the map project id
					mapRecord.setMapProjectId(mapProject.getId());
				
					// get the number of descendants
					mapRecord.setCountDescendantConcepts( new Long(
							contentService.getDescendants(
								concept.getTerminologyId(),
								concept.getTerminology(),
								concept.getTerminologyVersion(),
								new Long("116680003")).size()));
			
					// set the previous concept to this concept
					prevConceptId = new Long(refSetMember.getConcept().getTerminologyId());
					
					// persist the record
					mappingService.addMapRecord(mapRecord);
					
					if (mapRecordResults.size() % 500 == 0) {Logger.getLogger(MappingServiceJpa.class).info(Integer.toString(mapRecordResults.size()) + " records created");}
				}
				// check if target is in desired terminology; if so, create entry
				
					String targetName = null;
					if (!refSetMember.getMapTarget().equals(""))
					  targetName = contentService.getConcept(refSetMember.getMapTarget(), mapProject.getDestinationTerminology(),
							mapProject.getDestinationTerminologyVersion()).getDefaultPreferredName();
					
					MapEntry mapEntry = new MapEntryJpa();
					mapEntry.setTargetId(refSetMember.getMapTarget());
					mapEntry.setTargetName(targetName);
					mapEntry.setMapRecord(mapRecord);
					mapEntry.setRelationId(refSetMember.getMapRelationId().toString());
					mapEntry.setRule(refSetMember.getMapRule());
					mapEntry.setMapGroup(1);
					mapEntry.setMapBlock(1);
					
					mapRecord.addMapEntry(mapEntry);
					
					//Add support for advices
					if (refSetMember.getMapAdvice() != null && !refSetMember.getMapAdvice().equals("")) {
					  List<MapAdvice> mapAdvices = mappingService.getMapAdvices();
					  for (MapAdvice ma : mapAdvices) {
					  	if (ma.getName().equals(refSetMember.getMapAdvice())) {
						  	mapEntry.addMapAdvice(ma);
						  	break;
						  }
					  }
					}
			}
			
			
		
		return mapRecordResults;
	}*/

	/**
	 * Returns the concept.
	 * 
	 * @param conceptId
	 *            the concept id
	 * @return the concept
	 */
	private Concept getConcept(String terminologyId, String terminology,
			String terminologyVersion) throws Exception {


    Concept c =  contentService.getConcept(terminologyId, terminology, terminologyVersion);
    return c;
	}
	
  /**
   * Find version.
   *
   * @throws Exception the exception
   */
  public void findMapProject() throws Exception {
  	
		MappingService mappingService = new MappingServiceJpa();
		
		String refSetId = "";
		
		String line = "";

		while ((line = complexMapReader.readLine()) != null) {

			String fields[] = line.split("\t");
			
			if (!fields[0].equals("id")) { // header
				if (!refSetId.equals("") && !fields[4].equals(refSetId))
					throw new MojoFailureException(
							"More than one refSetId in " + inputFile);
				refSetId = fields[4];  
			}
		}
		
		for (MapProject mp : mappingService.getMapProjects()) {
			if (mp.getRefSetId().equals(refSetId)) {
				mapProject = mp;
			  break;
		  }
		}
		if (mapProject == null)
			throw new MojoFailureException(
					"Map Project was not found for refsetid: " + refSetId);

    mappingService.close();
  }
}