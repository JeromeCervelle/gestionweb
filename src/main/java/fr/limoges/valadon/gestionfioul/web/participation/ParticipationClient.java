package fr.limoges.valadon.gestionfioul.web.participation;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;

/**
 * Appels à l'API concernant les participations.
 *
 * <p>Les identifiants sont ajoutés par l'intercepteur
 * ({@link fr.limoges.valadon.gestionfioul.web.api.AuthentificationApiInterceptor AuthentificationApiInterceptor}) :
 * l'API sait QUI participe. Toute erreur de l'API est levée sous forme d'{@link ApiException}.</p>
 */
@Component
public class ParticipationClient {

    private final RestClient restClient;

    public ParticipationClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * {@code POST /api/campagnes/{campagneId}/participations} : le client connecté s'engage sur un volume.
     *
     * @param campagneId identifiant de la campagne
     * @param requete    volume et adresse de livraison
     * @return la participation enregistrée
     * @throws ApiException 409 si la campagne n'est pas ouverte ou si le client y participe déjà
     */
    public Participation participer(String campagneId, ParticiperRequest requete) {
        return restClient.post()
                .uri("/api/campagnes/{campagneId}/participations", campagneId)
                .body(requete)
                .retrieve()
                .body(Participation.class);
    }

    /**
     * {@code GET /api/moi/participations}
     *
     * @return toutes les participations du client connecté, annulées comprises
     */
    public List<Participation> mesParticipations() {
        return restClient.get()
                .uri("/api/moi/participations")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Participation>>() {});
    }

    /**
     * {@code GET /api/campagnes/{campagneId}/participations} (administrateur).
     *
     * @param campagneId identifiant de la campagne
     * @return toutes les participations de la campagne, annulées comprises
     */
    public List<Participation> listerParCampagne(String campagneId) {
        return restClient.get()
                .uri("/api/campagnes/{campagneId}/participations", campagneId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Participation>>() {});
    }

    /**
     * {@code POST /api/participations/{id}/annuler} (requête sans corps).
     *
     * @param participationId identifiant de la participation
     * @return la participation, désormais au statut {@code ANNULEE}
     * @throws ApiException 409 si la date limite est dépassée, 403 si ce n'est pas la participation du client
     */
    public Participation annuler(String participationId) {
        return restClient.post()
                .uri("/api/participations/{id}/annuler", participationId)
                .retrieve()
                .body(Participation.class);
    }
}
