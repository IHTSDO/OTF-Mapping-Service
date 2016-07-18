/*
 * Copyright 2016 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.test.mojo;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A mechanism to reset to the stock demo database.
 */
public class ResetDemoDatabase {

  /** The properties. */
  static Properties config;

  /** The server. */
  static String server = "false";

  /**
   * Create test fixtures for class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {
    config = ConfigUtility.getConfigProperties();
  }

  /**
   * Test the sequence:
   * 
   * <pre>
   * 1. Load SNOMED terminology
   * 2. Load "allergy" terminology
   * </pre>
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  @Test
  public void test() throws Exception {

    if (config.getProperty("data.dir") == null) {
      throw new Exception("Config file must specify a data.dir property");
    }

    // Create database
    InvocationRequest request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/updatedb/pom.xml"));
    request.setProfiles(Arrays.asList("Updatedb"));
    request.setGoals(Arrays.asList("clean", "install"));
    Properties p = new Properties();
    p.setProperty("run.config", System.getProperty("run.config"));
    p.setProperty("hibernate.hbm2ddl.auto", "create");
    request.setProperties(p);
    request.setDebug(false);
    Invoker invoker = new DefaultInvoker();
    InvocationResult result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Clear indexes
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/lucene/pom.xml"));
    request.setProfiles(Arrays.asList("Reindex"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config", System.getProperty("run.config"));
    request.setProperties(p);
    request.setDebug(false);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Load RF2 snapshot
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("RF2-snapshot"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config", System.getProperty("run.config"));
    p.setProperty("terminology", "SNOMEDCT");
    p.setProperty("version", "20140731");
    p.setProperty("input.dir", config.getProperty("data.dir") + "/"
        + "snomedct-20140731-mini");
    request.setProperties(p);
    request.setDebug(false);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Load allergy terminology
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("Simple"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config", System.getProperty("run.config"));
    p.setProperty("terminology", "ALLERGY");
    p.setProperty("version", "latest");
    p.setProperty("input.file", config.getProperty("data.dir") + "/"
        + "allergy.txt");

    request.setProperties(p);
    request.setDebug(false);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

    // Generate Demo Data
    request = new DefaultInvocationRequest();
    request.setPomFile(new File("../admin/loader/pom.xml"));
    request.setProfiles(Arrays.asList("GenerateDemoData"));
    request.setGoals(Arrays.asList("clean", "install"));
    p = new Properties();
    p.setProperty("run.config", System.getProperty("run.config"));
    request.setProperties(p);
    invoker = new DefaultInvoker();
    result = invoker.execute(request);
    if (result.getExitCode() != 0) {
      throw result.getExecutionException();
    }

  }

  /**
   * Create test fixtures per test.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() throws Exception {
    // n/a
  }

  /**
   * Teardown.
   *
   * @throws Exception the exception
   */
  @After
  public void teardown() throws Exception {
    // n/a
  }

  /**
   * Teardown class.
   *
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {
    // n/a
  }

}
