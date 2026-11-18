package fr.limoges.valadon.gestionfioul.web.proposition;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Une proposition (barème) telle que l'API la renvoie en JSON. Les paliers sont imbriqués dans le document.
 *
 * @param id              identifiant de la proposition
 * @param campagneId      identifiant de la campagne
 * @param fournisseurId   identifiant du fournisseur
 * @param dateProposition date de dépôt
 * @param validiteJusqua  date jusqu'à laquelle le fournisseur s'engage sur ces prix
 * @param statut          soumise, retenue ou rejetée
 * @param paliers         les tranches de volume et leurs prix, du plus petit volume au plus grand
 */
public record Proposition(
        String id,
        String campagneId,
        String fournisseurId,
        LocalDateTime dateProposition,
        LocalDateTime validiteJusqua,
        StatutProposition statut,
        List<Palier> paliers) {
}
