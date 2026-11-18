package fr.limoges.valadon.gestionfioul.web.user;

/**
 * Rôle d'un utilisateur. Copie de l'enum {@code Role} de l'API (mêmes noms de constantes que dans le JSON).
 */
public enum Role {
    /** Gère les campagnes. Créé au démarrage de l'API, jamais par inscription. */
    ADMIN,
    /** Participe aux campagnes en s'engageant sur un volume. */
    CLIENT,
    /** Propose des barèmes de prix. */
    FOURNISSEUR
}
