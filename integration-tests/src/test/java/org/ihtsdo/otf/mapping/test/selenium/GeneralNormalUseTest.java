package org.ihtsdo.otf.mapping.test.selenium;

import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
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

/**
 * Selenium login test class.
 */
public class GeneralNormalUseTest {

  /** The web driver. */
  static WebDriver webDriver;

  /** The config. */
  private static Properties config;

  /**
   * Sets up the class.
   *
   * @throws Exception the exception
   */
  @BeforeClass
  public static void setupClass() throws Exception {

    // get the config properties
    config = ConfigUtility.getConfigProperties();

    // Skip if not specified
    if (config.contains("selenium.browser")) {
      return;
    }
    
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

  /**
   * Teardown for class
   * @throws Exception the exception
   */
  @AfterClass
  public static void teardownClass() throws Exception {

    // Skip if not specified
    if (config.contains("selenium.browser")) {
      return;
    }

    if (webDriver != null) {
      webDriver.quit();
    }
  }

  /**
   * Test of normal use of logging in to the application.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNormalUseGuiGeneral001() throws Exception {
    // Skip if not specified
    if (config.contains("selenium.browser")) {
      return;
    }
    Logger.getLogger(getClass()).info("Testing guest login...");

    // Open website
    webDriver.get(config.getProperty("selenium.url"));

    // Find the Guest login button and click it
    webDriver.findElement(By.id("guestLoginButton")).click();

    // Find the header content and test once injection is complete
    (new WebDriverWait(webDriver, new Long(
        config.getProperty("selenium.timeout")) / 1000))
        .until(new ExpectedCondition<Boolean>() {
          @Override
          public Boolean apply(WebDriver d) {
            return webDriver.findElement(By.id("userAndRole")).getText()
                .length() > 0;
          }
        });

    assertTrue(webDriver.findElement(By.id("userAndRole")).getText()
        .matches(".*(.*)*"));
    Logger.getLogger(getClass()).info("  PASS");

    Logger.getLogger(getClass()).info("Testing valid user login...");

    // Open website
    webDriver.get(config.getProperty("selenium.url"));

    // fill in the user name from created valid user
    webDriver.findElement(By.id("userField")).sendKeys(
        config.getProperty("selenium.user.valid.name"));

    // fill in the password from config file
    webDriver.findElement(By.id("passwordField")).sendKeys(
        config.getProperty("selenium.user.valid.password"));

    // login
    webDriver.findElement(By.id("userLoginButton")).click();

    // Find the header content and test once injection is complete
    (new WebDriverWait(webDriver, new Long(
        config.getProperty("selenium.timeout")) / 1000))
        .until(new ExpectedCondition<Boolean>() {
          @Override
          public Boolean apply(WebDriver d) {
            return webDriver.findElement(By.id("userAndRole")).getText()
                .length() > 0;
          }
        });

    // verify that string matches pattern Text (Text)
    assertTrue(webDriver.findElement(By.id("userAndRole")).getText()
        .matches(".*(.*)*"));
    Logger.getLogger(getClass()).info("  PASS");

  }

}
