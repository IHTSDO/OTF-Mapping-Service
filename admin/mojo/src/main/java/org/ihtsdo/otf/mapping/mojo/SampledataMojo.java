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
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapLead;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapRecord;
import org.ihtsdo.otf.mapping.model.MapSpecialist;
import org.ihtsdo.otf.mapping.rf2.ComplexMapRefSetMember;
//import org.ihtsdo.otf.mapping.jpa.MapBlockJpa; TODO: Removed these elements, update/rethink/etc.
//import org.ihtsdo.otf.mapping.jpa.MapGroupJpa;

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
	@SuppressWarnings("unchecked")
	@Override
	public void execute() throws MojoFailureException {

		EntityManagerFactory factory =
				Persistence.createEntityManagerFactory("MappingServiceDS");
		manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();
		

		try {
					
			getLog().info("Load Sample data");

			// ASSUMPTION: Database is unloaded, starting fresh

			List<MapProject> projects = new ArrayList<>();
			List<MapSpecialist> specialists = new ArrayList<>();
			List<MapLead> leads = new ArrayList<>();

			// Add Specialists and Leads
			MapLeadJpa mapLead = new MapLeadJpa();
			mapLead.setName("Kathy Giannangelo");
			mapLead.setUserName("kgi");
			mapLead.setEmail("kgi@ihtsdo.org");
			leads.add(mapLead);

			mapLead = new MapLeadJpa();
			mapLead.setName("Donna Morgan");
			mapLead.setUserName("dmo");
			mapLead.setEmail("dmo@ihtsdo.org");
			leads.add(mapLead);

			mapLead = new MapLeadJpa();
			mapLead.setName("Julie O'Halloran");
			mapLead.setUserName("joh");
			mapLead.setEmail("julie.ohalloran@sydney.edu.au");
			leads.add(mapLead);

			MapSpecialistJpa mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Krista Lilly");
			mapSpecialist.setUserName("kli");
			mapSpecialist.setEmail("kli@ihtsdo.org");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Nicola Ingram");
			mapSpecialist.setUserName("nin");
			mapSpecialist.setEmail("nin@ihtsdo.org");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Rory Davidson");
			mapSpecialist.setUserName("rda");
			mapSpecialist.setEmail("rda@ihtsdo.org");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Julie O'Halloran");
			mapSpecialist.setUserName("joh");
			mapSpecialist.setEmail("julie.ohalloran@sydney.edu.au");
			specialists.add(mapSpecialist);

			mapSpecialist = new MapSpecialistJpa();
			mapSpecialist.setName("Graeme Miller");
			mapSpecialist.setUserName("gmi");
			mapSpecialist.setEmail("graeme.miller@sydney.edu.au");
			specialists.add(mapSpecialist);

			tx.begin();
			for (MapSpecialist m : specialists) {
				Logger.getLogger(this.getClass()).info(
						"Adding map specialist " + m.getName());
				manager.persist(m);
			}

			for (MapLead m : leads) {
				Logger.getLogger(this.getClass())
						.info("Adding map lead " + m.getName());
				manager.persist(m);
			}
			tx.commit();

			// Add map advice
			List<MapAdvice> mapAdvices = new ArrayList<>();

			String[] adviceValues =
					new String[] {
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
							"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29(Burns)"
					};

			String[] icd10AdviceValues =
					new String[] {
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
							"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T29(Burns)"
					};
			String[] icd9cmAdviceValues =
					new String[] {
							"SNOMED CT source code not mappable to target coding scheme",
							"Exact match map from SNOMED CT source code to target code",
							"Narrow to broad map from SNOMED CT source code to target code",
							"Broad to narrow map from SNOMED CT source code to target code",
							"Partial overlap between SNOMED CT source code and target code"
					};

			String[] icpcAdviceValues =
					new String[] {
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
							"USE AS PRIMARY CODE ONLY IF SITE OF BURN UNSPECIFIED, OTHERWISE USE AS A SUPPLEMENTARY CODE WITH CATEGORIES T20-T25 (Burns)"
					};

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
			Map<Long, Long> refSetIdToMapProjectIdMap = new HashMap<Long, Long>();
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
			Long mapProjectId = new Long("1");
			mapProject.setId(mapProjectId);
			refSetIdToMapProjectIdMap.put(mapProject.getRefSetId(), mapProjectId);
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
			mapProjectId = new Long("2");
			mapProject.setId(mapProjectId);
			refSetIdToMapProjectIdMap.put(mapProject.getRefSetId(), mapProjectId);
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
			mapProject.addMapLead(leads.get(2));
			mapProject.addMapSpecialist(specialists.get(3));
			for (String s : icpcAdviceValues) {
				mapProject.addMapAdvice(mapAdviceValueMap.get(s));
			}
			mapProjectId = new Long("3");
			mapProject.setId(mapProjectId);
			refSetIdToMapProjectIdMap.put(mapProject.getRefSetId(), mapProjectId);
			projects.add(mapProject);

			// Set to assign map records to a project
			Map<Long, Long> projectRefSetIdMap = new HashMap<Long, Long>();
			
			tx.begin();
			for (MapProject m : projects) {
				
				
				
				
				Logger.getLogger(this.getClass()).info(
						"Adding map project " + m.getName());
				manager.merge(m);
			}
			tx.commit();
			
			javax.persistence.Query  query = manager.createQuery("select r from MapProjectJpa r");
			
			projects = query.getResultList();			
			
			for (MapProject m : projects) {

				// <RefSetId, ProjectId>
				projectRefSetIdMap.put(m.getRefSetId(), m.getId());
				getLog().debug("    Add entry to map " + m.getRefSetId() + ", " + m.getId().toString());
			}

			// Load map records from complex map refset members
			long prevConceptId = -1;
			MapRecord mapRecord = null;

		  query =
					manager
							.createQuery("select r from ComplexMapRefSetMemberJpa r order by r.concept.id, " +
									"r.mapBlock, r.mapGroup, r.mapPriority");
			getLog().debug("    complex refset member size "
					+ query.getResultList().size());
			
			// Added to speed up process
			tx.begin();
			int i = 0;// for progress tracking

			for (Object member : query.getResultList()) {
				
				ComplexMapRefSetMember refSetMember = (ComplexMapRefSetMember) member;
				
				if(refSetMember.getMapRule().matches("IFA\\s\\d*\\s\\|.*\\s\\|") &&
			    !(refSetMember.getMapAdvice().contains("MAP IS CONTEXT DEPENDENT FOR GENDER")) &&
			    !(refSetMember.getMapRule().matches("IFA\\s\\d*\\s\\|\\s.*\\s\\|\\s[<>]"))){
				  getLog().debug("    skipping refSetMember: " + refSetMember.getConcept().getTerminologyId() + " : " + 
				    refSetMember.getMapRule() + " : " + refSetMember.getMapAdvice()); 
				  continue; 
				}
				
				// if no concept for this ref set member, skip
				if (refSetMember.getConcept() == null)
					continue;
				
				// if different concept than previous ref set member, create new mapRecord
				if (!refSetMember.getConcept().getTerminologyId()
						.equals(new Long(prevConceptId).toString())) {
					
					mapRecord = new MapRecordJpa();
					mapRecord.setConceptId(refSetMember.getConcept().getTerminologyId());
					
					// if this refSet terminology id in project map, set the project id
					if (projectRefSetIdMap.containsKey(refSetMember.getRefSetId())) {
						mapRecord.setMapProjectId(projectRefSetIdMap.get(refSetMember.getRefSetId()));
						getLog().info("Adding map record to project " + projectRefSetIdMap.get(refSetMember.getRefSetId()).toString());
					} else {
						getLog().info("No map project for this record");
					}
				
					// set the previous concept to this concept
					prevConceptId = new Long(refSetMember.getConcept().getTerminologyId());
					
					// persist the record
					manager.persist(mapRecord);
				}

				// add map entry to record
				MapEntry mapEntry = new MapEntryJpa();
				mapEntry.setTarget(refSetMember.getMapTarget());
				mapEntry.setMapRecord(mapRecord);
				mapEntry.setRelationId(refSetMember.getMapRelationId().toString());
				mapEntry.setRule(refSetMember.getMapRule());
				mapEntry.setMapGroup(1);
				mapEntry.setMapBlock(1);
				
				// find the correct advice and add it
				if (mapAdviceValueMap.containsKey(refSetMember.getMapAdvice())) {
					mapEntry
							.addMapAdvice(mapAdviceValueMap.get(refSetMember.getMapAdvice()));
				}
				mapRecord.addMapEntry(mapEntry);
				
				manager.merge(mapRecord);
				
				if (++i % 1000 == 0) {System.out.println(Integer.toString(i) + " map records processed");}

			}
			
			// Commit all map records
			getLog().info("     Committing...");
			tx.commit();
			getLog().info("...done");
			manager.close();
			factory.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
