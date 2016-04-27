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
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Goal which removes a terminology from a database.
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal remove-terminology-maps
 * 
 * @phase package
 */
public class TerminologyMapsRemoverMojo extends AbstractMojo {

  /**
   * Ref set id to remove
   * @parameter
   * @required
   */
  private String refSetId;

  /**
   * Instantiates a {@link TerminologyMapsRemoverMojo} from the specified
   * parameters.
   * 
   */
  public TerminologyMapsRemoverMojo() {
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
    getLog().info("  refsetId = " + refSetId);
    try {
      Properties config = ConfigUtility.getConfigProperties();

      // NOTE: ideally this would not use entity manager,
      // but we do not have services for all data types yet.
      EntityManagerFactory factory =
          Persistence.createEntityManagerFactory("MappingServiceDS", config);
      EntityManager manager = factory.createEntityManager();

      EntityTransaction tx = manager.getTransaction();
      try {

        // truncate all the tables that we are going to use first
        tx.begin();

        Query query =
            manager
                .createQuery("DELETE From SimpleMapRefSetMemberJpa rs where refSetId = :refSetId");
        query.setParameter("refSetId", refSetId);
        int deleteRecords = query.executeUpdate();
        getLog().info(
            "    simple_map_ref_set records deleted: " + deleteRecords);

        query =
            manager
                .createQuery("DELETE From ComplexMapRefSetMemberJpa rs where refSetId = :refSetId");
        query.setParameter("refSetId", refSetId);
        deleteRecords = query.executeUpdate();
        getLog().info(
            "    complex_map_ref_set records deleted: " + deleteRecords);

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
