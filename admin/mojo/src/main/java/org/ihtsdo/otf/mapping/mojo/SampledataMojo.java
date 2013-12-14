package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.MapLeadJpa;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapSpecialistJpa;
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

			EntityManagerFactory factory = Persistence.createEntityManagerFactory("MappingServiceDS");
			manager = factory.createEntityManager();

			/** The lists */
			List<MapProject> projects = new ArrayList<MapProject>();
		    List<MapSpecialist> specialists = new ArrayList<MapSpecialist>();
			List<MapLead> leads = new ArrayList<MapLead>();
			
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
			
			EntityTransaction tx = manager.getTransaction();
			tx.begin();
			for (MapSpecialist m : specialists) {
				System.out.println("Adding map specialist " + m.getName());
				manager.persist(m);
			}
			
			for (MapLead m : leads) {
				System.out.println("Adding map lead " + m.getName());
				manager.persist(m);
			}
			
			for (MapProject m : projects) {
				System.out.println("Adding map project " + m.getName());
				manager.persist(m);
			}
			tx.commit();
			System.out.println(".. done loading sample data");
			manager.close();
			factory.close();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new MojoFailureException("Unexpected exception:", e);
		}

	}

}
