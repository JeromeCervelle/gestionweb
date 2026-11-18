package fr.limoges.valadon.gestionfioul.web.campagne;

import java.time.LocalDateTime;

/**
 * Une campagne telle que l'API la renvoie en JSON.
 *
 * <p>Côté web, ce n'est pas un document MongoDB mais un simple DTO en lecture : un record suffit.
 * Les noms des composants doivent correspondre aux noms des champs JSON.</p>
 *
 * @param id                   identifiant MongoDB de la campagne
 * @param libelle              nom affiché (« Commande d'hiver »...)
 * @param dateOuverture        date à partir de laquelle on peut participer
 * @param dateCloture          date limite de participation
 * @param statut               étape du cycle de vie
 * @param volumeTotal          volume total commandé, en litres ; vaut 0 tant que la campagne n'est pas clôturée
 * @param zoneLivraison        zone desservie (numéro de département...), facultative
 * @param propositionRetenueId identifiant de la proposition gagnante, {@code null} avant l'attribution
 */
public record Campagne(
        String id,
        String libelle,
        LocalDateTime dateOuverture,
        LocalDateTime dateCloture,
        StatutCampagne statut,
        int volumeTotal,
        String zoneLivraison,
        String propositionRetenueId) {
}
