package fr.limoges.valadon.gestionfioul.web.campagne;

import java.util.List;

/**
 * Étapes du cycle de vie d'une campagne.
 *
 * <p>Copie de l'enum de l'API : les noms doivent être identiques pour que Jackson
 * convertisse {@code "OUVERTE"} (JSON) en {@code StatutCampagne.OUVERTE} (Java).</p>
 */
public enum StatutCampagne {
    /** Créée par l'administrateur, pas encore visible des clients. */
    BROUILLON,
    /** Les clients s'engagent sur un volume, les fournisseurs proposent un barème. */
    OUVERTE,
    /** Participations fermées, volume total figé : en attente de l'attribution. */
    CLOTUREE,
    /** La proposition la moins chère a été retenue. */
    ATTRIBUEE,
    /** La commande a été livrée : état final. */
    LIVREE,
    /** Campagne abandonnée : état final. */
    ANNULEE;

    /**
     * Les actions possibles depuis ce statut : le cycle de vie de l'API, vu du côté de l'écran.
     *
     * <p>Le site n'affiche que ces boutons ; l'API refuserait de toute façon une transition interdite (409).</p>
     *
     * <p>{@code switch} « expression » (Java 14+) : chaque cas produit une valeur, le compilateur vérifie
     * que toutes les constantes de l'énumération sont traitées.</p>
     *
     * @return les actions proposées à l'administrateur, vide pour un état final
     */
    public List<ActionCampagne> actionsPossibles() {
        return switch (this) {
            case BROUILLON -> List.of(ActionCampagne.OUVRIR, ActionCampagne.ANNULER);
            case OUVERTE -> List.of(ActionCampagne.CLOTURER, ActionCampagne.ANNULER);
            case CLOTUREE -> List.of(ActionCampagne.ATTRIBUER, ActionCampagne.ANNULER);
            case ATTRIBUEE -> List.of(ActionCampagne.LIVRER, ActionCampagne.ANNULER);
            case LIVREE, ANNULEE -> List.of();
        };
    }
}
