package fr.limoges.valadon.gestionfioul.web.participation;

/**
 * Corps JSON envoyé à {@code POST /api/campagnes/{id}/participations}.
 *
 * <p>Pas de {@code clientId} : l'API prend l'utilisateur authentifié (on ne pourrait pas participer au nom d'un autre).</p>
 *
 * @param volumeSouhaite   volume commandé, en litres
 * @param adresseLivraison adresse de livraison ; vide : l'API reprend l'adresse du profil du client
 */
public record ParticiperRequest(int volumeSouhaite, String adresseLivraison) {
}
