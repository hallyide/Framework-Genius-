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
    
    private List<String> listContr;
    private Utilitaire util;

    
    private Map<UrlMethod, RouteMapping> routes = new HashMap<>();

    // private WebApplicationContext springContext;
    private Utilitaire util;

    private String prefix;
    private String suffix;
    private Gson gson = new Gson();

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


                if (route.getMethod().isAnnotationPresent(ApiREST.class)) {
                    Object result = new Object();

                    resp.setContentType("application/json;charset=UTF-8");

                    result = route.getMethod().invoke(obj);

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
                    result = route.getMethod().invoke(obj);

                    // }
                    ModelAndView m = (ModelAndView) result;
                    Map<String, Object> att = m.getAttribute();

                    for (Map.Entry<String, Object> en : att.entrySet()) {
                        req.setAttribute(en.getKey(), en.getValue());
                    }
                    String lien1 = prefix + m.getURL() + suffix;
                    req.getRequestDispatcher(lien1).forward(req, resp);

                } else if (returnType.equals(String.class) && !route.getMethod().isAnnotationPresent(ApiREST.class)) {
                    Model model = new Model();
                    Object result;

                    List<Object> args = new ArrayList<>();

                    for (Class<?> c : route.getMethod().getParameterTypes()) {

                        if (c.equals(Model.class)) {
                            args.add(model);
                        }

                        // else if(c.equals(WebApplicationContext.class)){
                        //     if(springContext == null)
                        //         throw new Exception("SpringContext introuvable");
                        //     args.add(springContext);
                        // }
                    }

                    result = route.getMethod().invoke(obj, args.toArray());

                    Map<String, Object> att = model.getAttribute();
                    if (att != null) {
                        for (Map.Entry<String, Object> en : att.entrySet()) {
                            req.setAttribute(en.getKey(), en.getValue());
                        }
                    }

                    String lien1 = prefix + (String) result + suffix;
                    req.getRequestDispatcher(lien1).forward(req, resp);
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new ServletException(e);

            }
        }

        // if(!lien) {
        //     out.println("Voila les liens existants : ");
        //     boolean hasMappedMethod = false;
        //     for(UrlMethod urlMethod : routes.keySet()) {
        //         RouteMapping routeM = routes.get(urlMethod);
        //         hasMappedMethod = true;
        //         out.println("<br>URL existant : " + urlMethod.getPath() + ", type : " + urlMethod.getMethod() + " Classe : " +routeM.getControllerClass().toString()+", methode : "+ routeM.getMethod().getName());
        //     }
        //     if (!hasMappedMethod) {
        //         out.println("<br>Aucune methode annotee @UrlMapping");
        //     }
        // }
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
