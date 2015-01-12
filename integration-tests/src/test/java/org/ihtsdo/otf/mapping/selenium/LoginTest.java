package org.ihtsdo.otf.mapping.selenium;

import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.MapRefsetPattern;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.RelationStyle;
import org.ihtsdo.otf.mapping.helpers.WorkflowType;
import org.ihtsdo.otf.mapping.jpa.MapProjectJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.handlers.DefaultProjectSpecificAlgorithmHandler;
import org.ihtsdo.otf.mapping.jpa.services.MappingServiceJpa;
import org.ihtsdo.otf.mapping.model.MapProject;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LoginTest {

  private static WebDriver webDriver;

  private static Properties config;


  @BeforeClass
  public static void init() throws Exception {

    // get the config properties
    config = ConfigUtility.getConfigProperties();

    // construct a new webdriver
    switch (config.getProperty("selenium.browser")) {
      case "firefox":
        webDriver = new FirefoxDriver();
        break;
      case "chrome":
        webDriver = new ChromeDriver();
        break;
      case "ie":
        webDriver = new InternetExplorerDriver();
        break;
      default:
        throw new Exception(
            "Invalid browser specified in config file.  Valid options are: firefox, chrome, ie");
    }

    webDriver
        .manage()
        .timeouts()
        .implicitlyWait(new Long(config.getProperty("selenium.timeout")),
            TimeUnit.SECONDS);
  }

  @AfterClass
  public static void cleanup() throws Exception {

    if (webDriver != null) {
      webDriver.quit();
    }
  }

  @Test
  public void guestLoginTest() throws Exception {
    Logger.getLogger(LoginTest.class).info("Testing guest login...");
    // Open website
    webDriver.get(config.getProperty("selenium.url"));

    // Find the Guest login button and click it
    webDriver.findElement(By.id("guestLoginButton")).click();

    // Find the header content and test once injection is complete
    (new WebDriverWait(webDriver, new Long(
        config.getProperty("selenium.timeout")) / 1000))
        .until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
            return webDriver.findElement(By.id("userAndRole")).getText()
                .length() > 0;
          }
        });

    assertTrue(webDriver.findElement(By.id("userAndRole")).getText()
        .matches(".*(.*)*"));

    Logger.getLogger(LoginTest.class).info("  Success!");
    
    // TODO:  Add logout
  }

  @Test
  public void userValidLoginTest() throws Exception {

    Logger.getLogger(LoginTest.class).info("Testing valid user login...");

    // Open website
    webDriver.get(config.getProperty("selenium.url"));

    // fill in the user name from created valid user
    webDriver.findElement(By.id("userField")).sendKeys(config.getProperty("selenium.user.valid.name"));

    // fill in the password from config file
    webDriver.findElement(By.id("passwordField")).sendKeys(
        config.getProperty("selenium.user.valid.password"));

    // login
    webDriver.findElement(By.id("userLoginButton")).click();

    // Find the header content and test once injection is complete
    (new WebDriverWait(webDriver, new Long(
        config.getProperty("selenium.timeout")) / 1000))
        .until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
            return webDriver.findElement(By.id("userAndRole")).getText()
                .length() > 0;
          }
        });

    // verify that string matches pattern Text (Text)
    assertTrue(webDriver.findElement(By.id("userAndRole")).getText()
        .matches(".*(.*)*"));
    Logger.getLogger(LoginTest.class).info("  Success!");
    
    // TODO:  Also logout

  }

  @Test
  public void userInvalidLoginTest() {

    Logger.getLogger(LoginTest.class).info("Testing invalid user login...");

    // Open website
    webDriver.get(config.getProperty("selenium.url"));

    // fill in the user name from created valid user
    webDriver.findElement(By.id("userField")).sendKeys(
        config.getProperty("selenium.user.invalid.name"));

    // fill in the password from config file
    webDriver.findElement(By.id("passwordField")).sendKeys(
        "invalid_password");

    // login
    webDriver.findElement(By.id("userLoginButton")).click();

    // Find the header content and test once injection is complete
    (new WebDriverWait(webDriver, new Long(
        config.getProperty("selenium.timeout")) / 1000))
        .until(new ExpectedCondition<Boolean>() {
          public Boolean apply(WebDriver d) {
            return webDriver.findElement(By.id("globalError")).getText()
                .length() > 0;
          }
        });

    // verify that string matches pattern Text (Text)
    assertTrue(webDriver.findElement(By.id("globalError")).getText().length() > 0);
    Logger.getLogger(LoginTest.class).info("  Success!");
  }
}
