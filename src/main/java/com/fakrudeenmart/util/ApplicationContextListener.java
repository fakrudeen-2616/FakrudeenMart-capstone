package com.fakrudeenmart.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class ApplicationContextListener implements ServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        LOGGER.info("Starting FakrudeenMart foundation");
        DatabaseConnectionManager manager = DatabaseConnectionManager.getInstance();
        DatabaseInitializer.initialize(manager.getDataSource());
        event.getServletContext().setAttribute(DatabaseConnectionManager.class.getName(), manager);
        LOGGER.info("FakrudeenMart foundation started");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        DatabaseConnectionManager manager = (DatabaseConnectionManager) event.getServletContext()
                .getAttribute(DatabaseConnectionManager.class.getName());
        if (manager != null) {
            manager.close();
        }
    }
}
