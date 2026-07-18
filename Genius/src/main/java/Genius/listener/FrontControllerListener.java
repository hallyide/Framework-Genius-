package Genius.listener;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.HashMap;
import Genius.util.*;

import org.springframework.web.context.WebApplicationContext;

@WebListener
public class FrontControllerListener implements ServletContextListener {

    public static final String SPRING_ROOT = WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {

            ServletContext context = sce.getServletContext();

            Utilitaire util = new Utilitaire();

            List<Class<?>> controllers = new ArrayList<>();

            Map<UrlMethod, RouteMapping> routes = new HashMap<>();


            util.findController("test",controllers,routes);

            context.setAttribute("routes", routes);

            context.setAttribute(
                "springContext",
                context.getAttribute(SPRING_ROOT)
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
