/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.services;

import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.SearchResult;
import org.ihtsdo.otf.mapping.helpers.SearchResultJpa;
import org.ihtsdo.otf.mapping.helpers.SearchResultList;
import org.ihtsdo.otf.mapping.helpers.SearchResultListJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.services.AmazonS3Service;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * JPA implementation of the {@link AmazonS3Service}.
 */
public class AmazonS3ServiceJpa extends RootServiceJpa
    implements AmazonS3Service {

  /**
   * Instantiates an empty {@link AmazonS3ServiceJpa}.
   * 
   * @throws Exception the exception
   */
  public AmazonS3ServiceJpa() throws Exception {
    super();
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
  }

  /**
   * Gets an AmazonS3 object based first on InstanceProfileCredentialsProvider.
   * If not available, will then use AWSStaticCredentialsProvider.
   * 
   * @return AmazonS3 AWS S3 client
   * @throws Exception the exception
   */
  public static AmazonS3 connectToAmazonS3() throws Exception {
    // Connect to server using instance profile credentials
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
        .withRegion(Regions.US_EAST_1)
        .withCredentials(new InstanceProfileCredentialsProvider(false)).build();

    // Check if connection was successful. If not, try to connect with static
    // keys instead
    try {
      s3Client.listBuckets();
    } catch (SdkClientException e) {
      // Connect to server with static keys
      BasicAWSCredentials awsCreds = new BasicAWSCredentials(
          ConfigUtility.getConfigProperties().getProperty("aws.access.key.id"),
          ConfigUtility.getConfigProperties()
              .getProperty("aws.secret.access.key"));
      s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1)
          .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

      // Check connection again. If this fails as well, it will throw the
      // exception to the calling method
      s3Client.listBuckets();
    }

    return s3Client;
  }

  /* see superclass */
  public SearchResultList getFileListFromAmazonS3(MapProject mapProject)
    throws Exception {

    String sourceTerminology = mapProject.getSourceTerminology();
    String destinationTerminology = mapProject.getDestinationTerminology();

    String bucketName = "release-ihtsdo-prod-published";
    SearchResultList searchResults = new SearchResultListJpa();

    // Connect to server
    AmazonS3 s3Client = connectToAmazonS3();

    // Verify Buckets Exists
    if (!s3Client.doesBucketExist(bucketName)) {
      throw new Exception("Cannot find Bucket Name");
    } else {
      Logger.getLogger(getClass()).info("Bucket " + bucketName + " accessed.");
    }

    // Determine international or U.S.
    String nationalPrefix =
        sourceTerminology.equals("SNOMEDCT_US") ? "us" : "international";

    // Determine year
    int year = Calendar.getInstance().get(Calendar.YEAR);
    String lastYear = Integer.toString(year - 1);
    // Only keep alpha and beta from most recent version
    String mostRecentAlphaBeta = "";
    // Map to ensure only latest version of a file is included in list
    Map<String, String> shortNameToFullDateMap = new HashMap<>();

    // TODO: Note: Logic here should be moved into a ProjectSpecificHandler
    if (mapProject.getDestinationTerminology().equals("ICPC")
        || mapProject.getDestinationTerminology().equals("GMDN")
        || mapProject.getDestinationTerminology().equals("ICNP")) {
      // List Files on Bucket "release-ihtsdo-prod-published"
      ObjectListing listing = null;
      if (mapProject.getDestinationTerminology().equals("ICPC")) {
        listing = s3Client.listObjects(bucketName,
            nationalPrefix + "/SnomedCT_GPFPICPC2");
      } else if (mapProject.getDestinationTerminology().equals("GMDN")) {
        listing =
            s3Client.listObjects(bucketName, nationalPrefix + "/SnomedCT_GMDN");
      } else if (mapProject.getDestinationTerminology().equals("ICNP")) {
        // There are two projects that use ICNP as their destination
        // terminology. Distinguish by refsetId.
        if (mapProject.getRefSetId().equals("711112009")) {
          listing = s3Client.listObjects(bucketName,
              nationalPrefix + "/SnomedCT_ICNPDiagnoses");
        } else if (mapProject.getRefSetId().equals("712505008")) {
          listing = s3Client.listObjects(bucketName,
              nationalPrefix + "/SnomedCT_ICNPInterventions");
        } else {
          throw new Exception(
              " unhandled ICNP refset Id= " + mapProject.getRefSetId());
        }
      } else {
        throw new Exception(" list S3 files for terminology= "
            + mapProject.getDestinationTerminology());
      }
      List<S3ObjectSummary> summaries = listing.getObjectSummaries();

      int i = 1;
      for (S3ObjectSummary sum : summaries) {
        String fileName = sum.getKey();
        Matcher m = Pattern.compile("[0-9T]{15}").matcher(fileName);
        String fileYear = "";
        String fileDate = "";
        String fullFileDate = "";
        while (m.find()) {
          fullFileDate = m.group();
          fileDate = fullFileDate.substring(0, 8);
          fileYear = fileDate.substring(0, 4);
        }
        if (fileYear.compareTo(lastYear) < 0) {
          continue;
        }
        if ((fileName.contains("ICPC2ExtendedMap")
            || fileName.contains("GMDNMapSimpleMap")
            || fileName.contains("ICNPSimpleMap")) && !fileName.contains("Full")
            && !fileName.contains("backup") && !fileName.contains("Delta")) {
          Logger.getLogger(getClass())
              .info(mapProject.getDestinationTerminology() + " Summary #" + i++
                  + " with: " + sum.getKey());
          SearchResult result = new SearchResultJpa();
          String shortName = fileName.substring(fileName.lastIndexOf('/'));
          result.setTerminology("FINAL");
          result.setTerminologyVersion(fileDate);
          result.setValue(shortName);
          result.setValue2(fileName);
          searchResults.addSearchResult(result);
        }
      }
    } else {

      // List All Files on Bucket "release-ihtsdo-prod-published"
      ObjectListing listing =
          s3Client.listObjects(bucketName, nationalPrefix + "/");
      List<S3ObjectSummary> summaries = listing.getObjectSummaries();
      int j = 0;
      int i = 1;
      Logger.getLogger(getClass())
          .info("Destination terminology *" + destinationTerminology + "*");
      while (listing.isTruncated()) {
        listing = s3Client.listNextBatchOfObjects(listing);
        summaries = listing.getObjectSummaries();

        for (S3ObjectSummary sum : summaries) {
          String fileName = sum.getKey();
          Matcher m = Pattern.compile("[0-9T]{15}").matcher(fileName);
          String fileYear = "";
          String fileDate = "";
          String fullFileDate = "";
          while (m.find()) {
            fullFileDate = m.group();
            fileDate = fullFileDate.substring(0, 8);
            fileYear = fileDate.substring(0, 4);
          }
          // last year okay, but not before that
          if (fileYear.compareTo(lastYear) < 0) {
            continue;
          }
          if (((mapProject.getName().contains("ICD10")
              && fileName.contains("ExtendedMap"))
              || (mapProject.getName().contains("ICDO")
                  && fileName.contains("SimpleMap")))
              && !fileName.contains("Full") && !fileName.contains("backup")
              && !fileName.contains("LOINC") && !fileName.contains("MRCM")
              && !fileName.contains("Starter") && !fileName.contains("Nursing")
              && !fileName.contains("Odontogram")
              && !fileName.contains("WithoutRT") && !fileName.contains("ButOld")
              && !fileName.contains("UPDATED") && !fileName.contains("ICNP")
              && !fileName.contains("SnomedCT_RF2Release")) {
            Logger.getLogger(getClass())
                .info("Summary #" + i++ + " with: " + sum.getKey());
            SearchResult result = new SearchResultJpa();
            String shortName = fileName.substring(fileName.lastIndexOf('/'));
            if (shortNameToFullDateMap.containsKey(shortName)) {
              String savedFullFileDate = shortNameToFullDateMap.get(shortName);
              // if current one is later, save this one into the map
              if (savedFullFileDate.compareTo(fullFileDate) < 0) {
                shortNameToFullDateMap.put(shortName, fullFileDate);
              }
            } else {
              shortNameToFullDateMap.put(shortName, fullFileDate);
            }
            if (fileName.toLowerCase().contains("alpha")) {
              result.setTerminology("ALPHA");
              if (fileDate.compareTo(mostRecentAlphaBeta) > 0) {
                mostRecentAlphaBeta = fileDate;
              }
            } else if (fileName.toLowerCase().contains("beta")) {
              result.setTerminology("BETA");
              if (fileDate.compareTo(mostRecentAlphaBeta) > 0) {
                mostRecentAlphaBeta = fileDate;
              }
            } else {
              result.setTerminology("FINAL");
            }

            result.setTerminologyVersion(fileDate);
            result.setValue(shortName);
            result.setValue2(fileName);
            searchResults.addSearchResult(result);
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

    // only keep most recent finals and most recent alpha/betas
    SearchResultList resultsToKeep = new SearchResultListJpa();
    for (SearchResult result : searchResults.getSearchResults()) {
      // must be final or the most current release alpha/beta
      if (result.getTerminology().equals("FINAL")
          || result.getTerminologyVersion().equals(mostRecentAlphaBeta)) {
        // also must be the most recent version of the release file
        if (shortNameToFullDateMap.containsKey(result.getValue())) {
          if (result.getValue2()
              .contains(shortNameToFullDateMap.get(result.getValue()))) {
            resultsToKeep.addSearchResult(result);
          }
        } else {
          resultsToKeep.addSearchResult(result);
        }
      }
    }

    return resultsToKeep;
  }

}
