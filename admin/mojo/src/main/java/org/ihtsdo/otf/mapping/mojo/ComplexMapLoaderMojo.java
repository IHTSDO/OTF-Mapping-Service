package org.ihtsdo.otf.mapping.mojo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
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
public class ComplexMapLoaderMojo extends AbstractMojo {

	/**
	 * @parameter projectId
	 */
	private String projectId = null;

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

	/** The complex map reader. */
	private BufferedReader complexMapReader;

	/** The complex map ref set members. */
	private Set<ComplexMapRefSetMember> complexMapRefSetMembers =
			new HashSet<ComplexMapRefSetMember>();

	/** The content service. */
	private ContentService contentService = new ContentServiceJpa();

	/** The mapping service. */
	private MappingService mappingService = new MappingServiceJpa();

	/** The map project. */
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

			// set the input directory
			inputFile =
					properties.getProperty("loader." + terminology
							+ ".complexmap.input.data");
			if (!new File(inputFile).exists()) {
				throw new MojoFailureException("Specified loader." + terminology
						+ ".complexmap.input.data directory does not exist: " + inputFile);
			}
			Logger.getLogger(this.getClass()).info("inputFile: " + inputFile);

			if (inputFile != null && projectId == null) {
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
					if (refSetMember.getMapAdvice().startsWith("IF")
							&& !mapAdvice.contains(" | ")) {
						refSetMember.setMapAdvice("");
					} else if (mapAdvice.startsWith("ALWAYS")
							&& !mapAdvice.contains(" | ")) {
						refSetMember.setMapAdvice("");
					} else if (mapAdvice.startsWith("ALWAYS")
							&& mapAdvice.contains(" | ")) {
						refSetMember.setMapAdvice(mapAdvice.substring(mapAdvice
								.indexOf("|") + 2));
					} else if (mapAdvice.startsWith("IF") && mapAdvice.contains(" | ")) {
						refSetMember.setMapAdvice(mapAdvice.substring(mapAdvice
								.indexOf("|") + 2));
					}
				}

				// create map records
				mappingService.createMapRecordsForMapProject(mapProject,
						complexMapRefSetMembers);

				complexMapReader.close();

			} else if (projectId != null) {

				for (String id : projectId.split(",")) {
					MapProject mapProject =
							mappingService.getMapProject(new Long(id));
					// create map records
					mappingService.createMapRecordsForMapProject(mapProject,
							complexMapRefSetMembers);
				}
			}

			// clean-up
			contentService.close();
			mappingService.close();
			propertiesInputStream.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(
					"Loading of Unpublished RF2 Complex Maps failed.", e);
		} finally {
			try {
				propertiesInputStream.close();
			} catch (IOException e) {
				// do nothing
			}
		}

	}

	/**
	 * Load extended map ref sets from the file.
	 * 
	 * @throws Exception the exception
	 */
	private void loadExtendedMapRefSets() throws Exception {

		String line = "";

		while ((line = complexMapReader.readLine()) != null) {

			String fields[] = line.split("\t");
			ComplexMapRefSetMember complexMapRefSetMember =
					new ComplexMapRefSetMemberJpa();

			if (!fields[0].equals("id")) { // header

				complexMapRefSetMember.setTerminologyId(fields[0]);
				complexMapRefSetMember.setEffectiveTime(dt.parse(fields[1]));
				complexMapRefSetMember.setActive(fields[2].equals("1") ? true : false);
				complexMapRefSetMember.setModuleId(Long.valueOf(fields[3].trim()));
				complexMapRefSetMember.setRefSetId(fields[4]);
				// conceptId

				// ComplexMap unique attributes
				complexMapRefSetMember.setMapGroup(Integer.parseInt(fields[6].trim()));
				complexMapRefSetMember
						.setMapPriority(Integer.parseInt(fields[7].trim()));
				complexMapRefSetMember.setMapRule(fields[8]);
				complexMapRefSetMember.setMapAdvice(fields[9]);
				complexMapRefSetMember.setMapTarget(fields[10]);
				complexMapRefSetMember
						.setMapRelationId(Long.valueOf(fields[12].trim()));

				// ComplexMap unique attributes NOT set by file (mapBlock
				// elements)
				complexMapRefSetMember.setMapBlock(1); // default value
				complexMapRefSetMember.setMapBlockRule(null); // no default
				complexMapRefSetMember.setMapBlockAdvice(null); // no default

				// Terminology attributes
				complexMapRefSetMember
						.setTerminology(mapProject.getSourceTerminology());
				complexMapRefSetMember.setTerminologyVersion(mapProject
						.getSourceTerminologyVersion());

				// set Concept
				Concept concept =
						getConcept(
								fields[5], // referencedComponentId
								mapProject.getSourceTerminology(),
								mapProject.getSourceTerminologyVersion());

				if (concept != null) {
					complexMapRefSetMember.setConcept(concept);
					// TODO: don't persist, non-published shouldn't be in the db
					complexMapRefSetMembers.add(complexMapRefSetMember);

				} else {
					Logger.getLogger(this.getClass()).info(
							"complexMapRefSetMember "
									+ complexMapRefSetMember.getTerminologyId()
									+ " references non-existent concept " + fields[5]);
				}
			}
		}
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
		String terminologyVersion) throws Exception {

		Concept c =
				contentService.getConcept(terminologyId, terminology,
						terminologyVersion);
		return c;
	}

	/**
	 * Find map project for the file's refSetId.
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
					throw new MojoFailureException("More than one refSetId in "
							+ inputFile);
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
			throw new MojoFailureException("Map Project was not found for refsetid: "
					+ refSetId);

		mappingService.close();
	}
}