package org.ihtsdo.otf.mapping.helpers;

/**
 * Generically represents a terminology versions as found in AWS.
 *
 * @author ${author}
 */
public class TerminologyVersion {

  /** The terminology. */
  private String terminology;

  /** The version. */
  private String version;

  /** The version. */
  private String scope;

  /** The version. */
  private String awsZipFilname;

  /**
   * Instantiates an empty {@link KeyValuePair}.
   */
  public TerminologyVersion() {
    // do nothing
  }

  /**
   * Instantiates a {@link KeyValuePair} from the specified parameters.
   *
   * @param terminology the terminology
   * @param version the version
   * @param scope the scope
   * @param awsZipFilname the aws zip filname
   */
  public TerminologyVersion(String terminology, String version, String scope,
      String awsZipFilname) {
    this.terminology = terminology;
    this.version = version;
    this.scope = scope;
    this.awsZipFilname = awsZipFilname;
  }

  /**
   * Instantiates a {@link TerminologyVersion} from the specified parameters.
   *
   * @param awsKey the aws key
   * @param terminology the terminology
   */
  public TerminologyVersion(String awsKey, String terminology) {
    System.out.println(awsKey);
    this.terminology = terminology;
    awsZipFilname = awsKey;

    // Get Year
    int idx = awsKey.indexOf("_20");
    version = awsKey.substring(idx + 1, idx + 9);
  }

  /**
   * Returns the terminology.
   *
   * @return the terminology
   */
  public String getTerminology() {
    return terminology;
  }

  /**
   * Sets the terminology.
   *
   * @param terminology the terminology
   */
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /**
   * Returns the version.
   *
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version.
   *
   * @param version the version
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Returns the scope.
   *
   * @return the scope
   */
  public String getScope() {
    return scope;
  }

  /**
   * Sets the scope.
   *
   * @param scope the scope
   */
  public void setScope(String scope) {
    this.scope = scope;
  }

  /**
   * Returns the aws zip filname.
   *
   * @return the aws zip filname
   */
  public String getAwsZipFilname() {
    return awsZipFilname;
  }

  /**
   * Sets the aws zip filname.
   *
   * @param awsZipFilname the aws zip filname
   */
  public void setAwsZipFilname(String awsZipFilname) {
    this.awsZipFilname = awsZipFilname;
  }

  /**
   * Identify scope.
   */
  public void identifyScope() {
    // Get Scope
    if (awsZipFilname.toLowerCase().contains("alpha")) {
      scope = "Alpha";
    } else if (awsZipFilname.toLowerCase().contains("beta")) {
      scope = "Beta";
    } else {
      scope = "Production";
    }
  }
}
