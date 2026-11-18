package fr.limoges.valadon.gestionfioul.web.participation;

/**
 * État d'une participation. Copie de l'enum de l'API (mêmes noms de constantes que dans le JSON).
 */
public enum StatutParticipation {
    /** Le volume compte dans la commande groupée. */
    CONFIRMEE,
    /** Le client s'est retiré avant la date limite. */
    ANNULEE
}
