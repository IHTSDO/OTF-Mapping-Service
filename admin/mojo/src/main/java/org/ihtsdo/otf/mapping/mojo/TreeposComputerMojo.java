package org.ihtsdo.otf.mapping.mojo;

import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.ihtsdo.otf.mapping.helpers.ValidationResultJpa;
import org.ihtsdo.otf.mapping.jpa.services.ContentServiceJpa;
import org.ihtsdo.otf.mapping.jpa.services.MetadataServiceJpa;
import org.ihtsdo.otf.mapping.services.ContentService;
import org.ihtsdo.otf.mapping.services.MetadataService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

/**
 * Goal which loads an RF2 Snapshot of SNOMED CT data into a database.
 * 
 * See admin/remover/pom.xml for a sample execution.
 * 
 * @goal compute-treepos
 * 
 * @phase package
 */
public class TreeposComputerMojo extends AbstractMojo {

  /**
   * Name of terminology to be loaded.
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
   * A comma-separated list of the root ids
   * @parameter
   * @requried
   */
  private String rootIds;

  /**
   * Whether to send email notifications of any errors (default: false)
   * @parameter
   */
  private boolean sendNotification = false;

  /**
   * Instantiates a {@link TreeposComputerMojo} from the specified parameters.
   * 
   */
  public TreeposComputerMojo() {
    // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.maven.plugin.Mojo#execute()
   */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting computing tree positions");
    getLog().info("  terminology = " + terminology);
    getLog().info("  terminologyVersion = " + terminologyVersion);
    getLog().info("  rootIds = " + rootIds);
    getLog().info("  sendNotification = " + sendNotification);

    // check notification parameter requirements
    Properties config;
    try {
      config = ConfigUtility.getConfigProperties();
    } catch (Exception e1) {
      throw new MojoFailureException(
          "Could not retrieve parameters from conf file");
    }
    String notificationRecipients =
        config.getProperty("send.notification.recipients");
    if (!sendNotification) {
      getLog().info(
          "No notifications will be sent as a result of workflow computation.");
    }
    if (sendNotification
        && config.getProperty("send.notification.recipients") == null) {
      throw new MojoFailureException(
          "Email notification was requested, but no recipients were specified.");
    } else {
      getLog().info(
          "Request to send notification email for any errors to recipients: "
              + notificationRecipients);
    }

    try {

      // creating tree positions
      // first get isaRelType from metadata
      final MetadataService metadataService = new MetadataServiceJpa();
      Map<String, String> hierRelTypeMap =
          metadataService.getHierarchicalRelationshipTypes(terminology,
              terminologyVersion);
      String isaRelType = hierRelTypeMap.keySet().iterator().next().toString();

      getLog().info("Start creating tree positions.");
      metadataService.close();
      
      final ContentService contentService = new ContentServiceJpa();
      // Walk up tree to the root
      // ASSUMPTION: single root
      ValidationResult results = new ValidationResultJpa();
      for (String rootId : rootIds.split(",")) {
        getLog().info(
            "  Compute tree from rootId " + rootId + ", " + isaRelType);
        ValidationResult result =
            contentService.computeTreePositions(terminology,
                terminologyVersion, isaRelType, rootId);
        results.merge(result);
      }
      contentService.close();

      if (!results.isValid()) {
        ConfigUtility
            .sendValidationResultEmail(
                notificationRecipients,
                "OTF-Mapping-Tool:  Errors in computing " + terminology + ", "
                    + terminologyVersion + " hierarchical tree positions",
                "Hello,\n\nErrors were detected when computing hierarchical tree positions for "
                    + terminology + ", " + terminologyVersion, results);
      }

      getLog().info("Done ...");
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }
}
