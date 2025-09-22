/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.helpers.TerminologyVersion;
import org.ihtsdo.otf.mapping.helpers.TerminologyVersionList;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.AmazonS3Service;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * JPA implementation of the {@link AmazonS3Service}.
 */
public class AmazonS3ServiceJpa extends RootServiceJpa
    implements AmazonS3Service {

  private S3Client s3Client;

  /**
   * Instantiates an empty {@link AmazonS3ServiceJpa}.
   * 
   * @throws Exception the exception
   */
  public AmazonS3ServiceJpa() throws Exception {
    super();
    initializeS3Client();
  }

  /**
   * Initialize the S3 client.
   */
  private void initializeS3Client() throws Exception {
    // Example using basic credentials
    final AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
        ConfigUtility.getConfigProperties().getProperty("aws.access.key.id"),
        ConfigUtility.getConfigProperties().getProperty("aws.secret.access.key"));
    s3Client = S3Client.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
        .build();

    // Example using instance profile credentials
    // s3Client = S3Client.builder()
    // .region(Region.US_EAST_1)
    // .credentialsProvider(InstanceProfileCredentialsProvider.create())
    // .build();
  }

  /**
   * Close the manager when done with this service.
   * 
   * @throws Exception the exception
   */
  @Override
  public void close() throws Exception {
    if (manager.isOpen()) {
      manager.close();
    }
    if (s3Client != null) {
      s3Client.close();
    }
  }

  /**
   * Gets an S3Client object based first on InstanceProfileCredentialsProvider.
   * If not available, will then use StaticCredentialsProvider.
   * 
   * @return S3Client AWS S3 client
   * @throws Exception the exception
   */
  public static S3Client connectToAmazonS3() throws Exception {
    // Connect to server using instance profile credentials
    S3Client s3Client = S3Client.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(InstanceProfileCredentialsProvider.create())
        .build();

    // Check if connection was successful. If not, try to connect with static
    // keys instead
    try {
      s3Client.listBuckets();
    } catch (final Exception e) {
      // Connect to server with static keys
      final AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
          ConfigUtility.getConfigProperties().getProperty("aws.access.key.id"),
          ConfigUtility.getConfigProperties().getProperty("aws.secret.access.key"));
      s3Client = S3Client.builder()
          .region(Region.US_EAST_1)
          .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
          .build();

      // Check connection again. If this fails as well, it will throw the
      // exception to the calling method
      s3Client.listBuckets();
    }

    return s3Client;
  }

  /* see superclass */
  @Override
  public SearchResultList getFileListFromAmazonS3(final MapProject mapProject)
    throws Exception {

    String sourceTerminology = mapProject.getSourceTerminology();
    String destinationTerminology = mapProject.getDestinationTerminology();

    // determine filter that will be used on s3 file list
    // TODO: for SNOMEDCT_US decide between Edition and Extension files for
    // filtering
    String filterTerminology = "";
    if (destinationTerminology.startsWith("GMDN")) {
      filterTerminology = "GMDN";
    } else if (destinationTerminology.startsWith("ICPC")) {
      filterTerminology = "SnomedCT_GPFPICPC2";
    } else if (destinationTerminology.contains("ICNP")) {
      // There are two projects that use ICNP as their destination
      // terminology. Distinguish by refsetId.
      if (mapProject.getRefSetId().equals("711112009")) {
        filterTerminology = "SnomedCT_ICNPDiagnoses";
      } else if (mapProject.getRefSetId().equals("712505008")) {
        filterTerminology = "SnomedCT_ICNPInterventions";
      } else {
        throw new Exception(
            " unhandled ICNP refset Id= " + mapProject.getRefSetId());
      }
    } else if (removeSpaces(sourceTerminology).equals("SNOMEDCT")) {
      filterTerminology = "InternationalRF2";
    } else if (sourceTerminology.startsWith("ICNP")) {
      filterTerminology = sourceTerminology.substring(0, sourceTerminology.indexOf(" "))
              + sourceTerminology.substring(sourceTerminology.indexOf(" ") + 1);
    }

    int year = Calendar.getInstance().get(Calendar.YEAR);
    String currentYear = Integer.toString(year);
    String nextYear = Integer.toString(year + 1);
    String lastYear = Integer.toString(year - 1);

    // Determine international or U.S.
    String nationalPrefix = sourceTerminology.equals("SNOMEDCT_US") ? "us" : "international";

    String bucketName = "release-ihtsdo-prod-published";
    SearchResultList searchResults = new SearchResultListJpa();

    // Connect to server
    final S3Client s3Client = AmazonS3ServiceJpa.connectToAmazonS3();

    // Get full list of files on aws s3
    final List<S3Object> fullKeyList = new ArrayList<>();

    ListObjectsV2Request listObjectsReqManual = ListObjectsV2Request.builder()
        .bucket(bucketName)
        .prefix(nationalPrefix)
        .build();

    ListObjectsV2Response objects = s3Client.listObjectsV2(listObjectsReqManual);
    fullKeyList.addAll(objects.contents());

    while (objects.isTruncated()) {
      listObjectsReqManual = ListObjectsV2Request.builder()
          .bucket(bucketName)
          .prefix(nationalPrefix)
          .continuationToken(objects.nextContinuationToken())
          .build();
      objects = s3Client.listObjectsV2(listObjectsReqManual);
      fullKeyList.addAll(objects.contents());
    }

    // start filtering full list, to keep only relevant zip files
    final TerminologyVersionList returnList = new TerminologyVersionList();
    for (final S3Object obj : fullKeyList) {
      if (obj.key().endsWith("zip")
          && obj.key().contains(filterTerminology)
          && !obj.key().contains("published_build_backup")
          && (obj.key().contains(lastYear)
              || obj.key().contains(currentYear)
              || obj.key().contains(nextYear))
          && (obj.key().matches(".*\\d.zip")
              || obj.key().matches(".*\\dZ.zip"))) {
        final TerminologyVersion tv = new TerminologyVersion(
            obj.key().replace(".zip", "").replace(nationalPrefix + '/', ""),
            filterTerminology);
        tv.identifyScope();
        returnList.addTerminologyVersion(tv);
      }
    }

	// Remove all duplicates defined by term-version-scope but send out
	// notifications so people can address
    Map<String, Set<TerminologyVersion>> dups = returnList.removeDups();
    sendDuplicateVersionNotification(dups);

    String mostRecentAlphaBeta = "";

    // Create search result for each file
    for (TerminologyVersion tv : returnList.getTerminologyVersionList()) {
      SearchResult result = new SearchResultJpa();
      if (tv.getAwsZipFileName().toLowerCase().contains("alpha")) {
        result.setTerminology("ALPHA");
        if (tv.getVersion().compareTo(mostRecentAlphaBeta) > 0) {
          mostRecentAlphaBeta = tv.getVersion();
        }
      } else if (tv.getAwsZipFileName().toLowerCase().contains("beta")) {
        result.setTerminology("BETA");
        if (tv.getVersion().compareTo(mostRecentAlphaBeta) > 0) {
          mostRecentAlphaBeta = tv.getVersion();
        }
      } else if (tv.getAwsZipFileName().toLowerCase().contains("member")) {
        result.setTerminology("MEMBER");
        if (tv.getVersion().compareTo(mostRecentAlphaBeta) > 0) {
          mostRecentAlphaBeta = tv.getVersion();
        }
      } else {
        result.setTerminology("FINAL");
      }

      result.setTerminologyVersion(tv.getVersion());
      result = constructAwsFileFromZipInfo(mapProject, result, tv, "Snapshot",
          nationalPrefix, fullKeyList, filterTerminology);
      // confirm file exists on aws before adding to result list
      for (final S3Object obj : fullKeyList) {
        if (obj.key().equals(result.getValue2())) {
          searchResults.addSearchResult(result);
        }
      }

      // add delta files if looking at international release
      if (filterTerminology.equals("InternationalRF2")) {
        SearchResult deltaResult = new SearchResultJpa();
        deltaResult.setTerminology(result.getTerminology());
        deltaResult.setTerminologyVersion(result.getTerminologyVersion());
        deltaResult = constructAwsFileFromZipInfo(mapProject, deltaResult, tv,
            "Delta", nationalPrefix, fullKeyList, filterTerminology);
        for (final S3Object obj : fullKeyList) {
          if (obj.key().equals(deltaResult.getValue2())) {
            searchResults.addSearchResult(deltaResult);
          }
        }
      }
    }

    // sort files by release date
    searchResults.sortBy(new Comparator<SearchResult>() {
      @Override
      public int compare(SearchResult o1, SearchResult o2) {
        String releaseDate1 = o1.getTerminologyVersion();
        String releaseDate2 = o2.getTerminologyVersion();
        return releaseDate2.compareTo(releaseDate1);
      }
    });

    // only keep most recent alpha/betas
    SearchResultList resultsToKeep = new SearchResultListJpa();
    for (SearchResult result : searchResults.getSearchResults()) {
      if (result.getTerminology().equals("FINAL")
          || result.getTerminologyVersion().equals(mostRecentAlphaBeta)) {
        resultsToKeep.addSearchResult(result);
      }
    }

    return resultsToKeep;

  }

  /**
   * 
   * @param string
   * @return
   */
  private String removeSpaces(String string) {
    if (string != null)
      return string.replace(" ", "").trim();
    else
      return null;
  }

  /**
   *  use the zip file to figure out full file path for access on aws
   *
   * @param mapProject the map project
   * @param result the result
   * @param tv the tv
   * @param type the type
   * @param nationalPrefix the national prefix
   * @param fullKeyList the full key list
   * @param filterTerminology the filter terminology
   * @return the search result
   */  
  private SearchResult constructAwsFileFromZipInfo(final MapProject mapProject,
      final SearchResult result, final TerminologyVersion tv, final String type,
      final String nationalPrefix, final List<S3Object> fullKeyList,
      final String filterTerminology) {

    if (filterTerminology.equals("InternationalRF2")) {
      if (mapProject.getMapRefsetPattern()
          .equals(MapRefsetPattern.ExtendedMap)) {
        result
            .setValue(((result.getTerminology().equals("FINAL") || result.getTerminology().equals("MEMBER")) ? "" : "x")
            + "der2_iisssccRefset_ExtendedMap" + type + "_INT_"
            + tv.getVersion() + ".txt");
        result.setValue2(nationalPrefix + '/' + tv.getAwsZipFileName()
            + ((result.getTerminology().equals("FINAL") || result.getTerminology().equals("MEMBER"))
                ? '/' + tv.getAwsZipFileName() : "")
            + "/" + type + "/Refset/Map/"
            + ((result.getTerminology().equals("FINAL") || result.getTerminology().equals("MEMBER")) ? "" : "x")
            + "der2_iisssccRefset_ExtendedMap" + type + "_INT_"
            + tv.getVersion() + ".txt");
      } else if (mapProject.getMapRefsetPattern()
          .equals(MapRefsetPattern.SimpleMap)) {
        result
            .setValue(((result.getTerminology().equals("FINAL") || result.getTerminology().equals("MEMBER")) ? "" : "x")
            + "der2_sRefset_SimpleMap" + type + "_INT_" + tv.getVersion()
            + ".txt");
        result.setValue2(nationalPrefix + '/' + tv.getAwsZipFileName()
            + ((result.getTerminology().equals("FINAL") || result.getTerminology().equals("MEMBER"))
                ? '/' + tv.getAwsZipFileName()
                : "")
            + "/" + type + "/Refset/Map/"
            + ((result.getTerminology().equals("FINAL") || result.getTerminology().equals("MEMBER")) ? "" : "x")
            + "der2_sRefset_SimpleMap" + type + "_INT_" + tv.getVersion()
            + ".txt");
      }
      // special processing for other projects bc the zip file has a different
      // version date from the file name
    } else {
      for (final S3Object obj : fullKeyList) {
        if (obj.key()
            .startsWith(nationalPrefix + '/' + tv.getAwsZipFileName() + '/'
                + tv.getAwsZipFileName() + '/' + type + "/Refset/Map/")) {
          result.setValue(
              obj.key().substring(obj.key().lastIndexOf('/') + 1));
          result.setValue2(obj.key());
          return result;
        }
      }
    }
    return result;
  }

	/**
	 * If duplicate terminology-version pairs found on S3, send email
	 * notification.
	 *
	 * @param dups
	 *            Map of duplicate terminologies to each duplicate
	 * @throws Exception
	 *             the exception
	 */
	private void sendDuplicateVersionNotification(Map<String, Set<TerminologyVersion>> dups) throws Exception {
		Properties config = ConfigUtility.getConfigProperties();

		if (!dups.isEmpty()) {
			// Define recipients
			String notificationRecipients = config.getProperty("send.notification.recipients.devops");

			if (!notificationRecipients.isEmpty() && "true".equals(config.getProperty("mail.enabled"))) {
				Logger.getLogger(AmazonS3ServiceJpa.class)
						.info("Identified " + dups.size() + " sets of duplicate terminologies.  Sending email");

				// Define sender
				String sender;
				if (config.containsKey("mail.smtp.from")) {
					sender = config.getProperty("mail.smtp.from");
				} else {
					sender = config.getProperty("mail.smtp.user");
				}

				// Define email properties
				Properties props = new Properties();
				props.put("mail.smtp.user", config.getProperty("mail.smtp.user"));
				props.put("mail.smtp.password", config.getProperty("mail.smtp.password"));
				props.put("mail.smtp.host", config.getProperty("mail.smtp.host"));
				props.put("mail.smtp.port", config.getProperty("mail.smtp.port"));
				props.put("mail.smtp.starttls.enable", config.getProperty("mail.smtp.starttls.enable"));
				props.put("mail.smtp.auth", config.getProperty("mail.smtp.auth"));

				// Create Message Body
				StringBuffer messageBody = new StringBuffer();
				int counter = 1;
				for (String triplet : dups.keySet()) {
					Set<TerminologyVersion> termVers = dups.get(triplet);
					TerminologyVersion tvForPrintout = termVers.iterator().next();

					messageBody.append("Warning: Duplicate terminology-version pairs found on AWS");
					messageBody.append(System.getProperty("line.separator"));
					messageBody.append(System.getProperty("line.separator"));
					messageBody.append("DUPLICATE #" + counter++);
					messageBody.append(System.getProperty("line.separator"));
					messageBody.append("TERMINOLOGY: " + tvForPrintout.getTerminology());
					messageBody.append(System.getProperty("line.separator"));
					messageBody.append("VERSION: " + tvForPrintout.getVersion());
					messageBody.append(System.getProperty("line.separator"));
					if (tvForPrintout.getScope() != null) {
						messageBody.append("For Scope: " + tvForPrintout.getScope());
						messageBody.append(System.getProperty("line.separator"));
					}

					messageBody.append(System.getProperty("line.separator"));

					int fileCounter = 1;
					for (TerminologyVersion tv : termVers) {
						messageBody.append("\tAWS FILE #" + fileCounter++ + ": " + tv.getAwsZipFileName());
						messageBody.append(System.getProperty("line.separator"));
					}
					messageBody.append(System.getProperty("line.separator"));
					messageBody.append(System.getProperty("line.separator"));
				}

				ConfigUtility.sendEmail("IHTSDO Mapping Tool Duplicate Terminologies Warning", sender,
						notificationRecipients, messageBody.toString(), props,
						"true".equals(config.getProperty("mail.smtp.auth")));
			}
		}
	}
}
