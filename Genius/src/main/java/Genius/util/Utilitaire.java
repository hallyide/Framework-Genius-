package Genius.util;
import Genius.annotation.Controller;
import Genius.annotation.UrlMapping;

import java.util.List;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.Set;
import java.lang.reflect.Parameter;


import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Collection;
import java.lang.reflect.Array;

import Genius.annotation.*;
import Genius.listener.FrontControllerListener;

public class Utilitaire {
    public Utilitaire(){};

    public Set<Class<?>> chercherClasse(String packageClasse) {

        Reflections reflections = new Reflections(packageClasse, Scanners.SubTypes.filterResultsBy(s -> true));

        Set<Class<?>> toutesLesClasses = reflections.getSubTypesOf(Object.class);

        return toutesLesClasses;
    }

    public void findController(String packageClasse,List<Class<?>> liste,Map<UrlMethod, RouteMapping> result) throws Exception {
        Set<Class<?>> toutesLesClasses = chercherClasse(packageClasse);

        for (Class<?> clazz : toutesLesClasses) {
            if( clazz.isAnnotationPresent(Controller.class)){
                liste.add(clazz);

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(UrlMapping.class)) {
                        UrlMapping annotation = method.getAnnotation(UrlMapping.class);
                        UrlMethod urlMethod = new UrlMethod(annotation.method(), annotation.path());

                        if (result.containsKey(urlMethod)) {
                            throw new Exception("La clé '" + urlMethod + "' existe déjà.");
                        }
                        
                        result.put(urlMethod, new RouteMapping(clazz, method));
                    }
                }
                
            } 
        }
    }

    public int haveNumParameter(Method method, Class<?> param) {
        Class<?>[] cl = method.getParameterTypes();
        for (int i = 0; i < cl.length; i++) {
            if (cl[i].equals(param)){
                return i;
            }
        }
        return -1;
    }

    public boolean haveParameter(Method method, Class<?> param) {
        for(Class<?> c : method.getParameterTypes()) {

            if(c.equals(param))
                return true;
        }

        return false;
    }

    // public void arguments(Object[] args, Parameter[] params) {
    //     for (int i = 0; i < params.length; i++) {
    //         Parameter pppppppp = params[i];
    //         String paramName = pppppppp.getName();
    //         Class<?> paramType = pppppppp.getType();

    //         String value = req.getParameter(paramName);

    //         if (value == null || value.isEmpty()) {
                

    //             if (paramType.isPrimitive()) {
    //                 throw new IllegalArgumentException(
    //                         "Paramètre obligatoire manquant : " + paramName
    //                 );
    //             }
                    
    //             if (paramType.equals(Model.class)) {

    //                 args[i] = model;
    //             // } 
    //             // else if(paramType.equals(WebApplicationContext.class)){
    //             //     if(springContext.equals(null))
    //         //         throw new Exception("SpringContext introuvable");
    //             //     args[i] = springContext;
    //             } else {
    //                 args[i] = null;
    //             }
    //         } else {
    //             if (paramType.equals(String.class)) {
    //                 args[i] = value;
    //             } else if (paramType.equals(int.class) || paramType.equals(Integer.class)) {
    //                 args[i] = Integer.parseInt(value);
    //             } else if (paramType.equals(long.class) || paramType.equals(Long.class)) {
    //                 args[i] = Long.parseLong(value);
    //             } else if (paramType.equals(double.class) || paramType.equals(Double.class)) {
    //                 args[i] = Double.parseDouble(value);
    //             } else if (paramType.equals(boolean.class) || paramType.equals(Boolean.class)) {
    //                 args[i] = Boolean.parseBoolean(value);
    //             } else {
    //                 args[i] = value;
    //             }
    //         }
    //     }
    // }
    
    public void handleUnauthorized(
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
    
    public boolean isJsonRequest(
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
    
    public boolean checkAuthorization(
            HttpServletRequest req,
            HttpServletResponse resp,
            Method method,
            String sessionUserName,
            String sessionRoleName
    ) throws IOException {

        // 1. Récupérer Auth sur la méthode
        Auth auth = method.getAnnotation(Auth.class);

        // 2. Si absente, récupérer Auth sur la classe
        if (auth == null) {
            auth = method.getDeclaringClass().getAnnotation(Auth.class);
        }

        // 3. Aucune annotation : accès autorisé
        if (auth == null) {
            return true;
        }

        // 4. Récupérer le nom de l'attribut utilisateur
        // String sessionUserName = 

        if (sessionUserName == null || sessionUserName.trim().isEmpty()) {
            sessionUserName = "user";
        }

        // 5. Récupérer la session sans en créer une nouvelle
        HttpSession session = req.getSession(false);

        // 6. Vérifier l'authentification
        if (session == null || session.getAttribute(sessionUserName) == null) {

            handleUnauthorized(
                req,
                resp,
                isJsonRequest(req, method)
            );

            return false;
        }

        // 7. Récupérer les rôles requis dans l'annotation
        String[] requiredRoles = auth.roles();

        // 8. Aucun rôle requis : authentification suffisante
        if (requiredRoles.length == 0) {
            return true;
        }

        // 9. Récupérer le nom de l'attribut contenant les rôles
        // String sessionRoleName = 

        if (sessionRoleName == null || sessionRoleName.trim().isEmpty()) {
            sessionRoleName = "roles";
        }

        // 10. Récupérer les rôles de l'utilisateur
        Object userRoles = session.getAttribute(sessionRoleName);

        // 11. Vérifier si l'utilisateur possède un rôle autorisé
        if (!hasRequiredRole(userRoles, requiredRoles)) {

            handleForbidden(
                req,
                resp,
                isJsonRequest(req, method)
            );

            return false;
        }

        // 12. Authentification et autorisation validées
        return true;
    }

    public void handleForbidden(
        HttpServletRequest req,
        HttpServletResponse resp,
        boolean json
    ) throws IOException {

        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);

        if (json) {
            resp.setContentType("application/json;charset=UTF-8");

            PrintWriter out = resp.getWriter();

            out.print(
                "{\"status\":403,"
                + "\"error\":\"Forbidden\","
                + "\"message\":\"Vous ne possédez pas le rôle nécessaire pour accéder à cette ressource.\"}"
            );

        } else {
            resp.setContentType("text/html;charset=UTF-8");

            PrintWriter out = resp.getWriter();

            out.print(
                "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'><title>403 Forbidden</title></head>"
                + "<body>"
                + "<h1>403 - Accès interdit</h1>"
                + "<p>Vous êtes connecté, mais vous ne possédez pas les autorisations nécessaires.</p>"
                + "</body>"
                + "</html>"
            );
        }
    }

    public boolean hasRequiredRole(
        Object userRoles,
        String[] requiredRoles
    ) {

        // Aucun rôle requis : authentification suffisante
        if (requiredRoles == null || requiredRoles.length == 0) {
            return true;
        }

        if (userRoles == null) {
            return false;
        }

        // Cas 1 : un seul rôle sous forme String
        if (userRoles instanceof String) {

            String role = (String) userRoles;

            for (String requiredRole : requiredRoles) {
                if (requiredRole.equals(role)) {
                    return true;
                }
            }

            return false;
        }

        // Cas 2 : plusieurs rôles sous forme Collection ou List
        if (userRoles instanceof Collection<?>) {

            Collection<?> roles = (Collection<?>) userRoles;

            for (Object role : roles) {

                if (role == null) {
                    continue;
                }

                for (String requiredRole : requiredRoles) {

                    if (requiredRole.equals(role.toString())) {
                        return true;
                    }
                }
            }

            return false;
        }

        // Cas 3 : plusieurs rôles sous forme de tableau
        if (userRoles.getClass().isArray()) {

            int length = Array.getLength(userRoles);

            for (int i = 0; i < length; i++) {

                Object role = Array.get(userRoles, i);

                if (role == null) {
                    continue;
                }

                for (String requiredRole : requiredRoles) {

                    if (requiredRole.equals(role.toString())) {
                        return true;
                    }
                }
            }

            return false;
        }

        // Type non pris en charge
        return false;
    }

    public boolean estObjetComplexe(Class<?> paramType) {

        return !paramType.isPrimitive()
                && paramType != String.class
                && !Number.class.isAssignableFrom(paramType)
                && paramType != Boolean.class
                && paramType != Character.class
                && !paramType.isEnum()
                && !paramType.getName().startsWith("java.");
    }

    public Object convertirValeur(String valeur, Class<?> paramType) {

        if (paramType == String.class) {
            return valeur;
        }

        if (paramType == int.class || paramType == Integer.class) {
            return Integer.parseInt(valeur);
        }

        if (paramType == long.class || paramType == Long.class) {
            return Long.parseLong(valeur);
        }

        if (paramType == double.class || paramType == Double.class) {
            return Double.parseDouble(valeur);
        }

        if (paramType == float.class || paramType == Float.class) {
            return Float.parseFloat(valeur);
        }

        if (paramType == short.class || paramType == Short.class) {
            return Short.parseShort(valeur);
        }

        if (paramType == byte.class || paramType == Byte.class) {
            return Byte.parseByte(valeur);
        }

        if (paramType == boolean.class || paramType == Boolean.class) {
            return Boolean.parseBoolean(valeur);
        }

        if (paramType == char.class || paramType == Character.class) {
            if (valeur.length() != 1) {
                throw new IllegalArgumentException(
                        "Valeur invalide pour un caractère : " + valeur
                );
            }
            return valeur.charAt(0);
        }

        // Gestion des Enum
        if (paramType.isEnum()) {
            return Enum.valueOf(
                    (Class<? extends Enum>) paramType,
                    valeur
            );
        }

        throw new IllegalArgumentException(
                "Type non pris en charge : " + paramType.getName()
        );
    }

    public Object[] construireArguments(
        Method method,
        HttpServletRequest req,
        HttpServletResponse resp,
        Model model
    ) throws Exception {

        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];

        ParamBinder binder = new ParamBinder();

        for (int i = 0; i < params.length; i++) {

            Parameter parameter = params[i];

            String paramName = parameter.getName();
            Class<?> paramType = parameter.getType();

            // GeniusSession
            if (paramType == GeniusSession.class) {
                args[i] = GeniusSession.from(req.getSession(true));
                continue;
            }

            // Fichier uploadé
            if (paramType == Fichier.class) {
                args[i] = binder.construireFichier(req, paramName);
                continue;
            }

            // Model
            if (paramType == Model.class) {
                args[i] = model;
                continue;
            }

            // HttpServletRequest
            if (paramType == HttpServletRequest.class) {
                args[i] = req;
                continue;
            }

            // HttpServletResponse
            if (paramType == HttpServletResponse.class) {
                args[i] = resp;
                continue;
            }

            // Objets complexes : Personne, Artiste, etc.
            if (estObjetComplexe(paramType)) {
                args[i] = binder.bind(paramType, req);
                continue;
            }

            // Paramètres simples
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

            args[i] = convertirValeur(valeur, paramType);
        }

        return args;
    }

    public Object invokeController(
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

    public Object getControllerInstance(
        ServletContext context,
        Class<?> controllerClass
    ) throws Exception {

        Map<Class<?>, Object> controllerList =
                (Map<Class<?>, Object>) context.getAttribute("controllersInstance");

        if (controllerList == null) {
            throw new IllegalStateException(
                    "La liste des instances de contrôleurs est introuvable."
            );
        }

        // 1. Vérifier si le contrôleur existe déjà
        Object instance = controllerList.get(controllerClass);

        if (instance != null) {
            return instance;
        }

        // 2. Créer une nouvelle instance
        instance = controllerClass.getDeclaredConstructor().newInstance();

        // 3. Ajouter l'instance dans la HashMap
        controllerList.put(controllerClass, instance);

        // 4. Retourner l'instance
        return instance;
    }

}
