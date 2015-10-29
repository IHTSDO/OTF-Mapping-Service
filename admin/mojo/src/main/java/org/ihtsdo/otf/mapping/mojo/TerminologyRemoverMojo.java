/**
 * Copyright (c) 2012 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ihtsdo.otf.mapping.mojo;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Goal which removes a terminology from a database.
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal remove-terminology
 * 
 * @phase package
 */
public class TerminologyRemoverMojo extends AbstractMojo {

  /**
   * Name of terminology to be removed.
   * @parameter
   * @required
   */
  private String terminology;

  /**
   * The terminology version.
   * @parameter
   * @required
   */
  private String terminologyVersion;

  /**
   * Instantiates a {@link TerminologyRemoverMojo} from the specified
   * parameters.
   * 
   */
  public TerminologyRemoverMojo() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting removing terminology");
    getLog().info("  terminology = " + terminology);
    getLog().info("  terminologyVersion = " + terminologyVersion);

    try {
      Properties config = ConfigUtility.getConfigProperties();

      // NOTE: ideall this would not use entity manager,
      // but we do not have services for all data types yet.
      EntityManagerFactory factory =
          Persistence.createEntityManagerFactory("MappingServiceDS", config);
      EntityManager manager = factory.createEntityManager();

      EntityTransaction tx = manager.getTransaction();
      try {

        // truncate all the tables that we are going to use first
        tx.begin();

        // truncate RefSets
        Query query =
            manager
                .createQuery("DELETE From SimpleRefSetMemberJpa rs where terminology = :terminology and terminologyVersion = :version");
        query.setParameter("terminology", terminology);
        query.setParameter("version", terminologyVersion);
        int deleteRecords = query.executeUpdate();
        getLog().info("    simple_ref_set records deleted: " + deleteRecords);

        query =
            manager
                .createQuery("DELETE From SimpleMapRefSetMemberJpa rs where terminology = :terminology and terminologyVersion = :version");
        query.setParameter("terminology", terminology);
        query.setParameter("version", terminologyVersion);
        deleteRecords = query.executeUpdate();
        getLog().info(
            "    simple_map_ref_set records deleted: " + deleteRecords);

        query =
            manager
                .createQuery("DELETE From ComplexMapRefSetMemberJpa rs where terminology = :terminology and terminologyVersion = :version");
        query.setParameter("terminology", terminology);
        query.setParameter("version", terminologyVersion);
        deleteRecords = query.executeUpdate();
        getLog().info(
            "    complex_map_ref_set records deleted: " + deleteRecords);

        query =
            manager
                .createQuery("DELETE From AttributeValueRefSetMemberJpa rs where terminology = :terminology and terminologyVersion = :version");
        query.setParameter("terminology", terminology);
        query.setParameter("version", terminologyVersion);
        deleteRecords = query.executeUpdate();
        getLog().info(
            "    attribute_value_ref_set records deleted: " + deleteRecords);

        query =
            manager
                .createQuery("DELETE From LanguageRefSetMemberJpa rs where terminology = :terminology and terminologyVersion = :version");
        query.setParameter("terminology", terminology);
        query.setParameter("version", terminologyVersion);
        deleteRecords = query.executeUpdate();
        getLog().info("    language_ref_set records deleted: " + deleteRecords);

        // Truncate Terminology Elements
        query =
            manager
                .createQuery("DELETE From DescriptionJpa d where terminology = :terminology and terminologyVersion = :version");
        query.setParameter("terminology", terminology);
        query.setParameter("version", terminologyVersion);
        deleteRecords = query.executeUpdate();
        getLog().info("    description records deleted: " + deleteRecords);
        query =
            manager
                .createQuery("DELETE From RelationshipJpa r where terminology = :terminology and terminologyVersion = :version");
        query.setParameter("terminology", terminology);
        query.setParameter("version", terminologyVersion);
        deleteRecords = query.executeUpdate();
        getLog().info("    relationship records deleted: " + deleteRecords);
        query =
            manager
                .createQuery("DELETE From ConceptJpa c where terminology = :terminology and terminologyVersion = :version");
        query.setParameter("terminology", terminology);
        query.setParameter("version", terminologyVersion);
        deleteRecords = query.executeUpdate();
        getLog().info("    concept records deleted: " + deleteRecords);

        tx.commit();

        ContentService contentService = new ContentServiceJpa();
        getLog().info("Start removing tree positions from " + terminology);
        contentService.clearTreePositions(terminology, terminologyVersion);
        contentService.close();

        getLog().info("Done ...");

      } catch (Exception e) {
        tx.rollback();
        throw e;
      }

      // Clean-up
      manager.close();
      factory.close();

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }

}
