package com.apt;


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ServerInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Wait! setting up the ranker");
        Ranker ranker = new Ranker();
        System.out.println("Done setting the ranker");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
