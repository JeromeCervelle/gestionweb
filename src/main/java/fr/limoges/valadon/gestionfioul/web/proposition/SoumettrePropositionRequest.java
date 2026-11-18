package fr.limoges.valadon.gestionfioul.web.proposition;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corps JSON envoyé à {@code POST /api/campagnes/{id}/propositions}.
 *
 * <p>Pas de {@code fournisseurId} : l'API prend l'utilisateur authentifié.</p>
 *
 * @param validiteJusqua date jusqu'à laquelle les prix sont garantis
 * @param paliers        les tranches de volume et leurs prix
 */
public record SoumettrePropositionRequest(LocalDateTime validiteJusqua, List<Palier> paliers) {
}
