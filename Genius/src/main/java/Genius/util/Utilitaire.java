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
}
