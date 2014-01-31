package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapNoteJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapNote;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.services.MappingService;

/**
 * Loads map notes.
 * 
 * @goal load-map-notes
 * @phase process-resources
 */
public class MapNotesLoaderMojo extends AbstractMojo {

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
	
	/** The manager. */
	private EntityManager manager;

  private BufferedReader mapNoteReader;
    
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

			getLog().info("Start loading map notes data ...");

			// load Properties file
			Properties properties = new Properties();
			propertiesInputStream = new FileInputStream(propertiesFile);
			properties.load(propertiesInputStream);
			propertiesInputStream.close();

			// set the input directory
			inputFile = properties.getProperty("loader.mapnotes.input.data");
			if (!new File(inputFile).exists()) {
				throw new MojoFailureException(
						"Specified loader.mapnotes.input.data directory does not exist: "
								+ inputFile);
			}
			Logger.getLogger(this.getClass()).info("inputFile: " + inputFile);
			
			// create Entitymanager
			EntityManagerFactory emFactory =
					Persistence.createEntityManagerFactory("MappingServiceDS");
			manager = emFactory.createEntityManager();

			EntityTransaction tx = manager.getTransaction();
			tx.begin();

			File file = new File(inputFile);
			
			mapNoteReader = new BufferedReader(new FileReader(file));			
			// open input file and get MapProject and version
			findMapProject();
			mapNoteReader.close();
			
			mapNoteReader = new BufferedReader(new FileReader(file));
			loadMapNotes();
			
			tx.commit();

			mapNoteReader.close();
			manager.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(
					"Loading of Unpublished RF2 Complex Maps failed.", e);
		}

	}


	private void loadMapNotes() throws Exception {

		String line = "";

		while ((line = mapNoteReader.readLine()) != null) {

			String fields[] = line.split("\t");
			MapNote mapNote = new MapNoteJpa();

			if (!fields[0].equals("id")) { // header

				//mapNote.setId(new Long(fields[0]));
				mapNote.setTimestamp(dt.parse(fields[1]));
				String note = fields[7];
				if (note.startsWith("\"")) {
					note = note.substring(1);
				}
				if (note.endsWith("\"")) {
					note = note.substring(0, note.length() -1);
			  }
				note = note.trim();
				mapNote.setNote(note);

				MappingService mappingService = new MappingServiceJpa();
				List<MapRecord> mapRecords = mappingService.getMapRecordsForConceptId(fields[5]);
				
				if (mapRecords != null && mapRecords.size() > 0) {
					for (MapRecord mapRecord : mapRecords) {
						//TODO: use mappingService.
					  mapRecord.addMapNote(mapNote);
					  manager.merge(mapRecord);
					}
					
				} else {
					Logger.getLogger(this.getClass()).info("mapNote " + mapNote.getId() +
							" references non-existent concept " + fields[5]);
				}
			}
		}
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

		while ((line = mapNoteReader.readLine()) != null) {

			String fields[] = line.split("\t");
			
			if (!fields[0].equals("id")) { // header
				if (!refSetId.equals("") && !fields[4].equals(refSetId))
					throw new MojoFailureException(
							"More than one refSetId in " + inputFile + " :" + refSetId + " " + fields[4]);
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