package fr.limoges.valadon.gestionfioul.web.proposition;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;

/**
 * Appels à l'API concernant les propositions (barèmes des fournisseurs).
 *
 * <p>Toute erreur de l'API est levée sous forme d'{@link ApiException}.</p>
 */
@Component
public class PropositionClient {

    private final RestClient restClient;

    public PropositionClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * {@code POST /api/campagnes/{campagneId}/propositions} : le fournisseur connecté dépose un barème.
     *
     * @param campagneId identifiant de la campagne
     * @param requete    date de validité et paliers
     * @return la proposition enregistrée, au statut {@code SOUMISE}
     * @throws ApiException 400 si le barème est incohérent, 409 si la campagne n'est pas ouverte
     */
    public Proposition soumettre(String campagneId, SoumettrePropositionRequest requete) {
        return restClient.post()
                .uri("/api/campagnes/{campagneId}/propositions", campagneId)
                .body(requete)
                .retrieve()
                .body(Proposition.class);
    }

    /**
     * {@code GET /api/campagnes/{campagneId}/propositions} (administrateur).
     *
     * @param campagneId identifiant de la campagne
     * @return les barèmes de tous les fournisseurs sur cette campagne
     */
    public List<Proposition> listerParCampagne(String campagneId) {
        return restClient.get()
                .uri("/api/campagnes/{campagneId}/propositions", campagneId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Proposition>>() {});
    }

    /**
     * {@code GET /api/moi/propositions}
     *
     * @return les propositions du fournisseur connecté
     */
    public List<Proposition> mesPropositions() {
        return restClient.get()
                .uri("/api/moi/propositions")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Proposition>>() {});
    }
}
