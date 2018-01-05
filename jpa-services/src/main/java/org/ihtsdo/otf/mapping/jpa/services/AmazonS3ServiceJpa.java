/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.jpa.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

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
      filterTerminology =
          sourceTerminology.substring(0, sourceTerminology.indexOf(" "))
              + sourceTerminology.substring(sourceTerminology.indexOf(" ") + 1);
    }

    int year = Calendar.getInstance().get(Calendar.YEAR);
    String currentYear = Integer.toString(year);
    String nextYear = Integer.toString(year + 1);
    String lastYear = Integer.toString(year - 1);

    // Determine international or U.S.
    String nationalPrefix =
        sourceTerminology.equals("SNOMEDCT_US") ? "us" : "international";

    String bucketName = "release-ihtsdo-prod-published";
    SearchResultList searchResults = new SearchResultListJpa();

    // Connect to server
    AmazonS3 s3Client = AmazonS3ServiceJpa.connectToAmazonS3();

    // Get full list of files on aws s3
    List<S3ObjectSummary> fullKeyList = new ArrayList<S3ObjectSummary>();

    ObjectListing objects = s3Client.listObjects(bucketName, nationalPrefix);
    fullKeyList = objects.getObjectSummaries();
    objects = s3Client.listNextBatchOfObjects(objects);

    while (objects.isTruncated()) {
      fullKeyList.addAll(objects.getObjectSummaries());
      objects = s3Client.listNextBatchOfObjects(objects);
    }

    fullKeyList.addAll(objects.getObjectSummaries());

    // start filtering full list, to keep only relevant zip files
    TerminologyVersionList returnList = new TerminologyVersionList();
    for (S3ObjectSummary obj : fullKeyList) {
      if (obj.getKey().endsWith("zip")
          && obj.getKey().contains(filterTerminology)
          && !obj.getKey().contains("published_build_backup")
          && (obj.getKey().contains(lastYear)
              || obj.getKey().contains(currentYear)
              || obj.getKey().contains(nextYear))
          && (obj.getKey().matches(".*\\d.zip")
              || obj.getKey().matches(".*\\dZ.zip"))) {
        TerminologyVersion tv = new TerminologyVersion(
            obj.getKey().replace(".zip", "").replace(nationalPrefix + '/', ""),
            filterTerminology);
        tv.identifyScope();
        returnList.addTerminologyVersion(tv);
      }
    }

    // Remove all duplicates defined by term-version-scope
    returnList.removeDups();

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
      } else {
        result.setTerminology("FINAL");
      }

      result.setTerminologyVersion(tv.getVersion());
      result = constructAwsFileFromZipInfo(mapProject, result, tv, "Snapshot",
          nationalPrefix, fullKeyList, filterTerminology);
      // confirm file exists on aws before adding to result list
      for (S3ObjectSummary obj : fullKeyList) {
        if (obj.getKey().equals(result.getValue2())) {
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
        for (S3ObjectSummary obj : fullKeyList) {
          if (obj.getKey().equals(deltaResult.getValue2())) {
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
  private SearchResult constructAwsFileFromZipInfo(MapProject mapProject,
    SearchResult result, TerminologyVersion tv, String type,
    String nationalPrefix, List<S3ObjectSummary> fullKeyList,
    String filterTerminology) {

    if (filterTerminology.equals("InternationalRF2")) {
      if (mapProject.getMapRefsetPattern()
          .equals(MapRefsetPattern.ExtendedMap)) {
        result.setValue((result.getTerminology().equals("FINAL") ? "" : "x")
            + "der2_iisssccRefset_ExtendedMap" + type + "_INT_"
            + tv.getVersion() + ".txt");
        result.setValue2(nationalPrefix + '/' + tv.getAwsZipFileName()
            + (result.getTerminology().equals("FINAL")
                ? '/' + tv.getAwsZipFileName() : "")
            + "/" + type + "/Refset/Map/"
            + (result.getTerminology().equals("FINAL") ? "" : "x")
            + "der2_iisssccRefset_ExtendedMap" + type + "_INT_"
            + tv.getVersion() + ".txt");
      } else if (mapProject.getMapRefsetPattern()
          .equals(MapRefsetPattern.SimpleMap)) {
        result.setValue((result.getTerminology().equals("FINAL") ? "" : "x")
            + "der2_sRefset_SimpleMap" + type + "_INT_" + tv.getVersion()
            + ".txt");
        result.setValue2(nationalPrefix + '/' + tv.getAwsZipFileName()
            + (result.getTerminology().equals("FINAL")
                ? '/' + tv.getAwsZipFileName() : "")
            + "/" + type + "/Refset/Map/"
            + (result.getTerminology().equals("FINAL") ? "" : "x")
            + "der2_sRefset_SimpleMap" + type + "_INT_" + tv.getVersion()
            + ".txt");
      }
      // special processing for other projects bc the zip file has a different
      // version date from the file name
    } else {
      for (S3ObjectSummary obj : fullKeyList) {
        if (obj.getKey()
            .startsWith(nationalPrefix + '/' + tv.getAwsZipFileName() + '/'
                + tv.getAwsZipFileName() + '/' + type + "/Refset/Map/")) {
          result.setValue(
              obj.getKey().substring(obj.getKey().lastIndexOf('/') + 1));
          result.setValue2(obj.getKey());
          return result;
        }
      }
    }
    return result;
  }
}
