package Genius.util;

import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GeniusSession {

    private String id;
    private Map<String, Object> attributes;
    private Set<String> initialKeys;

    public GeniusSession() {
        this.attributes = new HashMap<>();
        this.initialKeys = new HashSet<>();
    }

    public static GeniusSession from(HttpSession httpSession) {
        GeniusSession session = new GeniusSession();

        session.id = httpSession.getId();

        httpSession.getAttributeNames().asIterator().forEachRemaining(name -> {
            Object value = httpSession.getAttribute(name);

            session.attributes.put(name, value);
            session.initialKeys.add(name);
        });

        return session;
    }

    public void flushTo(HttpSession httpSession) {
        if (httpSession == null) {
            return;
        }

        // Supprimer les attributs retirés
        for (String key : initialKeys) {
            if (!attributes.containsKey(key)) {
                httpSession.removeAttribute(key);
            }
        }

        // Ajouter ou mettre à jour les attributs
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            httpSession.setAttribute(entry.getKey(), entry.getValue());
        }

        // Actualiser les clés initiales
        initialKeys.clear();
        initialKeys.addAll(attributes.keySet());
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        if (name == null) {
            throw new IllegalArgumentException(
                "Le nom de l'attribut ne peut pas être null."
            );
        }

        if (value == null) {
            removeAttribute(name);
            return;
        }

        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Set<String> getInitialKeys() {
        return initialKeys;
    }
}