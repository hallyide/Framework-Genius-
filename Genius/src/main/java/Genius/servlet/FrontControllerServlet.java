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

        
        String urlCont = url.substring(req.getContextPath().length());

        UrlMethod urlMethod1 = new UrlMethod(MethodeType.valueOf(req.getMethod()), urlCont);

        RouteMapping route = routes.get(urlMethod1);

        Map<String, String[]> requestParams = req.getParameterMap();

        if (route != null) {
            try {
                Object obj;

                // if(springContext != null) {
                //     obj = springContext.getBean(route.getControllerClass());
                // }
                // else{
                obj = route.getControllerClass()
                        .getDeclaredConstructor()
                        .newInstance();
                // }
                Class<?> returnType = route.getMethod().getReturnType();

                Model model = new Model();

                Method mm = route.getMethod();

                Parameter[] params = mm.getParameters();
                Object[] args = new Object[params.length];

                for (int i = 0; i < params.length; i++) {

                    Parameter parameter = params[i];

                    String paramName = parameter.getName();
                    Class<?> paramType = parameter.getType();

                    if (paramType == Fichier.class) {
                        args[i] = construireFichier(req, paramName);
                        continue;
                    }

                    if (paramType == Model.class) {
                        args[i] = model;
                        continue;
                    }

                    if (paramType == HttpServletRequest.class) {
                        args[i] = req;
                        continue;
                    }

                    if (paramType == HttpServletResponse.class) {
                        args[i] = resp;
                        continue;
                    }

                    if (!paramType.isPrimitive()
                            && paramType != String.class
                            && !Number.class.isAssignableFrom(paramType)
                            && paramType != Boolean.class
                            && paramType != Character.class
                            && !paramType.getName().startsWith("java.")) {

                        args[i] = binder.bind(paramType, req);
                        continue;
                    }

                    String valeur = req.getParameter(paramName);

                    if (valeur == null || valeur.isEmpty()) {

                        if (paramType.isPrimitive()) {
                            throw new IllegalArgumentException(
                                    "Paramètre obligatoire manquant : " + paramName
                            );
                        }

                        args[i] = null;
                        continue;
                    }

                    if (paramType == String.class) {
                        args[i] = valeur;
                    } else if (paramType == int.class || paramType == Integer.class) {
                        args[i] = Integer.parseInt(valeur);
                    } else if (paramType == long.class || paramType == Long.class) {
                        args[i] = Long.parseLong(valeur);
                    } else if (paramType == double.class || paramType == Double.class) {
                        args[i] = Double.parseDouble(valeur);
                    } else if (paramType == boolean.class || paramType == Boolean.class) {
                        args[i] = Boolean.parseBoolean(valeur);
                    } else {
                        throw new IllegalArgumentException(
                                "Type non pris en charge : " + paramType.getName()
                        );
                    }
                }

                if (route.getMethod().isAnnotationPresent(ApiREST.class)) {
                    Object result = new Object();

                    resp.setContentType("application/json;charset=UTF-8");

                    if (requestParams == null || requestParams.isEmpty()) {
                        result = route.getMethod().invoke(obj);

                    } else {
                        result = mm.invoke(obj, args);
                    }

                    PrintWriter r = resp.getWriter();
                    if (returnType.equals(String.class)) {
                        r.print(result);
                    } else {
                        r.print(gson.toJson(result));
                    }

                }
                
                if (returnType.equals(ModelAndView.class)) {
                    Object result;

                    // if(this.util.haveParameter(route.getMethod(), WebApplicationContext.class)) {
                    //     if(springContext == null) {
                    //         throw new Exception("SpringContext introuvable");
                    //     }
                    //     result = route.getMethod().invoke(obj, springContext);
                    // } else {
                    result = route.getMethod().invoke(obj,args);

                    // }
                    ModelAndView m = (ModelAndView) result;
                    Map<String, Object> att = m.getAttribute();

                    for (Map.Entry<String, Object> en : att.entrySet()) {
                        req.setAttribute(en.getKey(), en.getValue());
                    }
                    String lien1 = prefix + m.getURL() + suffix;
                    req.getRequestDispatcher(lien1).forward(req, resp);

                } else if (returnType.equals(String.class) && !route.getMethod().isAnnotationPresent(ApiREST.class)) {
                    Object result;
                    
                    result = route.getMethod().invoke(obj, args);

                    Map<String, Object> att = model.getAttribute();
                    if (att != null) {
                        for (Map.Entry<String, Object> en : att.entrySet()) {
                            req.setAttribute(en.getKey(), en.getValue());
                        }
                    }

                    String lien1 = prefix + (String) result + suffix;
                    req.getRequestDispatcher(lien1).forward(req, resp);
                } else if (!(requestParams == null) || !requestParams.isEmpty()) {
                    Object result;
                    result = mm.invoke(obj, args);

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
