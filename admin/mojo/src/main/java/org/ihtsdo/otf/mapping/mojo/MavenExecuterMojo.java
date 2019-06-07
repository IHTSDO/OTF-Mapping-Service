/*
 * Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.mojo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mojo for initializing a map application from configuration files.
 * 
 * See admin/loader/pom.xml for a sample execution.
 * 
 * @goal maven-mojo-executor
 */
public class MavenExecuterMojo extends AbstractMojo {

  /**
   * Maven Mojo Config file
   * 
   * @parameter
   * @required
   */
  private String mavenExecuterConfigFile = null;

  /**
   * 
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  public void execute() throws MojoExecutionException, MojoFailureException {

    Properties config;

    try {
      config = ConfigUtility.getConfigProperties();
    } catch (Exception e) {
      Logger.getLogger(getClass()).error("Error ", e);
      throw new MojoExecutionException(
          "Mojo failed get configuration properties.", e);
    }

    if (config.getProperty("data.dir") == null) {
      throw new MojoExecutionException(
          "Config file must specify a data.dir property");
    }

    ObjectMapper mapper = new ObjectMapper();

    try {
      MojoConfig[] steps = mapper.readValue(new File(mavenExecuterConfigFile),
          MojoConfig[].class);

      InvocationRequest request;
      InvocationResult result = null;
      Invoker invoker = new DefaultInvoker();
      Properties p;

      for (MojoConfig step : steps) {
        Logger.getLogger(getClass()).info(step.getLog());

        Logger.getLogger(getClass()).info(" json:"
            + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(step));

        request = new DefaultInvocationRequest();

        Logger.getLogger(getClass()).info(System.getProperty("user.dir"));
        if (Files.exists(Paths.get(step.getPomFile())))
          request.setPomFile(new File(step.getPomFile()));
        else
          throw new IllegalArgumentException(String.format(
              "POM file s% does not exist.  Check config file and system.",
              step.getPomFile()));

        if (step.getProfile() != null && !step.getProfile().isEmpty())
          request.setProfiles(step.getProfile());
        else
          throw new IllegalArgumentException(
              "Configuration step does not have one or more profiles.");

        if (step.getGoals() != null && !step.getGoals().isEmpty())
          request.setGoals(step.getGoals());
        else
          throw new IllegalArgumentException(
              "Configuration step does not have one or more goals.");

        p = new Properties();
        // System properties
        if (step.getSystemProperties() != null) {
          for (Property prop : step.getSystemProperties()) {
            p.setProperty(prop.getName(), System.getProperty(prop.getValue())
                + ((prop.getSuffix() != null) ? prop.getSuffix() : ""));
            Logger.getLogger(getClass())
                .info("Added system prop: " + p.getProperty(prop.getName()));
          }
        }
        // config properties
        if (step.getConfigProperties() != null) {
          for (Property prop : step.getConfigProperties()) {
            p.setProperty(prop.getName(), config.getProperty(prop.getValue())
                + ((prop.getSuffix() != null) ? prop.getSuffix() : ""));
            Logger.getLogger(getClass())
                .info("Added config prop: " + p.getProperty(prop.getName()));
          }
        }
        // neither system or config properties
        if (step.getProperties() != null) {
          for (Property prop : step.getProperties()) {
            p.setProperty(prop.getName(),
                prop.getValue() + ((prop.getSuffix() != null) ? prop.getSuffix() : ""));
            Logger.getLogger(getClass())
                .info("Added prop: " + p.getProperty(prop.getName()));
          }
        }

        request.setProperties(p);
        request.setDebug(step.getSetDebug());
        
        if (step.getMavenOpts() != null)
          request.setMavenOpts(step.getMavenOpts());

        result = invoker.execute(request);

        if (result.getExitCode() != 0) {
          throw result.getExecutionException();
        }
      }
    } catch (Exception e) {
      Logger.getLogger(getClass()).error("Error ", e);
      throw new MojoExecutionException("Mojo failed to complete", e);
    }

  }

  @SuppressWarnings("unused")
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Property {

    private String name;
    private String value;
    private String suffix;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Property() {
    }

    /**
     * 
     * @param name
     * @param value
     * @param suffix
     */
    @JsonCreator
    public Property(
        @JsonProperty("name") String name,
        @JsonProperty("value") String value,
        @JsonProperty("suffix") String suffix) {
      super();
      this.name = name;
      this.value = value;
      this.suffix = suffix;
    }

