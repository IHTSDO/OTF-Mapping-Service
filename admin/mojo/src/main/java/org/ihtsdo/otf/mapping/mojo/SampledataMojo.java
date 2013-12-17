package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapSpecialist;

/**
 * Goal which updates the db to sync it with the model via JPA.
 * 
 * <pre>
 *   <plugin>
 *      <groupId>org.ihtsdo.otf.mapping</groupId>
 *      <artifactId>mapping-admin-mojo</artifactId>
 *      <version>${project.version}</version>
 *      <dependencies>
 *        <dependency>
 *          <groupId>org.ihtsdo.otf.mapping</groupId>
 *          <artifactId>mapping-admin-sampledata-config</artifactId>
 *          <version>${project.version}</version>
 *          <scope>system</scope>
 *          <systemPath>${project.build.directory}/mapping-admin-sampledata-${project.version}.jar</systemPath>
 *        </dependency>
 *      </dependencies>
 *      <executions>
 *        <execution>
 *          <id>sampledata</id>
 *          <phase>package</phase>
 *          <goals>
 *            <goal>sampledata</goal>
 *          </goals>
 *          <configuration>
 *            <propertiesFile>${project.build.directory}/generated-resources/resources/filters.properties.${run.config}</propertiesFile>
 *          </configuration>
 *        </execution>
 *      </executions>
 *    </plugin>
 * </pre>
 * 
 * @goal sampledata
 * 
 * @phase process-resources
 */
public class SampledataMojo extends AbstractMojo {

	/** The manager. */
	private EntityManager manager;

