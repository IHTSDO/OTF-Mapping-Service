package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
//import org.ihtsdo.otf.mapping.jpa.MapBlockJpa; TODO: Removed these elements, update/rethink/etc.
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
//import org.ihtsdo.otf.mapping.jpa.MapGroupJpa;
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

		EntityManagerFactory factory =
				Persistence.createEntityManagerFactory("MappingServiceDS");
		manager = factory.createEntityManager();
		EntityTransaction tx = manager.getTransaction();

		
		
		/*// truncate the tables -- don't want to have to rerun loader for refsets
		tx.begin();
		try {
			java.persistence.Query query;
			int deleteRecords;
			getLog().info("Removing table constraints");
			
			// truncate RefSets


			
			
			 * Dependencies:
			 * map_advices: (none)
			 * map_blocks: map_advices (1), map_groups (3)
			 * map_entries: map_advices (1), map_notes (1)
			 * map_groups: map_entries (2)
			 * map_notes: (none)
			 * map_projects: map_advices (1), map_leads (1), map_specialists (1)
			 * map_records: map_notes (1)
			 * map_leads: (none)
			 * map_specialists: (none)
			 * map_users: (none)
			 * 
			 
			
			
			
			// projects - remove all linked elements	
			query = manager.createQuery("SELECT m from MapProjectJpa m");
			List <MapProject> mplist = (List<MapProject>) query.getResultList();
			
			for (MapProject m : mplist) {
				
				m.setMapLeads(null);
				m.setMapSpecialists(null);
				m.setMapAdvices(null);
				
				manager.merge(m);
			}
			
			
			// map groups -- remove constraints
			query = manager.createQuery("SELECT m from MapGroupJpa m");
			List <MapGroup> mglist = (List<MapGroup>) query.getResultList();
			
			for (MapGroup m : mglist) {
				
				m.setMapEntries(null);	
				
				manager.merge(m);
			}
			
			// map entries -- remove constraints
			query = manager.createQuery("SELECT m from MapEntryJpa m");
			List <MapEntry> malist = (List<MapEntry>) query.getResultList();
			
			for (MapEntry m : malist) {
				
				manager.merge(m);
			}
			
			// map blocks -- remove constraints
			query = manager.createQuery("SELECT m from MapBlockJpa m");
			List <MapBlock> mblist = (List<MapBlock>) query.getResultList();
			
			for (MapBlock m : mblist) {
				
				m.setMapAdvices(null);
				m.setMapGroups(null);
				
				manager.merge(m);
			}
			
			query = manager.createQuery("SELECT m from MapEntryJpa m");
			List <MapEntry> melist = (List<MapEntry>) query.getResultList();
			
			for (MapEntry m : melist) {
				
				m.setAdvices(null);
				m.setNotes(null);
				
				manager.merge(m);
			}
			
			query = manager.createQuery("SELECT m from MapRecordJpa m");
			List<MapRecord> mrlist = (List<MapRecord>) query.getResultList();
			
			for (MapRecord m : mrlist) {
				m.setMapBlocks(null);
				m.setMapEntries(null);
				m.setMapGroups(null);
				m.setNotes(null);
				
				manager.merge(m);
			}
			
			tx.commit();
		} catch ( Exception e ) {
		}
		try {
			tx.begin();
			
			getLog().info("Constraints removed, truncating tables");

			// delete from unconstrained tables		
			query = manager.createQuery("DELETE From MapRecordJpa rs"); // NOTE: This should cascade
			deleteRecords=query.executeUpdate();
			
			query = manager.createQuery("DELETE From MapSpecialistJpa rs");
			deleteRecords=query.executeUpdate();
			
			query = manager.createQuery("DELETE From MapLeadJpa rs");
			deleteRecords=query.executeUpdate();
			
			query = manager.createQuery("DELETE From MapUserJpa rs");
			deleteRecords=query.executeUpdate();
			
			query = manager.createQuery("DELETE From MapProjectJpa rs");
			deleteRecords=query.executeUpdate();
						
			query = manager.createQuery("DELETE From MapNoteJpa rs");
			deleteRecords=query.executeUpdate();
			
			// second set of dependencies
			query = manager.createQuery("DELETE From MapEntryJpa rs");
			deleteRecords=query.executeUpdate();
			
			query = manager.createQuery("DELETE From MapProjectJpa rs");
			deleteRecords=query.executeUpdate();
			
			query = manager.createQuery("DELETE From MapGroupJpa rs");
			deleteRecords=query.executeUpdate();
			
			query = manager.createQuery("DELETE From MapBlockJpa rs");
			deleteRecords=query.executeUpdate();*/

			
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

			// Load map records from complex map refset members
			long prevConceptId = -1;
			long prevBlockId = -1;
			long prevGroupId = -1;
			MapRecord mapRecord = null;

			javax.persistence.Query  query =
					manager
							.createQuery("select r from ComplexMapRefSetMemberJpa r order by r.concept.id, " +
									"r.mapBlock, r.mapGroup, r.mapPriority");
			System.out.println("complex refset member size "
					+ query.getResultList().size());

			for (Object member : query.getResultList()) {
				ComplexMapRefSetMember refSetMember = (ComplexMapRefSetMember) member;
				// TODO: add back this section and test with full data
				/**
				 * if(refSetMember.getMapRule().matches("IFA \\d* | .* |") &&
				 * !(refSetMember
				 * .getMapAdvice().contains("MAP IS CONTEXT DEPENDENT FOR GENDER")) &&
				 * !(refSetMember.getMapRule().matches("IFA \\d* | .* | [<>]"))){
				 * System.out.println(refSetMember.getMapRule()); continue; }
				 */
				if (refSetMember.getConcept() == null)
					continue;
				if (!refSetMember.getConcept().getTerminologyId()
						.equals(new Long(prevConceptId).toString())) {

					mapRecord = new MapRecordJpa();
					mapRecord.setConceptId(refSetMember.getConcept().getTerminologyId());

					tx.begin();
					manager.persist(mapRecord);
					tx.commit();

					/*mapBlock = new MapBlockJpa();
					mapBlock.setIndex(refSetMember.getMapBlock());
					prevBlockId = refSetMember.getMapBlock();
					mapBlock.setMapRecord(mapRecord);
					mapRecord.addMapBlock(mapBlock);

					mapGroup = new MapGroupJpa();
					mapGroup.setIndex(refSetMember.getMapGroup());
					prevGroupId = refSetMember.getMapGroup();
					mapGroup.setMapRecord(mapRecord);
					mapRecord.addMapGroup(mapGroup);*/
				}

				if (mapRecord != null && !mapRecord.getConceptId().equals("")) {
					tx.begin();
					manager.persist(mapRecord);
					tx.commit();
				}

			/*	if (refSetMember.getMapBlock() != prevBlockId) {
					mapBlock = new MapBlockJpa();
					mapBlock.setIndex(refSetMember.getMapBlock());
					prevBlockId = refSetMember.getMapBlock();
					mapBlock.setMapRecord(mapRecord);
					mapRecord.addMapBlock(mapBlock);
				}
				if (refSetMember.getMapGroup() != prevGroupId) {
					mapGroup = new MapGroupJpa();
					mapGroup.setIndex(refSetMember.getMapGroup());
					prevGroupId = refSetMember.getMapGroup();
					mapGroup.setMapRecord(mapRecord);
					mapRecord.addMapGroup(mapGroup);
					if (mapBlock != null) {
						mapBlock.addMapGroup(mapGroup);
						mapGroup.setMapBlock(mapBlock);
					}
				}*/
				MapEntry mapEntry = new MapEntryJpa();
				mapEntry.setTarget(refSetMember.getMapTarget());
				mapEntry.setMapRecord(mapRecord);
				mapEntry.setRelationId(refSetMember.getMapRelationId().toString());
				mapEntry.setRule(refSetMember.getMapRule());
				// find the correct advice and add it
				if (mapAdviceValueMap.containsKey(refSetMember.getMapAdvice())) {
					mapEntry
							.addAdvice(mapAdviceValueMap.get(refSetMember.getMapAdvice()));
				}
				mapRecord.addMapEntry(mapEntry);

			}
			/**
			 * //for (ComplexMapRefSetMember member : select r from
			 * ComplexMapRefSetMember order by r.concept.id, r.mapblock,
			 * r.mapgroup, r.mappriority
			 * 
			 * -- Skip entries where the rule matches “IFA \d* | .* |” unless gender
			 * rule - the advice contains “MAP IS CONTEXT DEPENDENT FOR GENDER” unless
			 * age rule – the rule matches “IFA \d* | .* | [<>]”
			 * 
			 * -- whenever concept id changes, make a new map record, map block, map
			 * group if (mapRecord != null) manager.persist(mapRecord). -- whenever
			 * mapblock changes, make a new mapblock, add it to the current map record
			 * -- whenever mapgroup chagnes, make a new group, add it to the current
			 * block and map record -- always make a new entry, add it to the current
			 * group and mapRecord
			 * 
			 ** also need to handle "advice"
			 */

			System.out.println(".. done loading sample data");
			manager.close();
			factory.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
