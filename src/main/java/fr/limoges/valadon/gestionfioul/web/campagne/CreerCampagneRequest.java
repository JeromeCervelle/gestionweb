package fr.limoges.valadon.gestionfioul.web.campagne;

import java.time.LocalDateTime;

/**
 * Corps JSON envoyé à {@code POST /api/campagnes}.
 *
 * <p>Le statut, le volume et la proposition retenue ne sont pas envoyés : c'est l'API qui les fixe.</p>
 *
 * @param libelle       nom de la campagne
 * @param dateOuverture date d'ouverture aux participations
 * @param dateCloture   date limite de participation
 * @param zoneLivraison zone desservie, facultative
 */
public record CreerCampagneRequest(
        String libelle,
        LocalDateTime dateOuverture,
        LocalDateTime dateCloture,
        String zoneLivraison) {
}
