package org.ihtsdo.otf.mapping.jpa.services;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.NoResultException;

import org.apache.log4j.Logger;
import org.ihtsdo.otf.mapping.helpers.LocalException;
import org.ihtsdo.otf.mapping.helpers.MapUserList;
import org.ihtsdo.otf.mapping.helpers.MapUserListJpa;
import org.ihtsdo.otf.mapping.helpers.MapUserRole;
import org.ihtsdo.otf.mapping.helpers.PfsParameter;
import org.ihtsdo.otf.mapping.jpa.MapUserJpa;
import org.ihtsdo.otf.mapping.jpa.MapUserPreferencesJpa;
import org.ihtsdo.otf.mapping.model.MapUser;
import org.ihtsdo.otf.mapping.model.MapUserPreferences;
import org.ihtsdo.otf.mapping.services.MappingService;
import org.ihtsdo.otf.mapping.services.SecurityService;
import org.ihtsdo.otf.mapping.services.helpers.ConfigUtility;
import org.ihtsdo.otf.mapping.services.helpers.SecurityServiceHandler;

/**
 * Reference implementation of the {@link SecurityService}.
 */
public class SecurityServiceJpa extends RootServiceJpa implements
    SecurityService {

  /** The token userName . */
  private static Map<String, String> tokenMapUsernameMap = Collections
      .synchronizedMap(new HashMap<String, String>());

  /** The token login time . */
  private static Map<String, Date> tokenTimeoutMap = Collections
      .synchronizedMap(new HashMap<String, Date>());

  /** The handler. */
  private static SecurityServiceHandler handler = null;

  /** The timeout. */
  private static int timeout;

  /**
   * Instantiates an empty {@link SecurityServiceJpa}.
   *
   * @throws Exception the exception
   */
  public SecurityServiceJpa() throws Exception {
    super();
  }

  /* see superclass */
  @Override
  public MapUser authenticate(String userName, String password)
    throws Exception {
    // Check userName and password are not null
    if (userName == null || userName.isEmpty())
      throw new LocalException("Invalid userName: null");
    if (password == null || password.isEmpty())
      throw new LocalException("Invalid password: null");

    Properties config = ConfigUtility.getConfigProperties();

    if (handler == null) {
      timeout = Integer.valueOf(config.getProperty("security.timeout"));
      String handlerName = config.getProperty("security.handler");
      handler =
          ConfigUtility.newStandardHandlerInstanceWithConfiguration(
              "security.handler", handlerName, SecurityServiceHandler.class);
    }

    //
    // Call the security service
    //
    // handle guest user unless
    MapUser authMapUser = handler.authenticate(userName, password);
    return authHelper(authMapUser);
  }

  /**
   * Auth helper.
   *
   * @param authMapUser the auth user
   * @return the user
   * @throws Exception the exception
   */
  private MapUser authHelper(MapUser authMapUser) throws Exception {
    if (authMapUser == null)
      return null;

    // check if authenticated user exists
    MapUser userFound = getMapUser(authMapUser.getUserName());

    // if user was found, update to match settings
    Long userId = null;
    if (userFound != null) {
      handleLazyInit(userFound);

      Logger.getLogger(getClass()).info("update");
      userFound.setEmail(authMapUser.getEmail());
      userFound.setName(authMapUser.getName());
      userFound.setUserName(authMapUser.getUserName());
      userFound.setApplicationRole(authMapUser.getApplicationRole());

      updateMapUser(userFound);
      // if (userFound.getUserPreferences() == null) {
      // MapUserPreferences newMapUserPreferences = new MapUserPreferencesJpa();
      // newMapUserPreferences.setUser(userFound);
      // addMapUserPreferences(newMapUserPreferences);
      // }
      userId = userFound.getId();
    }
    // if MapUser not found, create one for our use
    else {
      Logger.getLogger(getClass()).info("add");
      MapUser newMapUser = new MapUserJpa();
      newMapUser.setEmail(authMapUser.getEmail());
      newMapUser.setName(authMapUser.getName());
      newMapUser.setUserName(authMapUser.getUserName());
      newMapUser.setApplicationRole(authMapUser.getApplicationRole());
      newMapUser = addMapUser(newMapUser);

      MapUserPreferences newMapUserPreferences = new MapUserPreferencesJpa();
      newMapUserPreferences.setMapUser(newMapUser);
      newMapUserPreferences.setLastLogin(new Date().getTime());
      newMapUserPreferences.setLastMapProjectId(0L);
      newMapUserPreferences.setNotifiedByEmail(false);
      addMapUserPreferences(newMapUserPreferences);
      userId = newMapUser.getId();
    }
    manager.clear();

    // Generate application-managed token
    String token = handler.computeTokenForUser(authMapUser.getUserName());
    tokenMapUsernameMap.put(token, authMapUser.getUserName());
    tokenTimeoutMap.put(token, new Date(new Date().getTime() + timeout));

    Logger.getLogger(getClass()).debug(
        "MapUser = " + authMapUser.getUserName() + ", " + authMapUser);

    // Reload the user to populate MapUserPreferences
    final MapUser result = getMapUser(userId);
    handleLazyInit(result);
    result.setAuthToken(token);

    return result;
  }

  /* see superclass */
  @Override
  public void logout(String authToken) throws Exception {
    tokenMapUsernameMap.remove(authToken);
    tokenTimeoutMap.remove(authToken);
  }

  /* see superclass */
  @Override
  public String getUsernameForToken(String authToken) throws Exception {
    // use guest user for null auth token
    if (authToken == null)
      throw new LocalException(
          "Attempt to access a service without an AuthToken, the user is likely not logged in.");

    // handle guest user unless
    if (authToken.equals("guest")
        && "false".equals(ConfigUtility.getConfigProperties().getProperty(
            "security.guest.disabled"))) {
      return "guest";
    }

    // Replace double quotes in auth token.
    final String parsedToken = authToken.replace("\"", "");

    // Check auth token against the userName map
    if (tokenMapUsernameMap.containsKey(parsedToken)) {
      String userName = tokenMapUsernameMap.get(parsedToken);

      // Validate that the user has not timed out.
      if (handler.timeoutUser(userName)) {

        if (tokenTimeoutMap.get(parsedToken) == null) {
          throw new LocalException("No login timeout set for authToken.");
        }

        if (tokenTimeoutMap.get(parsedToken).before(new Date())) {
          throw new LocalException(
              "AuthToken has expired. Please reload and log in again.");
        }
        tokenTimeoutMap.put(parsedToken, new Date(new Date().getTime()
            + timeout));
      }
      return userName;
    } else {
      throw new LocalException("AuthToken does not have a valid userName.");
    }
  }

  /* see superclass */
  @Override
  public MapUserRole getApplicationRoleForToken(String authToken)
    throws Exception {
    if (authToken == null) {
      throw new LocalException(
          "Attempt to access a service without an AuthToken, the user is likely not logged in.");
    }
    // Handle "guest" user
    if (authToken.equals("guest")
        && "false".equals(ConfigUtility.getConfigProperties().getProperty(
            "security.guest.disabled"))) {
      return MapUserRole.VIEWER;
    }

    final String parsedToken = authToken.replace("\"", "");
    final String userName = getUsernameForToken(parsedToken);

    // check for null userName
    if (userName == null) {
      throw new LocalException("Unable to find user for the AuthToken");
    }
    final MapUser user = getMapUser(userName.toLowerCase());
    if (user == null) {
      return MapUserRole.VIEWER;
      // throw new
      // LocalException("Unable to obtain user information for userName = " +
      // userName);
    }
    return user.getApplicationRole();
  }

  /* see superclass */
  @Override
  public MapUserRole getMapProjectRoleForToken(String authToken, Long projectId)
    throws Exception {
    if (authToken == null) {
      throw new LocalException(
          "Attempt to access a service without an AuthToken, the user is likely not logged in.");
    }
    if (projectId == null) {
      throw new Exception("Unexpected null project id");
    }

    final String userName = getUsernameForToken(authToken);

    final MappingService service = new MappingServiceJpa();
    try {
      for (final MapUser user : service.getMapProject(projectId).getMapLeads()) {
        if (user.getUserName().equals(userName)) {
          return MapUserRole.LEAD;
        }
      }
      for (final MapUser user : service.getMapProject(projectId)
          .getMapSpecialists()) {
        if (user.getUserName().equals(userName)) {
          return MapUserRole.SPECIALIST;
        }
      }
    } catch (Exception e) {
      throw e;
    } finally {
      service.close();
    }
    return MapUserRole.NONE;
  }

  /* see superclass */
  @Override
  public MapUser getMapUser(Long id) throws Exception {
    return manager.find(MapUserJpa.class, id);
  }

  /* see superclass */
  @Override
  public MapUser getMapUser(String userName) throws Exception {
    final javax.persistence.Query query =
        manager
            .createQuery("select u from MapUserJpa u where userName = :userName");
    query.setParameter("userName", userName);
    try {
      final List<?> list = query.getResultList();
      if (list.isEmpty()) {
        return null;
      }
      return (MapUser) list.iterator().next();
    } catch (NoResultException e) {
      return null;
    }
  }

  /* see superclass */
  @Override
  public MapUser addMapUser(MapUser user) {
    Logger.getLogger(getClass()).debug("Security Service - add user " + user);
    try {
      if (getTransactionPerOperation()) {
        tx = manager.getTransaction();
        tx.begin();
        manager.persist(user);
        tx.commit();
      } else {
        manager.persist(user);
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }

    return user;
  }

  /* see superclass */
  @Override
  public void removeMapUser(Long id) {
    Logger.getLogger(getClass()).debug("Security Service - remove user " + id);
    tx = manager.getTransaction();
    // retrieve this user
    final MapUser mu = manager.find(MapUserJpa.class, id);
    try {
      if (getTransactionPerOperation()) {
        tx.begin();
        if (manager.contains(mu)) {
          manager.remove(mu);
        } else {
          manager.remove(manager.merge(mu));
        }
        tx.commit();

      } else {
        if (manager.contains(mu)) {
          manager.remove(mu);
        } else {
          manager.remove(manager.merge(mu));
        }
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }

  }

  /* see superclass */
  @Override
  public void updateMapUser(MapUser user) {
    Logger.getLogger(getClass())
        .debug("Security Service - update user " + user);
    try {
      if (getTransactionPerOperation()) {
        tx = manager.getTransaction();
        tx.begin();
        manager.merge(user);
        tx.commit();
      } else {
        manager.merge(user);
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public MapUserList getMapUsers() {
    javax.persistence.Query query =
        manager.createQuery("select u from MapUserJpa u");
    final List<MapUser> m = query.getResultList();
    final MapUserListJpa mapMapUserList = new MapUserListJpa();
    mapMapUserList.setMapUsers(m);
    mapMapUserList.setTotalCount(m.size());
    return mapMapUserList;
  }

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public MapUserList findMapUsersForQuery(String query, PfsParameter pfs)
    throws Exception {
    Logger.getLogger(getClass()).info(
        "Security Service - find users " + query + ", pfs= " + pfs);

    int[] totalCt = new int[1];
    final List<MapUser> list =
        (List<MapUser>) getQueryResults(query == null || query.isEmpty()
            ? "id:[* TO *]" : query, MapUserJpa.class, MapUserJpa.class, pfs,
            totalCt);
    final MapUserList result = new MapUserListJpa();
    result.setTotalCount(totalCt[0]);
    result.setMapUsers(list);
    for (final MapUser user : result.getMapUsers()) {
      handleLazyInit(user);
    }
    return result;
  }

  /* see superclass */
  @Override
  public MapUserPreferences addMapUserPreferences(
    MapUserPreferences userPreferences) {
    Logger.getLogger(getClass()).debug(
        "Security Service - add user preferences " + userPreferences);
    try {
      if (getTransactionPerOperation()) {
        tx = manager.getTransaction();
        tx.begin();
        manager.persist(userPreferences);
        tx.commit();
      } else {
        manager.persist(userPreferences);
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }

    return userPreferences;
  }

  /* see superclass */
  @Override
  public void removeMapUserPreferences(Long id) {
    Logger.getLogger(getClass()).debug(
        "Security Service - remove user preferences " + id);
    tx = manager.getTransaction();
    // retrieve this user
    final MapUserPreferences mu = manager.find(MapUserPreferencesJpa.class, id);
    try {
      if (getTransactionPerOperation()) {
        tx.begin();
        if (manager.contains(mu)) {
          manager.remove(mu);
        } else {
          manager.remove(manager.merge(mu));
        }
        tx.commit();

      } else {
        if (manager.contains(mu)) {
          manager.remove(mu);
        } else {
          manager.remove(manager.merge(mu));
        }
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }

  }

  /* see superclass */
  @Override
  public void updateMapUserPreferences(MapUserPreferences userPreferences) {
    Logger.getLogger(getClass()).debug(
        "Security Service - update user preferences " + userPreferences);
    try {
      if (getTransactionPerOperation()) {
        tx = manager.getTransaction();
        tx.begin();
        manager.merge(userPreferences);
        tx.commit();
      } else {
        manager.merge(userPreferences);
      }
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  /**
   * Handle lazy init.
   *
   * @param user the user
   */
  @Override
  public void handleLazyInit(MapUser user) {
    // n/a - no objects connected
  }
}
