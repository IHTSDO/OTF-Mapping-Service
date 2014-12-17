package org.ihtsdo.otf.mapping.services.helpers;

import java.io.File;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Properties;
import java.util.UUID;

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
  public synchronized static Properties getTestConfigProperties() throws Exception {
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
  public static UUID getReleaseUuid(String hash) throws NoSuchAlgorithmException,
    UnsupportedEncodingException {
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
  public static UUID getUuidForString(String name) throws NoSuchAlgorithmException,
    UnsupportedEncodingException {

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
  public static Comparator<String> COMPLEX_MAP_COMPARATOR = new Comparator<String>() {

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
                      // leave out 13th field so it works for complex map too.
                      else {
                        return -1;
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
}
