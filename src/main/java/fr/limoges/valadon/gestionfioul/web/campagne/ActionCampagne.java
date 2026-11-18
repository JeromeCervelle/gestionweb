package fr.limoges.valadon.gestionfioul.web.campagne;

/**
 * Les actions de l'administrateur sur une campagne, avec tout ce qu'il faut pour les AFFICHER (libellé du bouton,
 * message de confirmation) et pour les EXÉCUTER (fin de l'URL de l'API).
 *
 * <p>Une énumération peut avoir des attributs et un constructeur : chaque constante est un objet complet.</p>
 */
public enum ActionCampagne {

    /** {@code BROUILLON → OUVERTE} : les clients et les fournisseurs peuvent participer. */
    OUVRIR("ouvrir", "Ouvrir la campagne",
            "Ouvrir la campagne ? Clients et fournisseurs pourront y participer."),
    /** {@code OUVERTE → CLOTUREE} : plus de participation, le volume total est figé. */
    CLOTURER("cloturer", "Clôturer",
            "Clôturer la campagne ? Plus aucune participation ne sera acceptée et le volume total sera figé."),
    /** {@code CLOTUREE → ATTRIBUEE} : l'API retient la proposition la moins chère pour le volume total. */
    ATTRIBUER("attribution", "Attribuer au moins-disant",
            "Attribuer la campagne ? La proposition la moins chère pour le volume total sera retenue, les autres rejetées."),
    /** {@code ATTRIBUEE → LIVREE} : la commande a été livrée. */
    LIVRER("livrer", "Marquer comme livrée",
            "Confirmer la livraison de la campagne ?"),
    /** Annulation définitive, possible tant que la campagne n'est pas livrée. */
    ANNULER("annuler", "Annuler la campagne",
            "Annuler définitivement la campagne ? Cette action est irréversible.");

    private final String segmentApi;
    private final String libelle;
    private final String confirmation;

    ActionCampagne(String segmentApi, String libelle, String confirmation) {
        this.segmentApi = segmentApi;
        this.libelle = libelle;
        this.confirmation = confirmation;
    }

    /**
     * Fin de l'URL de l'API : {@code POST /api/campagnes/{id}/{segmentApi}}.
     *
     * @return le dernier segment de l'URL ({@code ouvrir}, {@code attribution}...)
     */
    public String getSegmentApi() {
        return segmentApi;
    }

    /**
     * Texte du bouton.
     *
     * @return le libellé affiché sur la page de la campagne
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * Question posée avant d'exécuter l'action.
     *
     * @return le message de la boîte de confirmation
     */
    public String getConfirmation() {
        return confirmation;
    }

    /**
     * Indique si l'action est irréversible, pour afficher son bouton en rouge.
     *
     * @return {@code true} pour l'annulation
     */
    public boolean isDestructive() {
        return this == ANNULER;
    }
}
