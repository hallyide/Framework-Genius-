package Genius.util;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jakarta.servlet.http.Part;
import Genius.util.Fichier;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ParamBinder {
    public <T> T bind(Class<T> classe, HttpServletRequest req)
            throws Exception {

        return bindObject(classe, "", req);
    }

    /**
     *
     * @param classe
     * @param prefixe
     * @param req
     */
    private <T> T bindObject(
            Class<T> classe,
            String prefixe,
            HttpServletRequest req
    ) throws Exception {

        // Création de l'objet
        Constructor<T> constructeur = classe.getDeclaredConstructor();
        constructeur.setAccessible(true);

        T objet = constructeur.newInstance();

        // Parcours des attributs de la classe
        Field[] fields = classe.getDeclaredFields();

        for (Field field : fields) {

            // Ignorer les attributs static
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String nomChamp = field.getName();

            // Construire le nom du paramètre HTTP
            String nomParametre;

            if (prefixe == null || prefixe.isEmpty()) {
                nomParametre = nomChamp;
            } else {
                nomParametre = prefixe + "." + nomChamp;
            }

            Class<?> typeChamp = field.getType();

            // Récupérer la valeur envoyée par le formulaire
            String valeur = req.getParameter(nomParametre);

            // Si une valeur simple existe, l'affecter
            if (valeur != null) {

                // Une valeur vide n'est pas convertie en nombre
                if (valeur.isEmpty()) {
                    continue;
                }

                Object valeurConvertie = convertir(valeur, typeChamp);

                affecterAttribut(objet, field, valeurConvertie);

            } else if (estObjetComplexe(typeChamp)) {

                /*
                 * Aucun paramètre direct :
                 * rechercher récursivement les attributs imbriqués.
                 *
                 * Exemple :
                 * genre.libelle
                 * genre.categorie.nom
                 */
                if (contientParametre(prefixe, nomChamp, req)) {

                    Object sousObjet = bindObject(
                            typeChamp,
                            nomParametre,
                            req
                    );

                    affecterAttribut(objet, field, sousObjet);
                }
            }
        }

        return objet;
    }

    /**
     * Vérifie si la requête contient au moins un paramètre
     * correspondant à un attribut imbriqué.
     *
     * Exemple :
     * genre.libelle
     * genre.categorie.nom
     */
    private boolean contientParametre(
            String prefixe,
            String nomChamp,
            HttpServletRequest req
    ) {

        String debut;

        if (prefixe == null || prefixe.isEmpty()) {
            debut = nomChamp + ".";
        } else {
            debut = prefixe + "." + nomChamp + ".";
        }

        for (String nom : req.getParameterMap().keySet()) {
            if (nom.startsWith(debut)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Affecte un attribut à un objet.
     *
     * Privilégie le setter public.
     * Si aucun setter n'existe, utilise directement le champ.
     */
    private void affecterAttribut(
            Object objet,
            Field field,
            Object valeur
    ) throws Exception {

        String nomChamp = field.getName();

        String nomSetter =
                "set"
                + Character.toUpperCase(nomChamp.charAt(0))
                + nomChamp.substring(1);

        try {

            Method setter = objet.getClass().getMethod(
                    nomSetter,
                    field.getType()
            );

            setter.invoke(objet, valeur);

        } catch (NoSuchMethodException e) {

            // Aucun setter : affectation directe du champ
            field.setAccessible(true);
            field.set(objet, valeur);
        }
    }

    /**
     * Détermine si un type représente un objet complexe.
     */
    private boolean estObjetComplexe(Class<?> type) {

        return !type.isPrimitive()
                && type != String.class
                && !Number.class.isAssignableFrom(type)
                && type != Boolean.class
                && type != Character.class
                && !type.isEnum()
                && !type.getName().startsWith("java.");
    }

    /**
     * Convertit une valeur HTTP vers le type Java attendu.
     */
    private Object convertir(String valeur, Class<?> type) {

        if (type == String.class) {
            return valeur;
        }

        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(valeur);
        }

        if (type == long.class || type == Long.class) {
            return Long.parseLong(valeur);
        }

        if (type == double.class || type == Double.class) {
            return Double.parseDouble(valeur);
        }

        if (type == float.class || type == Float.class) {
            return Float.parseFloat(valeur);
        }

        if (type == short.class || type == Short.class) {
            return Short.parseShort(valeur);
        }

        if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(valeur);
        }

        if (type == boolean.class || type == Boolean.class) {
            if (!valeur.equalsIgnoreCase("true")
                    && !valeur.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException(
                        "Valeur booléenne invalide : " + valeur
                );
            }

            return Boolean.parseBoolean(valeur);
        }

        if (type == char.class || type == Character.class) {
            if (valeur.length() != 1) {
                throw new IllegalArgumentException(
                        "Caractère invalide : " + valeur
                );
            }

            return valeur.charAt(0);
        }

        if (type.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object resultat = Enum.valueOf(
                    (Class<? extends Enum>) type,
                    valeur
            );

            return resultat;
        }

        throw new IllegalArgumentException(
                "Type non pris en charge : " + type.getName()
        );
    }

    public Fichier construireFichier(
            HttpServletRequest req,
            String nomParametre
    ) throws Exception {

        // Récupérer le fichier envoyé par le formulaire
        Part part = req.getPart(nomParametre);

        if (part == null || part.getSize() == 0) {
            return null;
        }

        // Récupérer le nom du fichier
        String nomFichier = part.getSubmittedFileName();

        // Récupérer le type MIME
        String typeFichier = part.getContentType();

        // Lire les données binaires du fichier
        byte[] contenu;

        try (InputStream input = part.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int longueur;

            while ((longueur = input.read(buffer)) != -1) {
                output.write(buffer, 0, longueur);
            }

            contenu = output.toByteArray();
        }

        // Construire l'objet Fichier
        return new Fichier(
                contenu,
                nomFichier,
                typeFichier
        );
    }
}