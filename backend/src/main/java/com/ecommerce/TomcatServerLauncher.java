package com.ecommerce;

import com.ecommerce.config.RootConfig;
import com.ecommerce.config.WebMvcConfig;
import jakarta.servlet.MultipartConfigElement;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.File;

/**
 * Embedded Apache Tomcat 10.1 Launcher for running the Spring MVC + Hibernate
 * application directly via Maven (`mvn exec:java`) or from an IDE without
 * requiring a separate manual Tomcat installation. Also packages cleanly as a WAR.
 */
public class TomcatServerLauncher {

    private static final Logger log = LoggerFactory.getLogger(TomcatServerLauncher.class);

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080"));

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector(); // Initialize default HTTP connector

        File docBase = new File(System.getProperty("java.io.tmpdir"), "ecommerce-tomcat-docbase");
        if (!docBase.exists()) {
            docBase.mkdirs();
        }

        Context ctx = tomcat.addContext("", docBase.getAbsolutePath());
        ctx.setParentClassLoader(TomcatServerLauncher.class.getClassLoader());
        WebappLoader loader = new WebappLoader();
        loader.setDelegate(true);
        ctx.setLoader(loader);

        // Single unified Spring WebApplicationContext registering both RootConfig & WebMvcConfig
        AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();
        appContext.setClassLoader(TomcatServerLauncher.class.getClassLoader());
        appContext.register(RootConfig.class, WebMvcConfig.class);

        DispatcherServlet dispatcherServlet = new DispatcherServlet(appContext);
        Wrapper wrapper = Tomcat.addServlet(ctx, "dispatcher", dispatcherServlet);
        wrapper.setLoadOnStartup(1);
        wrapper.setMultipartConfigElement(new MultipartConfigElement(
                System.getProperty("java.io.tmpdir"),
                5 * 1024 * 1024L,
                10 * 1024 * 1024L,
                0
        ));
        ctx.addServletMappingDecoded("/", "dispatcher");

        tomcat.start();
        log.info("=================================================================");
        log.info("E-Commerce Engine with Automated Order Tracking started!");
        log.info("Backend API Base URL : http://localhost:{}/api", port);
        log.info("=================================================================");
        tomcat.getServer().await();
    }
}
