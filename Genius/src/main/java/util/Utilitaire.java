package util;
import annotation.Controller;

import java.util.List;
import java.util.ArrayList;
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

    public List<String> findController(String packageClasse){
        List<String> liste = new ArrayList<>();

        Set<Class<?>> toutesLesClasses = chercherClasse(packageClasse);

        for (Class<?> clazz : toutesLesClasses) {
            if( clazz.isAnnotationPresent(Controller.class)){

                // Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
                // String valeur = controllerAnnotation.value();

                // if (valeur.equals(annotationValue)){
                    liste.add(clazz.getName());
                // }
            } 
        }
        return liste;
    }
}