    public String getName() {
      return name;
    }
    
    public void setName(String name) {
      this.name = name;
    }

    public String getValue() {
      return value;
    }
    
    public void setValue(String value) {
      this.value = value;
    }

    public String getSuffix() {
      return suffix;
    }
    
    public void setSuffix(String suffix) {
      this.suffix = suffix;
    }

  }

  @SuppressWarnings("unused")
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class MojoConfig {

    private Integer stepNumber;
    private String log;
    private String pomFile;
    private List<String> profile = null;
    private List<String> goals = null;
    private List<Property> systemProperties = null;
    private List<Property> configProperties = null;
    private List<Property> properties = null;
    private Boolean setDebug;
    private String mavenOpts;

    /**
     * No args constructor for use in serialization
     * 
     */
    public MojoConfig() {
    }

    /**
     * 
     * @param goals
     *          Maven goals
     * @param pomFile
     *          Location of Maven pom file.
     * @param setDebug
     *          Maven debug
     * @param properties
     *          Maven mojo properties
     * @param stepNumber
     *          Process step number
     * @param log
     *          Text to log at start of execution
     * @param profile
     *          Maven profiles to execute
     */
    @JsonCreator
    public MojoConfig(@JsonProperty("stepNumber") Integer stepNumber,
        @JsonProperty("log") String log,
        @JsonProperty("pomFile") String pomFile,
        @JsonProperty("profile") List<String> profile,
        @JsonProperty("goals") List<String> goals,
        @JsonProperty("systemProperties") List<Property> systemProperties,
        @JsonProperty("configProperties") List<Property> configProperties,
        @JsonProperty("properties") List<Property> properties,
        @JsonProperty("setDebug") Boolean setDebug,
        @JsonProperty("mavenOpts") String mavenOpts) {
      super();
      this.stepNumber = stepNumber;
      this.log = log;
      this.pomFile = pomFile;
      this.profile = profile;
      this.goals = goals;
      this.systemProperties = systemProperties;
      this.configProperties = configProperties;
      this.properties = properties;
      this.setDebug = setDebug;
      this.mavenOpts = mavenOpts;
    }

    public Integer getStepNumber() {
      return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
      this.stepNumber = stepNumber;
    }

    public String getLog() {
      return log;
    }

    public void setLog(String log) {
      this.log = log;
    }

    public String getPomFile() {
      return pomFile;
    }

    public void setPomFile(String pomFile) {
      this.pomFile = pomFile;
    }

    public List<String> getProfile() {
      return profile;
    }

    public void setProfile(List<String> profile) {
      this.profile = profile;
    }

    public List<String> getGoals() {
      return goals;
    }

    public void setGoals(List<String> goals) {
      this.goals = goals;
    }

    public List<Property> getSystemProperties() {
      return systemProperties;
    }

    public void setSystemProperties(List<Property> systemProperties) {
      this.systemProperties = systemProperties;
    }

    public List<Property> getConfigProperties() {
      return configProperties;
    }

    public void setConifgProperties(List<Property> configProperties) {
      this.configProperties = configProperties;
    }

    public List<Property> getProperties() {
      return properties;
    }

    public void setProperties(List<Property> properties) {
      this.properties = properties;
    }

    public Boolean getSetDebug() {
      return setDebug;
    }

    public void setSetDebug(Boolean setDebug) {
      this.setDebug = setDebug;
    }
    
    public String getMavenOpts() {
      return mavenOpts;
    }

    public void setMavenOpts(String mavenOpts) {
      this.mavenOpts = mavenOpts;
    }

  }

}
