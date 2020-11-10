/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;

/**
 * Goal which updates the db to sync it with the model via JPA.
 * 
 * See admin/updatedb/pom.xml for a sample execution.
 * 
 * @goal updatedb
 * 
 * @phase package
 */
public class UpdateDbMojo extends AbstractOtfMappingMojo {

  /**
   * Instantiates a {@link UpdateDbMojo} from the specified parameters.
   * 
   */
  public UpdateDbMojo() {
    // do nothing
  }

  /* see superclass */
  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Start updating database schema");

    setupBindInfoPackage();

   try {
       // Trigger a JPA event
      new ContentServiceJpa().close();
      getLog().info("done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }

  }

}
