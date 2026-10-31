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

                // Vérification de l'authentification avant l'exécution
                if (!checkAuthorization(req, resp, mm)) {
                    return;
                }

                Parameter[] params = mm.getParameters();
                Object[] args = new Object[params.length];

                for (int i = 0; i < params.length; i++) {

                    Parameter parameter = params[i];

                    String paramName = parameter.getName();
                    Class<?> paramType = parameter.getType();
                    
                    if (paramType == GeniusSession.class) {
                        args[i] = GeniusSession.from(req.getSession(true));
                        continue;
                    }

                    if (paramType == Fichier.class) {
                        args[i] = binder.construireFichier(req, paramName);
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

                Object result = invokeController(mm, obj, args, req);

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
    
    private Object invokeController(
        Method method,
        Object controller,
        Object[] args,
        HttpServletRequest req
    ) throws Exception {

        Object result = method.invoke(controller, args);

        HttpSession httpSession = req.getSession(false);

        if (httpSession != null) {
            for (Object arg : args) {
                if (arg instanceof GeniusSession) {
                    ((GeniusSession) arg).flushTo(httpSession);
                }
            }
        }

        return result;
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
    
    private void handleUnauthorized(
            HttpServletRequest req,
            HttpServletResponse resp,
            boolean json
    ) throws IOException {

        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        if (json) {
            resp.setContentType("application/json;charset=UTF-8");

            PrintWriter out = resp.getWriter();

            out.print("{\"status\":401,\"error\":\"Unauthorized\","
                    + "\"message\":\"Authentification requise.\"}");

        } else {
            resp.setContentType("text/html;charset=UTF-8");

            PrintWriter out = resp.getWriter();

            out.print(
                "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'><title>401 Unauthorized</title></head>"
                + "<body>"
                + "<h1>401 - Accès non autorisé</h1>"
                + "<p>Vous devez être connecté pour accéder à cette page.</p>"
                + "</body>"
                + "</html>"
            );
        }
    }
    
    private boolean isJsonRequest(
        HttpServletRequest req,
        Method method
    ) {

        // Une méthode annotée ApiREST attend une réponse JSON
        if (method.isAnnotationPresent(ApiREST.class)) {
            return true;
        }

        // Vérifier le type de réponse accepté par le client
        String accept = req.getHeader("Accept");

        if (accept != null && accept.contains("application/json")) {
            return true;
        }

        // Vérifier les requêtes AJAX
        String requestedWith = req.getHeader("X-Requested-With");

        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    private boolean checkAuthorization(
            HttpServletRequest req,
            HttpServletResponse resp,
            Method method
    ) throws IOException {

        // 1. Récupérer l'annotation Auth
        Auth auth = method.getAnnotation(Auth.class);

        // 2. Si elle n'existe pas sur la méthode,
        //    vérifier l'annotation sur la classe
        if (auth == null) {
            auth = method.getDeclaringClass().getAnnotation(Auth.class);
        }

        // 3. Aucune annotation : accès autorisé
        if (auth == null) {
            return true;
        }

        // 4. Récupérer le nom de l'attribut utilisateur
        String sessionUserName = (String) getServletContext()
                .getAttribute(FrontControllerListener.SESSION_USER_NAME);

        if (sessionUserName == null || sessionUserName.trim().isEmpty()) {
            sessionUserName = "user";
        }

        // 5. Récupérer la session sans en créer une nouvelle
        HttpSession session = req.getSession(false);

        // 6. Vérifier l'existence de la session
        if (session == null) {
            handleUnauthorized(req, resp, isJsonRequest(req, method));
            return false;
        }

        // 7. Vérifier l'existence de l'utilisateur connecté
        Object user = session.getAttribute(sessionUserName);

        if (user == null) {
            handleUnauthorized(req, resp, isJsonRequest(req, method));
            return false;
        }

        // 8. Utilisateur authentifié
        return true;
    }
    
}
