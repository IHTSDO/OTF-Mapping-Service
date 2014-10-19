package org.ihtsdo.otf.mapping.services.helpers;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Loads and serves configuration.
 */
public class ConfigUtility {

  /** The config. */
  public static Properties config = null;

  /** The test config. */
  public static Properties testConfig = null;

  /**
   * Returns the config properties.
   * @return the config properties
   *
   * @throws Exception the exception
   */
  public static Properties getConfigProperties() throws Exception {
    if (config == null) {
      String configFileName = System.getProperty("run.config");
      Logger.getLogger(ConfigUtility.class.getName()).info(
          "  run.config = " + configFileName);
      config = new Properties();
      FileReader in = new FileReader(new File(configFileName));
      config.load(in);
      in.close();
      Logger.getLogger(ConfigUtility.class).info("  properties = " + config);
    }
    return config;
  }

  /**
   * Returns the config properties.
   * @return the config properties
   *
   * @throws Exception the exception
   */
  public static Properties getTestConfigProperties() throws Exception {
    if (testConfig == null) {
      String configFileName = System.getProperty("run.config.test");
      Logger.getLogger(ConfigUtility.class.getName()).info(
          "  run.config.test = " + configFileName);
      testConfig = new Properties();
      FileReader in = new FileReader(new File(configFileName));
      testConfig.load(in);
      in.close();
      Logger.getLogger(ConfigUtility.class).info("  properties = " + testConfig);
    }
    return testConfig;
  }

  /**
   * New handler instance.
   *
   * @param handler the handler
   * @param handlerClass the handler class
   * @param type the type
   * @return the object
   * @throws Exception the exception
   */
  public static Object newHandlerInstance(String handler, String handlerClass,
    Class<?> type) throws Exception {
    if (handlerClass == null) {
      throw new Exception("Handler class " + handler + " is not defied");
    }
    Class<?> toInstantiate = Class.forName(handlerClass);
    if (toInstantiate == null) {
      throw new Exception("Unable to find class " + handlerClass);
    }
    Object o = null;
    try {
      o = toInstantiate.newInstance();
    } catch (Exception e) {
      // do nothing
    }
    if (o == null) {
      throw new Exception("Unable to instantiate class " + handlerClass
          + ", check for default constructor.");
    }
    if (type.isAssignableFrom(o.getClass())) {
      return toInstantiate;
    }
    throw new Exception("Handler is not assignable from " + type.getName());
  }
}
