package Genius.util;

import jakarta.servlet.http.HttpServletRequest; // ou javax.servlet selon ta version
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParamBinder {

    /** Types qu'on lit directement depuis un seul paramètre */
    public static boolean isSimple(Class<?> t) {
        return t.isPrimitive()
                || t.equals(String.class)
                || Number.class.isAssignableFrom(t)
                || t.equals(Boolean.class)
                || t.equals(Character.class)
                || t.equals(LocalDate.class);
    }

    /** Point d'entrée : objet, tableau ou type simple */
    public static Object bind(HttpServletRequest req, String name, Class<?> type, boolean allowNoPrefix)
            throws Exception {
        if (type.isArray()) {
            return bindArray(req, name, type.getComponentType());
        }
        if (isSimple(type)) {
            return convert(req.getParameter(name), type);
        }
        return bindObject(req, name, type, allowNoPrefix);
    }

    /** Remplit un objet à partir des paramètres de la requête */
    private static Object bindObject(HttpServletRequest req, String prefix, Class<?> type, boolean allowNoPrefix)
            throws Exception {

        Object instance = type.getDeclaredConstructor().newInstance();

        for (Field f : type.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;

            String fieldName = f.getName();
            Class<?> fieldType = f.getType();
            String key = prefix + "." + fieldName;

            Object value = null;
            boolean found = false;

            if (fieldType.isArray()) {
                String k = hasParam(req, key) ? key : (allowNoPrefix && hasParam(req, fieldName) ? fieldName : null);
                if (k != null) { value = bindArray(req, k, fieldType.getComponentType()); found = true; }

            } else if (isSimple(fieldType)) {
                String raw = req.getParameter(key);
                if (raw == null && allowNoPrefix) raw = req.getParameter(fieldName);
                if (raw != null) { value = convert(raw, fieldType); found = true; }

            } else { // objet imbriqué
                if (hasParam(req, key)) {
                    value = bindObject(req, key, fieldType, false);
                    found = true;
                }
            }

            if (found) setValue(instance, f, value);
        }
        return instance;
    }

    /** Tableaux : String[], int[], Personne[] ... */
    private static Object bindArray(HttpServletRequest req, String name, Class<?> comp) throws Exception {

        // tableau de types simples : ?tags=a&tags=b  (ou name="tags[]")
        if (isSimple(comp)) {
            String[] values = req.getParameterValues(name);
            if (values == null) values = req.getParameterValues(name + "[]");
            if (values == null) values = new String[0];

            Object arr = Array.newInstance(comp, values.length);
            for (int i = 0; i < values.length; i++) {
                Array.set(arr, i, convert(values[i], comp));
            }
            return arr;
        }

        // tableau d'objets : personnes[0].nom, personnes[1].nom ...
        Pattern p = Pattern.compile(Pattern.quote(name) + "\\[(\\d+)\\].*");
        int max = -1;
        for (String k : req.getParameterMap().keySet()) {
            Matcher m = p.matcher(k);
            if (m.matches()) max = Math.max(max, Integer.parseInt(m.group(1)));
        }

        Object arr = Array.newInstance(comp, max + 1);
        for (int i = 0; i <= max; i++) {
            Array.set(arr, i, bindObject(req, name + "[" + i + "]", comp, false));
        }
        return arr;
    }

    private static boolean hasParam(HttpServletRequest req, String key) {
        for (String k : req.getParameterMap().keySet()) {
            if (k.equals(key) || k.startsWith(key + ".") || k.startsWith(key + "[")) return true;
        }
        return false;
    }

    private static void setValue(Object obj, Field f, Object value) throws Exception {
        String setter = "set" + Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1);
        try {
            Method m = obj.getClass().getMethod(setter, f.getType());
            m.invoke(obj, value);
        } catch (NoSuchMethodException e) {
            f.setAccessible(true);
            f.set(obj, value);
        }
    }

    /** Conversion String -> type cible */
    public static Object convert(String value, Class<?> type) {
        if (value == null || value.isEmpty()) {
            if (type.isPrimitive()) { // valeur par défaut : 0, false...
                return Array.get(Array.newInstance(type, 1), 0);
            }
            return null;
        }
        if (type.equals(String.class)) return value;
        if (type.equals(int.class) || type.equals(Integer.class)) return Integer.parseInt(value);
        if (type.equals(long.class) || type.equals(Long.class)) return Long.parseLong(value);
        if (type.equals(short.class) || type.equals(Short.class)) return Short.parseShort(value);
        if (type.equals(float.class) || type.equals(Float.class)) return Float.parseFloat(value.replace(',', '.'));
        if (type.equals(double.class) || type.equals(Double.class)) return Double.parseDouble(value.replace(',', '.'));
        if (type.equals(boolean.class) || type.equals(Boolean.class)) return Boolean.parseBoolean(value);
        if (type.equals(char.class) || type.equals(Character.class)) return value.charAt(0);
        if (type.equals(LocalDate.class)) return LocalDate.parse(value);
        return value;
    }
}