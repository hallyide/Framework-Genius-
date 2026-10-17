package Genius.util;

public class Fichier {

    private byte[] contenu;
    private String nom;
    private String type;

    public Fichier() {
    }

    public Fichier(byte[] contenu, String nom, String type) {
        this.contenu = contenu;
        this.nom = nom;
        this.type = type;
    }

    public byte[] getContenu() {
        return contenu;
    }

    public void setContenu(byte[] contenu) {
        this.contenu = contenu;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}