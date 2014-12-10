package org.ihtsdo.otf.mapping.mojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.OtfEmailHandler;

/**
 * QA Check for Properly Numbered Map Groups
 * 
 * See admin/qa/pom.xml for a sample execution.
 * 
 * @goal qa-database
 * @phase package
 */
public class QaDatabase extends AbstractMojo {

  /**
   * The queries
   * @parameter
   * @required
   */
  private Properties queries;

  /** The manager. */
  EntityManager manager;

  /**
   * Executes the plugin.
   * 
   * @throws MojoExecutionException the mojo execution exception
   */
  @SuppressWarnings("unchecked")
  @Override
  public void execute() throws MojoExecutionException {
    getLog().info("Starting database QA");

    try {

      // Obtain an entity manager;
      ContentService service = new ContentServiceJpa() {
        {
          QaDatabase.this.manager = manager;
        }
      };

      Map<String, List<String>> errors = new HashMap<>();

      // Iterate through queries, execute and report
      for (Object property : queries.keySet()) {
        String queryStr =
            queries.getProperty(property.toString()).replace(";", "");
        getLog().info("  " + property);
        getLog().info("    " + queryStr);

        // Get and execute query (truncate any trailing semi-colon)
        Query query = manager.createNativeQuery(queryStr);
        query.setMaxResults(10);
        List<Object[]> objects = query.getResultList();

        // Expect zero count, any results are failures
        if (objects.size() > 0) {
          List<String> results = new ArrayList<>();
          for (Object[] array : objects) {
            StringBuilder sb = new StringBuilder();
            for (Object o : array) {
              sb.append(o.toString()).append(",");
            }
            results.add(sb.toString().replace(",$", ""));
          }
          errors.put(property.toString(), results);
        }

      }

      // Check for errors and report the
      if (!errors.isEmpty()) {
        StringBuilder msg = new StringBuilder();
        msg.append("\r\n");
        msg.append("The automated database QA mojo has found some issues with the following checks:\r\n");
        msg.append("\r\n");

        for (String key : errors.keySet()) {
          msg.append("  CHECK: ").append(key).append("\r\n");
          for (String result : errors.get(key)) {
            msg.append("    " + result).append("\r\n");
          }
          if (errors.get(key).size() > 9) {
            msg.append("    ...\r\n");
          }
        }

        OtfEmailHandler emailHandler = new OtfEmailHandler();
        String notificationRecipients =
            ConfigUtility.getConfigProperties().getProperty(
                "send.notification.recipients");
        if (notificationRecipients != null) {
          emailHandler.sendSimpleEmail(notificationRecipients,
              "[OTF-Mapping-Tool] Database QA Results", msg.toString());
        }
        getLog().info(msg.toString());
      } else {
        getLog().info("  NO errors");
      }

      // cleanup
      service.close();
      getLog().info("Done ...");

    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException("Performing map group QA failed.", e);
    }

  }
}