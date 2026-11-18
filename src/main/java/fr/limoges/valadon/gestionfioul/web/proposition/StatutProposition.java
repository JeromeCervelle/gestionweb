package fr.limoges.valadon.gestionfioul.web.proposition;

/**
 * État d'une proposition. Copie de l'enum de l'API (mêmes noms de constantes que dans le JSON).
 */
public enum StatutProposition {
    /** En attente de la clôture et de l'attribution. */
    SOUMISE,
    /** La moins-disante : elle a gagné. */
    RETENUE,
    /** Une autre proposition a été retenue. */
    REJETEE
}
