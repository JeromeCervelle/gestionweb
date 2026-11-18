package fr.limoges.valadon.gestionfioul.web.participation;

import java.time.LocalDateTime;

/**
 * Une participation telle que l'API la renvoie en JSON.
 *
 * <p>Elle ne contient que l'IDENTIFIANT de la campagne (pas son libellé) : c'est le choix de modélisation de l'API.</p>
 *
 * @param id                identifiant de la participation
 * @param campagneId        identifiant de la campagne
 * @param clientId          identifiant du client qui participe
 * @param volumeSouhaite    volume commandé, en litres
 * @param adresseLivraison  adresse de livraison retenue
 * @param statut            confirmée ou annulée
 * @param dateParticipation date d'enregistrement
 */
public record Participation(
        String id,
        String campagneId,
        String clientId,
        int volumeSouhaite,
        String adresseLivraison,
        StatutParticipation statut,
        LocalDateTime dateParticipation) {
}
