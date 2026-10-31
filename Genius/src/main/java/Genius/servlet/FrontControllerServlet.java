package Genius.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import Genius.util.*;
import Genius.annotation.*;

// import org.springframework.web.context.WebApplicationContext;
import com.google.gson.Gson;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import Genius.listener.FrontControllerListener;

import java.util.Collection;
import java.lang.reflect.Array;

public class FrontControllerServlet extends HttpServlet {

    private Map<UrlMethod, RouteMapping> routes = new HashMap<>();

    // private WebApplicationContext springContext;
    private Utilitaire util;

    private String prefix;
    private String suffix;
    private Gson gson = new Gson();
    private ParamBinder binder = new ParamBinder();

    public void init() throws ServletException {
        this.prefix = getInitParameter("prefix");
        this.suffix = getInitParameter("suffix");

        if (this.prefix == null) {
            this.prefix = "";
        }
        if (this.suffix == null) {
            this.suffix = ".jsp";
        }

        try {
            this.util = new Utilitaire();
            this.routes = (Map<UrlMethod, RouteMapping>) getServletContext().getAttribute("routes");
            // springContext = (WebApplicationContext)getServletContext().getAttribute("springContext");
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }

    protected void processRequest(HttpServletRequest req,
            HttpServletResponse resp)
            throws ServletException, IOException {

        output(req, resp);
    }

    private void output(HttpServletRequest req,
            HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getRequestURI();

        PrintWriter out = resp.getWriter();

        boolean lien = false;
        String urlCont = url.substring(req.getContextPath().length());

        UrlMethod urlMethod1 = new UrlMethod(MethodeType.valueOf(req.getMethod()), urlCont);

        RouteMapping route = routes.get(urlMethod1);

        Map<String, String[]> requestParams = req.getParameterMap();

        if (route != null) {
            try {
                // Object obj;

                // if(springContext != null) {
                //     obj = springContext.getBean(route.getControllerClass());
                // }
                // else{
                // obj = route.getControllerClass()
                //         .getDeclaredConstructor()
                //         .newInstance();
                // }
                Class<?> controllerClass = route.getControllerClass();

                Object obj = util.getControllerInstance(
                        req.getServletContext(),
                        controllerClass
                );

                Class<?> returnType = route.getMethod().getReturnType();

                Model model = new Model();

                Method mm = route.getMethod();

                // Vérification de l'authentification avant l'exécution
                if (!util.checkAuthorization(req, resp, mm, 
                (String) getServletContext().getAttribute(FrontControllerListener.SESSION_USER_NAME), 
                (String) getServletContext().getAttribute(FrontControllerListener.SESSION_ROLE_NAME))) {
                    return;
                }

                Parameter[] params = mm.getParameters();
                
                Object[] args = util.construireArguments(
                        mm,
                        req,
                        resp,
                        model
                );

                
                
                Object result = util.invokeController(mm, obj, args, req);

                if (mm.isAnnotationPresent(ApiREST.class)) {

                    resp.setContentType("application/json;charset=UTF-8");

                    PrintWriter r = resp.getWriter();

                    if (returnType.equals(String.class)) {
                        r.print(result);
                    } else {
                        r.print(gson.toJson(result));
                    }

                } else if (returnType.equals(ModelAndView.class)) {

                    ModelAndView m = (ModelAndView) result;

                    for (Map.Entry<String, Object> en : m.getAttribute().entrySet()) {
                        req.setAttribute(en.getKey(), en.getValue());
                    }

                    String lien1 = prefix + m.getURL() + suffix;

                    req.getRequestDispatcher(lien1).forward(req, resp);

                } else if (returnType.equals(String.class)) {

                    for (Map.Entry<String, Object> en : model.getAttribute().entrySet()) {
                        req.setAttribute(en.getKey(), en.getValue());
                    }

                    String lien1 = prefix + (String) result + suffix;

                    req.getRequestDispatcher(lien1).forward(req, resp);
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new ServletException(e);

            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req,
            HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req,
            HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/html;charset=UTF-8");
        processRequest(req, resp);
    }
    
}
