package util;
import annotation.Controller;
import annotation.UrlMapping;

import java.util.List;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.Set;

public class Utilitaire {
    public Utilitaire(){};

    public Set<Class<?>> chercherClasse(String packageClasse) {

        Reflections reflections = new Reflections(packageClasse, Scanners.SubTypes.filterResultsBy(s -> true));

        Set<Class<?>> toutesLesClasses = reflections.getSubTypesOf(Object.class);

        return toutesLesClasses;
    }

    public List<Class<?>> findController(String packageClasse){
        List<Class<?>> liste = new ArrayList<>();

        Set<Class<?>> toutesLesClasses = chercherClasse(packageClasse);

        for (Class<?> clazz : toutesLesClasses) {
            if( clazz.isAnnotationPresent(Controller.class)){
                liste.add(clazz);
            } 
        }
        return liste;
    }

    public Map<String, RouteMapping> findRoutes(List<Class<?>> controllerClasses) throws Exception {
        Map<String, RouteMapping> result = new HashMap<>();

        if (controllerClasses == null) {
            return result;
        }

        for (Class<?> clazz : controllerClasses) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(UrlMapping.class)) {
                    UrlMapping annotation = method.getAnnotation(UrlMapping.class);
                    if (result.containsKey(annotation.value())) {
                        throw new Exception("La clé '" + annotation.value() + "' existe déjà.");
                    }
                    result.put(annotation.value(),new RouteMapping(clazz, method));
                }
            }
        }

        return result;
    }

    // public List<RouteMapping> findRoutes(List<Class<?>> controllerClasses) {
    //     List<RouteMapping> result = new ArrayList<>();

    //     if (controllerClasses == null) {
    //         return result;
    //     }

    //     for (Class<?> clazz : controllerClasses) {
    //         for (Method method : clazz.getDeclaredMethods()) {
    //             if (method.isAnnotationPresent(UrlMapping.class)) {
    //                 UrlMapping annotation = method.getAnnotation(UrlMapping.class);
    //                 result.add(new RouteMapping(annotation.value(), clazz, method));
    //             }
    //         }
    //     }

    //     return result;
    // }
}
