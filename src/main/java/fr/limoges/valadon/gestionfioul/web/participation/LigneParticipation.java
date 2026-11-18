package fr.limoges.valadon.gestionfioul.web.participation;

import java.time.LocalDateTime;

import fr.limoges.valadon.gestionfioul.web.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.web.campagne.StatutCampagne;

/**
 * Une ligne du tableau « Mes participations » : la participation ET sa campagne.
 *
 * <p>C'est un objet de présentation (« view model ») : il assemble ce dont la VUE a besoin,
 * à partir de deux réponses distinctes de l'API.</p>
 *
 * @param participation la participation du client
 * @param campagne      la campagne concernée (libellé, statut, date limite)
 */
public record LigneParticipation(Participation participation, Campagne campagne) {

    /**
     * Indique si le bouton « Annuler » doit être affiché.
     *
     * <p>Mêmes conditions que l'API : participation confirmée, campagne ouverte et date limite non dépassée.
     * Ne sert qu'à afficher ou masquer le bouton : en cas de désaccord, c'est l'API qui tranche.</p>
     *
     * @return {@code true} si la participation peut encore être annulée
     */
    public boolean annulable() {
        return participation.statut() == StatutParticipation.CONFIRMEE
                && campagne.statut() == StatutCampagne.OUVERTE
                && campagne.dateCloture().isAfter(LocalDateTime.now());
    }
}
