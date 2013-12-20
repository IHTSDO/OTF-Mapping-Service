package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapAdviceJpa;
import org.ihtsdo.otf.mapping.jpa.MapBlockJpa;
import org.ihtsdo.otf.mapping.jpa.MapEntryJpa;
import org.ihtsdo.otf.mapping.jpa.MapGroupJpa;
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapRecordJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
import org.ihtsdo.otf.mapping.model.MapAdvice;
import org.ihtsdo.otf.mapping.model.MapBlock;
import org.ihtsdo.otf.mapping.model.MapEntry;
import org.ihtsdo.otf.mapping.model.MapGroup;
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

		try {

			getLog().info("Load Sample data");

			EntityManagerFactory factory = Persistence.createEntityManagerFactory("MappingServiceDS");
			manager = factory.createEntityManager();
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
		
			manager.createQuery("delete from MapSpecialistJpa").executeUpdate();
			manager.createQuery("delete from MapLeadJpa").executeUpdate();
			manager.createQuery("delete from MapProjectJpa").executeUpdate();
			tx.commit();

			
			/** The lists */
			List<MapProject> projects = new ArrayList<MapProject>();
		    List<MapSpecialist> specialists = new ArrayList<MapSpecialist>();
			List<MapLead> leads = new ArrayList<MapLead>();
			
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


			tx.begin();
			for (MapSpecialist m : specialists) {
				System.out.println("Adding map specialist " + m.getName());
				manager.persist(m);
			}
			
			for (MapLead m : leads) {
				System.out.println("Adding map lead " + m.getName());
				manager.persist(m);
			}
			tx.commit();
			
			MapProject mapProject = new MapProjectJpa();
			mapProject.setName("SNOMED to ICD10");
			mapProject.setRefSetId(new Long("5781349"));
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
			projects.add(mapProject);
			
			mapProject = new MapProjectJpa();
			mapProject.setName("SNOMED to ICPC - Family Practice/GPF Refset");
			mapProject.setRefSetId(new Long("5235669"));
			mapProject.setSourceTerminology("SNOMEDCT");
			mapProject.setSourceTerminologyVersion("20130731");
			mapProject.setDestinationTerminology("ICPC");
			mapProject.setDestinationTerminologyVersion("21");
			mapProject.setBlockStructure(false);
			mapProject.setGroupStructure(false);
			mapProject.setPublished(false);
			mapProject.addMapLead(leads.get(1));
			mapProject.addMapSpecialist(specialists.get(0));
			mapProject.addMapSpecialist(specialists.get(1));
			projects.add(mapProject);
			
			
			tx.begin();
			for (MapProject m : projects) {
				System.out.println("Adding map project " + m.getName());
				manager.merge(m);
			}
			tx.commit();
			
		// Load map records from complex map refset members
			long prevConceptId = -1;
			long prevBlockId = -1;
			long prevGroupId = -1;
			MapRecord mapRecord = null;
			MapBlock mapBlock = null;
			MapGroup mapGroup = null;
			
			javax.persistence.Query query = manager.createQuery("select r from ComplexMapRefSetMemberJpa r order by r.concept.id");
			System.out.println("complex refset member size " + query.getResultList().size());
			
			for (Object member : query.getResultList()) {
				ComplexMapRefSetMember refSetMember = (ComplexMapRefSetMember)member;
				/**if(refSetMember.getMapRule().matches("IFA \\d* | .* |") &&
						!(refSetMember.getMapAdvice().contains("MAP IS CONTEXT DEPENDENT FOR GENDER")) &&
						!(refSetMember.getMapRule().matches("IFA \\d* | .* | [<>]"))){
					System.out.println(refSetMember.getMapRule());
					continue;
				}*/
				if (refSetMember.getConcept() == null)
					continue;
				if (!refSetMember.getConcept().getTerminologyId().equals(new Long(prevConceptId).toString())) {
					if (mapRecord != null) {
						tx.begin();
						manager.persist(mapRecord);
						tx.commit();
					}
					mapRecord = new MapRecordJpa();
					if (refSetMember.getConcept() != null) {
					  mapRecord.setConceptId(refSetMember.getConcept().getTerminologyId());
					
					mapBlock = new MapBlockJpa();
					mapBlock.setIndex(refSetMember.getMapBlock());
					prevBlockId = refSetMember.getMapBlock();
					mapRecord.addMapBlock(mapBlock);
					
					mapGroup = new MapGroupJpa();
					mapGroup.setIndex(refSetMember.getMapGroup());
					prevGroupId = refSetMember.getMapGroup();
					mapRecord.addMapGroup(mapGroup);
				}
				if (refSetMember.getMapBlock() != prevBlockId) {
					mapBlock = new MapBlockJpa();
					mapBlock.setIndex(refSetMember.getMapBlock());
					prevBlockId = refSetMember.getMapBlock();
					mapRecord.addMapBlock(mapBlock);
				}
			  if (refSetMember.getMapGroup() != prevGroupId) {
						mapGroup = new MapGroupJpa();
						mapGroup.setIndex(refSetMember.getMapGroup());
						prevGroupId = refSetMember.getMapGroup();
						mapRecord.addMapGroup(mapGroup);
						if (mapBlock != null)
						  mapBlock.addMapGroup(mapGroup);
				}
			  MapEntry mapEntry = new MapEntryJpa();
			  mapEntry.setTarget(refSetMember.getMapTarget());
			  mapEntry.setMapRecord(mapRecord);
			  mapEntry.setRelationId(refSetMember.getMapRelationId().toString());
			  mapEntry.setRule(refSetMember.getMapRule());
			  MapAdvice mapAdvice = new MapAdviceJpa();
			  // TODO: find the correct advice and add it
			  mapRecord.addMapEntry(mapEntry);
				
			}
		/**	
		 * //for (ComplexMapRefSetMember member : select r from ComplexMapRefSetMember order by r.concept.terminologyid, r.mapblock, r.mapgroup, r.mappriority
			
		 * -- Skip entries where the rule matches “IFA \d* | .* |”
			   * unless gender rule - the advice contains “MAP IS CONTEXT DEPENDENT FOR GENDER”
			   * unless age rule – the rule matches “IFA \d* | .* | [<>]”

			   -- whenever concept id changes, make a new map record, map block, map group
			      * if (mapRecord != null) manager.persist(mapRecord).
			   -- whenever mapblock changes, make a new mapblock, add it to the current map record
			   -- whenever mapgroup chagnes, make a new group, add it to the current block and map record
			   -- always make a new entry, add it to the current group and mapRecord

			** also need to handle "advice"*/
			}

			
			System.out.println(".. done loading sample data");
			manager.close();
			factory.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
