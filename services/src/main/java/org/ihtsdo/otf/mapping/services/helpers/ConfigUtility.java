/*
 *    Copyright 2024 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.services.helpers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.mapping.helpers.Configurable;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and serves configuration.
 */
public class ConfigUtility {

  /** The Constant LOGGER. */
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ConfigUtility.class);

  /** The config. */
  public static Properties config = null;

  /** The test config. */
  public static Properties testConfig = null;

  /**  The project id to scope concepts. */
  protected static Map<Long, Set<String>> projectIdToScopeConcepts =
      new HashMap<>();

  /**  The generic user cookie. */
  protected static String genericUserCookie = null;

  /**
   * The get local config file.
   *
   * @return the local config file
   * @throws Exception the exception
   */
  public static String getLocalConfigFile() throws Exception {
    return getLocalConfigFolder() + "config.properties";
  }

  /**
   * Gets the local config folder.
   *
   * @return the local config folder
   * @throws Exception the exception
   */
  public static String getLocalConfigFolder() throws Exception {
    return System.getProperty("user.home") + "/.mapping-service/"
        + getConfigLabel() + "/";
  }

  /**
   * Gets the scope concepts for map project.
   *
   * @param id the id
   * @return the scope concepts for map project
   */
  public static Set<String> getScopeConceptsForMapProject(Long id) {
    return projectIdToScopeConcepts.get(id);
  }

  /**
   * Sets the scope concepts for map project.
   *
   * @param id the id
   * @param scopeConcepts the scope concepts
   */
  public static void setScopeConceptsForMapProject(Long id,
    Set<String> scopeConcepts) {
    projectIdToScopeConcepts.put(id, scopeConcepts);
  }

  /**
   * Get the config label.
   *
   * @return the label
   * @throws Exception the exception
   */
  public static String getConfigLabel() throws Exception {
    // Need to determine the label (default "")
    String label = "";
    Properties labelProp = new Properties();

    // If no resource is available, go with the default
    // ONLY setups that explicitly intend to override the setting
    // cause it to be something other than the default.
    InputStream input = ConfigUtility.class.getResourceAsStream("/label.prop");
    if (input != null) {
      labelProp.load(input);
      // If a run.config.label override can be found, use it
      String candidateLabel = labelProp.getProperty("run.config.label");
      // If the default, uninterpolated value is used, stick again with the
      // default
      if (candidateLabel != null
          && !candidateLabel.equals("${run.config.label}")) {
        label = candidateLabel;
      }
    } else {
      LOGGER.info("  label.prop resource cannot be found, using default");

    }
    LOGGER.info("  run.config.label = " + label);

    return label;
  }

  /**
   * Returns the config properties.
   * @return the config properties
   *
   * @throws Exception the exception
   */
  public synchronized static Properties getConfigProperties() throws Exception {
    if (config == null) {
      String configFileName = System.getProperty("run.config");
      LOGGER.info("  run.config = " + configFileName);
      config = new Properties();
      FileReader in = new FileReader(new File(configFileName));
      config.load(in);
      in.close();
      LOGGER.info("  properties = " + config);
    }
    return config;
  }

  /**
   * Returns the ui config properties.
   *
   * @return the ui config properties
   * @throws Exception the exception
   */
  public static Properties getUiConfigProperties() throws Exception {
    final Properties config = getConfigProperties();
    // use "deploy.*" and "site.*" and "base.url" properties
    final Properties p = new Properties();
    for (final Object prop : config.keySet()) {
      final String str = prop.toString();

      if (str.startsWith("deploy.") || str.startsWith("site.")
          || str.startsWith("base.url") || str.startsWith("logout.")
          || str.contains(".OAUTH2.")) {
        p.put(prop, config.getProperty(prop.toString()));
      }

      if (str.startsWith("security") && str.contains("url")) {
        p.put(prop, config.getProperty(prop.toString()));
      }

    }
    return p;

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
      LOGGER.info("  run.config.test = " + configFileName);
      testConfig = new Properties();
      FileReader in = new FileReader(new File(configFileName));
      testConfig.load(in);
      in.close();
      LOGGER.info("  properties = " + testConfig);
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
      o = toInstantiate.getDeclaredConstructor().newInstance();
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
    LOGGER.info("Instantiate " + handlerClass);
    T handler =
        ConfigUtility.newHandlerInstance(handlerName, handlerClass, type);

    // Look up and build properties
    Properties handlerProperties = new Properties();
    handlerProperties.setProperty("security.handler", handlerName);

    for (Object key : config.keySet()) {
      // Find properties like "metadata.service.handler.SNOMED.class"
      if (key.toString().startsWith(property + "." + handlerName + ".")) {
        String shortKey = key.toString()
            .substring((property + "." + handlerName + ".").length());
        if (!shortKey.contains("secret") && !shortKey.contains("client")) {
          LOGGER.info(" property " + shortKey + " = "
              + config.getProperty(key.toString()));
        }
        handlerProperties.put(shortKey, config.getProperty(key.toString()));
      }
    }
    handler.setProperties(handlerProperties);
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
    String from;
    if (config.containsKey("mail.smtp.from")) {
      from = config.getProperty("mail.smtp.from");
    } else {
      from = config.getProperty("mail.smtp.user");
    }
    Properties props = new Properties();
    props.put("mail.smtp.user", config.getProperty("mail.smtp.user"));
    props.put("mail.smtp.password", config.getProperty("mail.smtp.password"));
    props.put("mail.smtp.host", config.getProperty("mail.smtp.host"));
    props.put("mail.smtp.port", config.getProperty("mail.smtp.port"));
    props.put("mail.smtp.starttls.enable",
        config.getProperty("mail.smtp.starttls.enable"));
    props.put("mail.smtp.auth", config.getProperty("mail.smtp.auth"));
    ConfigUtility.sendEmail(subject, from, recipients, validationMessage, props,
        "true".equals(config.getProperty("mail.smtp.auth")));

  }

  /**
   * Sends email.
   *
   * @param subject the subject
   * @param from the from
   * @param recipients the recipients
   * @param body the body
   * @param details the details
   * @param authFlag the auth flag
   * @throws Exception the exception
   */
  public static void sendEmail(String subject, String from, String recipients,
    String body, Properties details, boolean authFlag) throws Exception {
    // avoid sending mail if disabled
    if ("false".equals(details.getProperty("mail.enabled"))) {
      // do nothing
      return;
    }
    Session session = null;
    if (authFlag) {
      Authenticator auth = new SMTPAuthenticator();
      session = Session.getInstance(details, auth);
    } else {
      session = Session.getInstance(details);
    }

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
      msg.addRecipient(Message.RecipientType.TO,
          new InternetAddress(recipient));
    }
    Transport.send(msg);
  }

  /**
   * Sends email with attachment.
   *
   * @param subject the subject
   * @param from the from
   * @param recipients the recipients
   * @param body the body
   * @param details the details
   * @param filename the filename
   * @param authFlag the auth flag
   * @throws Exception the exception
   */
  public static void sendEmailWithAttachment(String subject, String from,
    String recipients, String body, Properties details, String filename,
    boolean authFlag) throws Exception {
    // avoid sending mail if disabled
    if ("false".equals(details.getProperty("mail.enabled"))) {
      // do nothing
      return;
    }
    Session session = null;
    if (authFlag) {
      Authenticator auth = new SMTPAuthenticator();
      session = Session.getInstance(details, auth);
    } else {
      session = Session.getInstance(details);
    }

    MimeMessage msg = new MimeMessage(session);

    // Create the message part
    BodyPart messageBodyPart = new MimeBodyPart();

    // Set text message part
    if (body.contains("<html")) {
      messageBodyPart.setContent(body.toString(), "text/html; charset=utf-8");
    } else {
      messageBodyPart.setText(body.toString());
    }

    // Create a multipart message
    Multipart multipart = new MimeMultipart();

    multipart.addBodyPart(messageBodyPart);

    // Set the attachment
    messageBodyPart = new MimeBodyPart();
    DataSource source = new FileDataSource(filename);
    messageBodyPart.setDataHandler(new DataHandler(source));
    messageBodyPart.setFileName(filename);
    multipart.addBodyPart(messageBodyPart);

    // Send the complete message parts
    msg.setContent(multipart);

    msg.setSubject(subject);
    msg.setFrom(new InternetAddress(from));
    String[] recipientsArray = recipients.split(";");
    for (String recipient : recipientsArray) {
      msg.addRecipient(Message.RecipientType.TO,
          new InternetAddress(recipient));
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
   * Capitalize.
   *
   * @param value the value
   * @return the string
   */
  public static String capitalize(String value) {
    if (value == null) {
      return value;
    }
    return value.substring(0, 1).toUpperCase() + value.substring(1);
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
              // Handle simple case - compare referencedComponentId (will be
              // unique)
              if (fields1.length == 7) {
                return 0;
              }
              i = Integer.parseInt(fields1[6]) - Integer.parseInt(fields2[6]);
              if (i != 0) {
                return i;
              } else {
                i = Integer.parseInt(fields1[7]) - Integer.parseInt(fields2[7]);
                if (i != 0) {
                  return i;
                } else {
                  i = (fields1[0] + fields1[1] + fields1[2] + fields1[3])
                      .compareTo(
                          fields2[0] + fields2[1] + fields2[2] + fields2[3]);
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
          // handle simple human readable case
          if (fields1.length == 9) {
            return 0;
          }
          i = Integer.parseInt(fields1[7]) - Integer.parseInt(fields2[7]);
          if (i != 0) {
            return i;
          } else {
            i = Integer.parseInt(fields1[8]) - Integer.parseInt(fields2[8]);
            if (i != 0) {
              return i;
            } else {
              i = (fields1[0] + fields1[1] + fields1[2] + fields1[3])
                  .compareTo(fields2[0] + fields2[1] + fields2[2] + fields2[3]);
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

  /**
   * To arabic.
   *
   * @param number the number
   * @return the int
   * @throws Exception the exception
   */
  public static int toArabic(String number) throws Exception {
    if (number.isEmpty())
      return 0;
    if (number.startsWith("M"))
      return 1000 + toArabic(number.substring(1));
    if (number.startsWith("CM"))
      return 900 + toArabic(number.substring(2));
    if (number.startsWith("D"))
      return 500 + toArabic(number.substring(1));
    if (number.startsWith("CD"))
      return 400 + toArabic(number.substring(2));
    if (number.startsWith("C"))
      return 100 + toArabic(number.substring(1));
    if (number.startsWith("XC"))
      return 90 + toArabic(number.substring(2));
    if (number.startsWith("L"))
      return 50 + toArabic(number.substring(1));
    if (number.startsWith("XL"))
      return 40 + toArabic(number.substring(2));
    if (number.startsWith("X"))
      return 10 + toArabic(number.substring(1));
    if (number.startsWith("IX"))
      return 9 + toArabic(number.substring(2));
    if (number.startsWith("V"))
      return 5 + toArabic(number.substring(1));
    if (number.startsWith("IV"))
      return 4 + toArabic(number.substring(2));
    if (number.startsWith("I"))
      return 1 + toArabic(number.substring(1));
    throw new Exception("something bad happened");
  }

  /**
   * Indicates whether or not roman numeral is the case.
   *
   * @param number the number
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public static boolean isRomanNumeral(String number) {
    return number
        .matches("^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$");
  }

  /**
   * Indicates whether or not the server is active.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws Exception the exception
   */
  public static boolean isServerActive() throws Exception {
    if (config == null)
      config = ConfigUtility.getConfigProperties();

    try {
      // Attempt to logout to verify service is up (this works like a
      // "ping").
      Client client = ClientBuilder.newClient();
      WebTarget target = client.target(
          config.getProperty("base.url") + "/security/logout/user/id/dummy");

      Response response = target.request(MediaType.TEXT_PLAIN).post(null);
      if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Gets the generic user cookie.
   *
   * @return the generic user cookie
   * @throws Exception the exception
   */
  public static String getGenericUserCookie() throws Exception {

    if (genericUserCookie != null) {
      return genericUserCookie;
    }

    // Login the generic user, then save and return the cookie
    final String userName = ConfigUtility.getConfigProperties()
        .getProperty("generic.user.userName");
    final String password = ConfigUtility.getConfigProperties()
        .getProperty("generic.user.password");
    final String imsUrl = ConfigUtility.getConfigProperties()
        .getProperty("generic.user.authenticationUrl");

    if (StringUtils.isAnyBlank(userName, password, imsUrl)) {
      throw new LocalException("Verify properties for generic.user are set.");
    }

    final Client client = ClientBuilder.newClient();
    final WebTarget target = client.target(imsUrl + "/authenticate");
    final Builder builder = target.request(MediaType.APPLICATION_JSON);

    try (Response response = builder.post(Entity.json("{ \"login\": \""
        + userName + "\", \"password\": \"" + password + "\" }"));) {
      if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
        throw new LocalException(
            "Authentication of generic user failed. " + response.toString());
      }
      final Map<String, NewCookie> genericUserCookies = response.getCookies();
      final StringBuilder sb = new StringBuilder();
      for (final String key : genericUserCookies.keySet()) {
        sb.append(genericUserCookies.get(key));
        sb.append(";");
      }

      genericUserCookie = sb.toString();
      return genericUserCookie;
    }
  }

  /**
   * Creates the file from a List<String>.
   *
   * @param filename the filename
   * @param reportRows the report rows
   * @return the string
   * @throws Exception the exception
   */
  public static File createFile(final String filename,
    final List<String> reportRows) throws Exception {

    // Add results to file
    final File resultFile =
        new File(System.getProperty("java.io.tmpdir") + filename + ".txt");
    LOGGER.info("Created result file: " + resultFile.getAbsolutePath());

    try (final FileWriter writer = new FileWriter(resultFile);) {
      for (String str : reportRows) {
        writer.write(str);
        writer.write(System.getProperty("line.separator"));
      }
    }

    return resultFile;
  }

  /**
   * Zip a file.
   *
   * @param filename the filename
   * @param resultFile the result file
   * @return the file
   * @throws Exception the exception
   */
  public static File zipFile(final String filename, final File resultFile)
    throws Exception {

    // Zip results file
    final File zipFile =
        new File(System.getProperty("java.io.tmpdir") + filename + ".zip");

    try (final FileOutputStream fos = new FileOutputStream(zipFile);
        final ZipOutputStream zipOut =
            new ZipOutputStream(new BufferedOutputStream(fos));
        final FileInputStream fis = new FileInputStream(resultFile);) {

      final ZipEntry ze = new ZipEntry(resultFile.getName());
      LOGGER.info("Zipping the file: " + resultFile.getName());
      zipOut.putNextEntry(ze);
      byte[] tmp = new byte[4 * 1024];
      int size = 0;
      while ((size = fis.read(tmp)) != -1) {
        zipOut.write(tmp, 0, size);
      }

      return zipFile;
    }
  }
}
