/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
package org.ihtsdo.otf.mapping.rest.impl;

import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.ihtsdo.otf.mapping.jpa.services.RootServiceJpa;

/**
 * Listener for shutting down session factory.
 *
 * @see SessionFactoryShutdownEvent
 */
public class SessionFactoryShutdownListener implements ServletContextListener {

  /**
   * Instantiates an empty {@link SessionFactoryShutdownListener}.
   *
   * @throws Exception the exception
   */
  public SessionFactoryShutdownListener() throws Exception {
    super();
    // n/a
  }

  /* see superclass */
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    // n/a
  }

  /* see superclass */
  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    // Close the factory
    try (final LocalService service = new LocalService();) {
      service.getFactory().close();
    } catch (Exception e) {
      // n/a
    }
  }

  /**
   * Accessor for factory.
   */
  private class LocalService extends RootServiceJpa {

    /**
     * Instantiates an empty {@link LocalService}.
     *
     * @throws Exception the exception
     */
    public LocalService() throws Exception {
      super();
      // n/a
    }

    /**
     * Returns the factory.
     *
     * @return the factory
     */
    public EntityManagerFactory getFactory() {
      return factory;
    }
  }

}
