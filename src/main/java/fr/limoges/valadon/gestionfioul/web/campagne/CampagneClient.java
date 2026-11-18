package fr.limoges.valadon.gestionfioul.web.campagne;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;

/**
 * Appels à l'API concernant les campagnes.
 *
 * <p>Joue côté web le rôle que joue le Repository côté API : c'est la seule classe qui sait
 * COMMENT obtenir les campagnes (ici par HTTP, là-bas dans MongoDB).</p>
 *
 * <p>Toute erreur de l'API est levée sous forme d'{@link ApiException}.</p>
 */
@Component
public class CampagneClient {

    private final RestClient restClient;

    public CampagneClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * {@code GET /api/campagnes?statut=...}
     *
     * @param statut statut recherché
     * @return les campagnes ayant ce statut
     */
    public List<Campagne> listerParStatut(StatutCampagne statut) {
        return restClient.get()
                .uri("/api/campagnes?statut={statut}", statut)
                .retrieve()
                // List<Campagne>.class n'existe pas en Java (effacement des génériques) :
                // ParameterizedTypeReference conserve le type complet pour Jackson
                .body(new ParameterizedTypeReference<List<Campagne>>() {});
    }

    /**
     * {@code GET /api/campagnes/{id}}
     *
     * @param id identifiant de la campagne
     * @return la campagne
     * @throws ApiException 404 si la campagne n'existe pas
     */
    public Campagne trouver(String id) {
        return restClient.get()
                .uri("/api/campagnes/{id}", id)
                .retrieve()
                .body(Campagne.class);
    }

    /**
     * {@code POST /api/campagnes} (administrateur) : la campagne est créée au statut {@code BROUILLON}.
     *
     * @param requete libellé, dates et zone de la nouvelle campagne
     * @return la campagne créée, avec son identifiant
     * @throws ApiException 400 si l'API refuse les données
     */
    public Campagne creer(CreerCampagneRequest requete) {
        return restClient.post()
                .uri("/api/campagnes")
                .body(requete)
                .retrieve()
                .body(Campagne.class);
    }

    /**
     * {@code POST /api/campagnes/{id}/ouvrir}, {@code /cloturer}, {@code /attribution}, {@code /livrer}
     * ou {@code /annuler} (administrateur).
     *
     * <p>Une seule méthode pour les cinq actions : l'énumération porte la fin de l'URL.
     * La réponse ne nous sert pas (la page sera rechargée) : {@code toBodilessEntity()} ignore le corps.</p>
     *
     * @param id     identifiant de la campagne
     * @param action action à exécuter
     * @throws ApiException 409 si la transition est interdite depuis le statut actuel
     */
    public void executer(String id, ActionCampagne action) {
        restClient.post()
                .uri("/api/campagnes/{id}/{action}", id, action.getSegmentApi())
                .retrieve()
                .toBodilessEntity();
    }
}
