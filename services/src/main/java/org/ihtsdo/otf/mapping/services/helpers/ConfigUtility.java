/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.services.helpers;

import java.io.File;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.Configurable;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;

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
  public synchronized static Properties getConfigProperties() throws Exception {
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
  public synchronized static Properties getTestConfigProperties()
    throws Exception {
    if (testConfig == null) {
      String configFileName = System.getProperty("run.config.test");
      Logger.getLogger(ConfigUtility.class.getName()).info(
          "  run.config.test = " + configFileName);
      testConfig = new Properties();
      FileReader in = new FileReader(new File(configFileName));
      testConfig.load(in);
      in.close();
      Logger.getLogger(ConfigUtility.class)
          .info("  properties = " + testConfig);
    }
    return testConfig;
  }

  /**
   * New handler instance.
   *
   * @param <T> the
   * @param handler the handler
   * @param handlerClass the handler class
   * @param type the type
   * @return the object
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public static <T> T newHandlerInstance(String handler, String handlerClass,
    Class<T> type) throws Exception {
    if (handlerClass == null) {
      throw new Exception("Handler class " + handlerClass + " is not defined");
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
      return (T) o;
    }
    throw new Exception("Handler is not assignable from " + type.getName());
  }

  /**
   * New standard handler instance with configuration.
   *
   * @param <T> the
   * @param property the property
   * @param handlerName the handler name
   * @param type the type
   * @return the t
   * @throws Exception the exception
   */
  public static <T extends Configurable> T newStandardHandlerInstanceWithConfiguration(
    String property, String handlerName, Class<T> type) throws Exception {

    // Instantiate the handler
    // property = "metadata.service.handler" (e.g)
    // handlerName = "SNOMED" (e.g.)
    String classKey = property + "." + handlerName + ".class";
    if (config.getProperty(classKey) == null) {
      throw new Exception("Unexpected null classkey " + classKey);
    }
    String handlerClass = config.getProperty(classKey);
    Logger.getLogger(ConfigUtility.class).info("Instantiate " + handlerClass);
    T handler =
        ConfigUtility.newHandlerInstance(handlerName, handlerClass, type);

    // Look up and build properties
    Properties handlerProperties = new Properties();
    handlerProperties.setProperty("security.handler", handlerName);

    for (Object key : config.keySet()) {
      // Find properties like "metadata.service.handler.SNOMED.class"
      if (key.toString().startsWith(property + "." + handlerName + ".")) {
        String shortKey =
            key.toString().substring(
                (property + "." + handlerName + ".").length());
        Logger.getLogger(ConfigUtility.class).info(
            " property " + shortKey + " = "
                + config.getProperty(key.toString()));
        handlerProperties.put(shortKey, config.getProperty(key.toString()));
      }
    }
    // handler.setProperties(handlerProperties);
    return handler;
  }

  /**
   * Indicates whether or not recipients list specified is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public static boolean isRecipientsListSpecified() {
    return !config.getProperty("send.notification.recipients").isEmpty();
  }

  /**
   * Send validation result email.
   *
   * @param recipients the recipients
   * @param subject the subject
   * @param message the message
   * @param validationResult the validation result
   * @throws Exception the exception
   */
  public static void sendValidationResultEmail(String recipients,
    String subject, String message, ValidationResult validationResult)
    throws Exception {

    // validation message is specified header message and two new lines
    String validationMessage = message + "\n\n";

    // add the messages
    if (!validationResult.getMessages().isEmpty()) {
      validationMessage += "Messages:\n";
      for (String s : validationResult.getMessages()) {
        validationMessage += "  " + s + "\n";
      }
      validationMessage += "\n";
    }

    // add the errors
    if (!validationResult.getErrors().isEmpty()) {
      validationMessage += "Errors:\n";
      for (String s : validationResult.getErrors()) {
        validationMessage += "  " + s + "\n";
      }
      validationMessage += "\n";
    }

    // add the warnings
    if (!validationResult.getWarnings().isEmpty()) {
      validationMessage += "Warnings:\n";
      for (String s : validationResult.getWarnings()) {
        validationMessage += "  " + s + "\n";
      }
      validationMessage += "\n";
    }

    // send the revised message
    ConfigUtility.sendEmail(recipients, subject, validationMessage);

  }

  /**
   * Sends email.
   *
   * @param recipients the recipients
   * @param subject the subject
   * @param body the body
   * @throws Exception the exception
   */
  public static void sendEmail(String recipients, String subject, String body)
    throws Exception {
    // avoid sending mail if disabled
    final Properties details = ConfigUtility.getConfigProperties();

    String from = details.getProperty("mail.smtp.user");
    if ("false".equals(details.getProperty("mail.enabled"))) {
      // do nothing
      return;
    }
    SMTPAuthenticator auth = new SMTPAuthenticator();
    Session session = Session.getInstance(config, auth);

    MimeMessage msg = new MimeMessage(session);
    if (body.contains("<html")) {
      msg.setContent(body.toString(), "text/html; charset=utf-8");
    } else {
      msg.setText(body.toString());
    }
    msg.setSubject(subject);
    msg.setFrom(new InternetAddress(from));
    String[] recipientsArray = recipients.split(";");
    for (String recipient : recipientsArray) {
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
    }
    Transport.send(msg);
  }

  /**
   * SMTPAuthenticator.
   */
  public static class SMTPAuthenticator extends javax.mail.Authenticator {

    /**
     * Instantiates an empty {@link SMTPAuthenticator}.
     */
    public SMTPAuthenticator() {
      // do nothing
    }

    /* see superclass */
    @Override
    public PasswordAuthentication getPasswordAuthentication() {
      Properties config = null;
      try {
        config = ConfigUtility.getConfigProperties();
      } catch (Exception e) {
        // do nothing
      }
      if (config == null) {
        return null;
      } else {
        return new PasswordAuthentication(config.getProperty("mail.smtp.user"),
            config.getProperty("mail.smtp.password"));
      }
    }
  }

  /**
   * Returns the raw bytes.
   * 
   * @param uid the uid
   * @return the raw bytes
   */
  public static byte[] getRawBytes(UUID uid) {
    String id = uid.toString();
    byte[] rawBytes = new byte[16];

    for (int i = 0, j = 0; i < 36; ++j) {
      // Need to bypass hyphens:
      switch (i) {
        case 8:
        case 13:
        case 18:
        case 23:
          ++i;
          break;
        default:
          break;
      }
      char c = id.charAt(i);

      if (c >= '0' && c <= '9') {
        rawBytes[j] = (byte) ((c - '0') << 4);
      } else if (c >= 'a' && c <= 'f') {
        rawBytes[j] = (byte) ((c - 'a' + 10) << 4);
      }

      c = id.charAt(++i);

      if (c >= '0' && c <= '9') {
        rawBytes[j] |= (byte) (c - '0');
      } else if (c >= 'a' && c <= 'f') {
        rawBytes[j] |= (byte) (c - 'a' + 10);
      }
      ++i;
    }
    return rawBytes;
  }

  /**
   * Gets the release uuid.
   *
   * @param hash the hash
   * @return the release uuid
   * @throws NoSuchAlgorithmException the no such algorithm exception
   * @throws UnsupportedEncodingException the unsupported encoding exception
   */
  public static UUID getReleaseUuid(String hash)
    throws NoSuchAlgorithmException, UnsupportedEncodingException {
    return getUuidForString(hash);
  }

  /**
   * Returns the uuid for string.
   *
   * @param name the name
   * @return the uuid for string
   * @throws NoSuchAlgorithmException the no such algorithm exception
   * @throws UnsupportedEncodingException the unsupported encoding exception
   */
  public static UUID getUuidForString(String name)
    throws NoSuchAlgorithmException, UnsupportedEncodingException {

    MessageDigest sha1Algorithm = MessageDigest.getInstance("SHA-1");

    String namespace = "00000000-0000-0000-0000-000000000000";
    String encoding = "UTF-8";

    UUID namespaceUUID = UUID.fromString(namespace);

    // Generate the digest.
    sha1Algorithm.reset();

    // Generate the digest.
    sha1Algorithm.reset();
    if (namespace != null) {
      sha1Algorithm.update(getRawBytes(namespaceUUID));
    }

    sha1Algorithm.update(name.getBytes(encoding));
    byte[] sha1digest = sha1Algorithm.digest();

    sha1digest[6] &= 0x0f; /* clear version */
    sha1digest[6] |= 0x50; /* set to version 5 */
    sha1digest[8] &= 0x3f; /* clear variant */
    sha1digest[8] |= 0x80; /* set to IETF variant */

    long msb = 0;
    long lsb = 0;
    for (int i = 0; i < 8; i++) {
      msb = (msb << 8) | (sha1digest[i] & 0xff);
    }
    for (int i = 8; i < 16; i++) {
      lsb = (lsb << 8) | (sha1digest[i] & 0xff);
    }

    return new UUID(msb, lsb);

  }

  /** The comparator. */
  public static Comparator<String> COMPLEX_MAP_COMPARATOR =
      new Comparator<String>() {

        @Override
        public int compare(String o1, String o2) {
          String[] fields1 = o1.split("\t");
          String[] fields2 = o2.split("\t");

          int i = fields1[4].compareTo(fields2[4]);
          if (i != 0) {
            return i;
          } else {
            i = fields1[5].compareTo(fields2[5]);
            if (i != 0) {
              return i;
            } else {
              // Handle simple case
              if (fields1.length == 7) {
                return fields1[6].compareTo(fields2[6]);
              }
              i = Integer.parseInt(fields1[6]) - Integer.parseInt(fields2[6]);
              if (i != 0) {
                return i;
              } else {
                i = Integer.parseInt(fields1[7]) - Integer.parseInt(fields2[7]);
                if (i != 0) {
                  return i;
                } else {
                  i =
                      (fields1[0] + fields1[1] + fields1[2] + fields1[3])
                          .compareTo(fields2[0] + fields2[1] + fields2[2]
                              + fields2[3]);
                  if (i != 0) {
                    return i;
                  } else {
                    i = fields1[8].compareTo(fields2[8]);
                    if (i != 0) {
                      return i;
                    } else {
                      i = fields1[9].compareTo(fields2[9]);
                      if (i != 0) {
                        return i;
                      } else {
                        i = fields1[10].compareTo(fields2[10]);
                        if (i != 0) {
                          return i;
                        } else {
                          i = fields1[11].compareTo(fields2[11]);
                          if (i != 0) {
                            return i;
                          }
                          // leave out 13th field so it works for complex map
                          // too.
                          else {
                            return 0;
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      };

  /** The comparator. */
  public static Comparator<String> TSV_COMPARATOR = new Comparator<String>() {

    @Override
    public int compare(String o1, String o2) {
      String[] fields1 = o1.split("\t");
      String[] fields2 = o2.split("\t");

      int i = fields1[4].compareTo(fields2[4]);
      if (i != 0) {
        return i;
      } else {
        i = fields1[5].compareTo(fields2[5]);
        if (i != 0) {
          return i;
        } else {
          if (fields1.length == 7) {
            return fields1[6].compareTo(fields2[6]);
          }
          i = Integer.parseInt(fields1[7]) - Integer.parseInt(fields2[7]);
          if (i != 0) {
            return i;
          } else {
            i = Integer.parseInt(fields1[8]) - Integer.parseInt(fields2[8]);
            if (i != 0) {
              return i;
            } else {
              i =
                  (fields1[0] + fields1[1] + fields1[2] + fields1[3])
                      .compareTo(fields2[0] + fields2[1] + fields2[2]
                          + fields2[3]);
              if (i != 0) {
                return i;
              } else {
                return 0;
              }
            }
          }
        }
      }
    }
  };
}