	/**
	 * Instantiates a {@link SampledataMojo} from the specified parameters.
	 * 
	 */
	public SampledataMojo() {
		// Do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoFailureException {

		try {

			getLog().info("Load Sample data");

			EntityManagerFactory factory = Persistence
					.createEntityManagerFactory("MappingServiceDS");
			manager = factory.createEntityManager();
			EntityTransaction tx = manager.getTransaction();
			
			// ASSUMPTION: Database is unloaded, starting fresh

			List<MapProject> projects = new ArrayList<>();
			List<MapSpecialist> specialists = new ArrayList<>();
			List<MapLead> leads = new ArrayList<>();

			// Add Specialists and Leads
			MapLeadJpa mapLead = new MapLeadJpa();
			mapLead.setName("Kathy Giannangelo");
			mapLead.setUserName("kgi");
			mapLead.setEmail("***REMOVED***");
			leads.add(mapLead);

			mapLead = new MapLeadJpa();
			mapLead.setName("Donna Morgan");
			mapLead.setUserName("dmo");
			mapLead.setEmail("***REMOVED***");
			leads.add(mapLead);

			mapLead = new MapLeadJpa();
			mapLead.setName("Julie O'Halloran");
			mapLead.setUserName("joh");
			mapLead.setEmail("***REMOVED***");
			leads.add(mapLead);

			MapSpecialistJpa mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Krista Lilly");
			mapSpecialist.setUserName("kli");
			mapSpecialist.setEmail("***REMOVED***");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Nicola Ingram");
			mapSpecialist.setUserName("nin");
			mapSpecialist.setEmail("***REMOVED***");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Rory Davidson");
			mapSpecialist.setUserName("rda");
			mapSpecialist.setEmail("***REMOVED***");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Julie O'Halloran");
			mapSpecialist.setUserName("joh");
			mapSpecialist.setEmail("***REMOVED***");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Graeme Miller");
			mapSpecialist.setUserName("gmi");
			mapSpecialist.setEmail("***REMOVED***");
			specialists.add(mapSpecialist);

			tx.begin();
			for (MapSpecialist m : specialists) {
				Logger.getLogger(this.getClass()).info(
						"Adding map specialist " + m.getName());
				manager.persist(m);
			}

			for (MapLead m : leads) {
				Logger.getLogger(this.getClass()).info(
						"Adding map lead " + m.getName());
				manager.persist(m);
			}
			tx.commit();

			// Add map advice
			List<MapAdvice> mapAdvices = new ArrayList<>();

			String[] adviceValues = new String[] {
					"ADDITIONAL CODES MAY BE REQUIRED TO IDENTIFY PLACE OF OCCURRENCE",
					"Broad to narrow map from SNOMED CT source code to target code",
					"CONSIDER ADDITIONAL CODE TO IDENTIFY SPECIFIC CONDITION OR DISEASE",
					"CONSIDER LATERALITY SPECIFICATION",
					"CONSIDER STAGE OF GLAUCOMA SPECIFICATION",
					"CONSIDER TIME OF COMA SCALE SPECIFICATION",
					"CONSIDER TOPHUS SPECIFICATION",
					"CONSIDER TRIMESTER SPECIFICATION",
					"CONSIDER WHICH FETUS IS AFFECTED BY THE MATERNAL CONDITION",
					"DESCENDANTS NOT EXHAUSTIVELY MAPPED",
					"EPISODE OF CARE INFORMATION NEEDED",
					"Exact match map from SNOMED CT source code to target code",
					"FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE",
					"MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION",
					"MAP IS CONTEXT DEPENDENT FOR GENDER",
					"MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT",
					"MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
					"MAPPED FOLLOWING IHTSDO GUIDANCE",
					"MAPPED FOLLOWING WHO GUIDANCE",
					"MAPPED WITH IHTSDO GUIDANCE",
					"MAPPED WITH NCHS GUIDANCE",
					"MAPPING GUIDANCE FROM WHO IS AMBIGUOUS",
					"Narrow to broad map from SNOMED CT source code to target code",
					"NCHS ADVISES TO ASSUME CLOSED FRACTURE",
					"Partial overlap between SNOMED CT source code and target code",
					"POSSIBLE REQUIREMENT FOR ADDITIONAL CODE TO FULLY DESCRIBE DISEASE OR CONDITION",
					"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
					"POSSIBLE REQUIREMENT FOR CAUSATIVE AGENT CODE",
					"POSSIBLE REQUIREMENT FOR CAUSATIVE DISEASE CODE",
					"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
					"POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE",
					"SNOMED CT source code not mappable to target coding scheme",
					"SOURCE CONCEPT HAS BEEN RETIRED FROM MAP SCOPE",
					"SOURCE SNOMED CONCEPT IS AMBIGUOUS",
					"SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED",
					"THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION",
					"THIS IS A MANIFESTATION CODE FOR USE IN A SECONDARY POSITION",
					"THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION",
					"THIS IS AN INFECTIOUS AGENT CODE FOR USE IN A SECONDARY POSITION",
					"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)",
					"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29(Burns)" };

			String[] icd10AdviceValues = new String[] {
					"DESCENDANTS NOT EXHAUSTIVELY MAPPED",
					"FIFTH CHARACTER REQUIRED TO FURTHER SPECIFY THE SITE",
					"MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION",
					"MAP IS CONTEXT DEPENDENT FOR GENDER",
					"MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT",
					"MAPPED FOLLOWING IHTSDO GUIDANCE",
					"MAPPED FOLLOWING WHO GUIDANCE",
					"MAPPING GUIDANCE FROM WHO IS AMBIGUOUS",
					"MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
					"POSSIBLE REQUIREMENT FOR ADDITIONAL CODE TO FULLY DESCRIBE DISEASE OR CONDITION",
					"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
					"POSSIBLE REQUIREMENT FOR CAUSATIVE AGENT CODE",
					"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
					"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
					"POSSIBLE REQUIREMENT FOR PLACE OF OCCURRENCE",
					"SOURCE CONCEPT HAS BEEN RETIRED FROM MAP SCOPE",
					"SOURCE SNOMED CONCEPT IS AMBIGUOUS",
					"SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED",
					"THIS CODE IS NOT TO BE USED IN THE PRIMARY POSITION",
					"THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION",
					"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29(Burns)" };
			String[] icd9cmAdviceValues = new String[] {
					"SNOMED CT source code not mappable to target coding scheme",
					"Exact match map from SNOMED CT source code to target code",
					"Narrow to broad map from SNOMED CT source code to target code",
					"Broad to narrow map from SNOMED CT source code to target code",
					"Partial overlap between SNOMED CT source code and target code" };

			String[] icpcAdviceValues = new String[] {
					"ADDITIONAL CODES MAY BE REQUIRED TO IDENTIFY PLACE OF OCCURRENCE",
					"CONSIDER ADDITIONAL CODE TO IDENTIFY SPECIFIC CONDITION OR DISEASE",
					"CONSIDER LATERALITY SPECIFICATION",
					"CONSIDER STAGE OF GLAUCOMA SPECIFICATION",
					"CONSIDER TIME OF COMA SCALE SPECIFICATION",
					"CONSIDER TOPHUS SPECIFICATION",
					"CONSIDER TRIMESTER SPECIFICATION",
					"CONSIDER WHICH FETUS IS AFFECTED BY THE MATERNAL CONDITION",
					"DESCENDANTS NOT EXHAUSTIVELY MAPPED",
					"EPISODE OF CARE INFORMATION NEEDED",
					"MAP CONCEPT IS OUTSIDE SCOPE OF TARGET CLASSIFICATION",
					"MAP IS CONTEXT DEPENDENT FOR GENDER",
					"MAP OF SOURCE CONCEPT IS CONTEXT DEPENDENT",
					"MAPPED WITH IHTSDO GUIDANCE",
					"MAPPED WITH NCHS GUIDANCE",
					"MAP SOURCE CONCEPT CANNOT BE CLASSIFIED WITH AVAILABLE DATA",
					"NCHS ADVISES TO ASSUME CLOSED FRACTURE",
					"POSSIBLE REQUIREMENT FOR AN EXTERNAL CAUSE CODE",
					"POSSIBLE REQUIREMENT FOR CAUSATIVE DISEASE CODE",
					"POSSIBLE REQUIREMENT FOR MORPHOLOGY CODE",
					"SOURCE SNOMED CONCEPT IS AMBIGUOUS",
					"SOURCE SNOMED CONCEPT IS INCOMPLETELY MODELED",
					"THIS IS A MANIFESTATION CODE FOR USE IN A SECONDARY POSITION",
					"THIS IS AN EXTERNAL CAUSE CODE FOR USE IN A SECONDARY POSITION",
					"THIS IS AN INFECTIOUS AGENT CODE FOR USE IN A SECONDARY POSITION",
					"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)" };

			for (String value : adviceValues) {
				MapAdvice advice = new MapAdviceJpa();
				advice.setName(value);
				advice.setDescription(value);
				mapAdvices.add(advice);
			}
			tx.begin();
			Map<String, MapAdvice> mapAdviceValueMap = new HashMap<>();
			for (MapAdvice m : mapAdvices) {
				Logger.getLogger(this.getClass()).info(
						"Adding map advice " + m.getName());
				manager.persist(m);
				mapAdviceValueMap.put(m.getName(), m);
			}
			tx.commit();

			// Add map projects
			MapProject mapProject = new MapProjectJpa();
			mapProject.setName("SNOMED to ICD10");
			mapProject.setRefSetId(new Long("447562003"));
			mapProject.setSourceTerminology("SNOMEDCT");
			mapProject.setSourceTerminologyVersion("20140131");
			mapProject.setDestinationTerminology("ICD10");
			mapProject.setDestinationTerminologyVersion("2010");
			mapProject.setBlockStructure(false);
			mapProject.setGroupStructure(true);
			mapProject.setPublished(true);
			mapProject.addMapLead(leads.get(0));
			mapProject.addMapLead(leads.get(1));
			mapProject.addMapSpecialist(specialists.get(0));
			mapProject.addMapSpecialist(specialists.get(1));
			mapProject.addMapSpecialist(specialists.get(2));
			for (String s : icd10AdviceValues) {
				mapProject.addMapAdvice(mapAdviceValueMap.get(s));
			}
			projects.add(mapProject);

			mapProject = new MapProjectJpa();
			mapProject.setName("SNOMED to ICD9CM");
			mapProject.setRefSetId(new Long("5781347179"));
			mapProject.setSourceTerminology("SNOMEDCT");
			mapProject.setSourceTerminologyVersion("20140131");
			mapProject.setDestinationTerminology("ICD9CM");
			mapProject.setDestinationTerminologyVersion("2013");
			mapProject.setBlockStructure(false);
			mapProject.setGroupStructure(true);
			mapProject.setPublished(true);
			mapProject.addMapLead(leads.get(0));
			mapProject.addMapLead(leads.get(1));
			mapProject.addMapSpecialist(specialists.get(0));
			mapProject.addMapSpecialist(specialists.get(1));
			for (String s : icd9cmAdviceValues) {
				mapProject.addMapAdvice(mapAdviceValueMap.get(s));
			}
			projects.add(mapProject);

			mapProject = new MapProjectJpa();
			mapProject.setName("SNOMED to ICPC - Family Practice/GPF Refset");
			mapProject.setRefSetId(new Long("5235669"));
			mapProject.setSourceTerminology("SNOMEDCT");
			mapProject.setSourceTerminologyVersion("20130731");
			mapProject.setDestinationTerminology("ICPC");
			mapProject.setDestinationTerminologyVersion("2");
			mapProject.setBlockStructure(false);
			mapProject.setGroupStructure(false);
			mapProject.setPublished(false);
			mapProject.addMapLead(leads.get(1));
			mapProject.addMapSpecialist(specialists.get(0));
			mapProject.addMapSpecialist(specialists.get(1));
			for (String s : icpcAdviceValues) {
				mapProject.addMapAdvice(mapAdviceValueMap.get(s));
			}
			projects.add(mapProject);

			tx.begin();
			for (MapProject m : projects) {
				Logger.getLogger(this.getClass()).info(
						"Adding map project " + m.getName());
				manager.merge(m);
			}
			tx.commit();
			Logger.getLogger(this.getClass()).info(
					".. done loading sample data");
			manager.close();
			factory.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
