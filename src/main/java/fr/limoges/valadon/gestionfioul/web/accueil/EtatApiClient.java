package fr.limoges.valadon.gestionfioul.web.accueil;

import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;

/**
 * Vérifie que l'API répond, en lisant sa description OpenAPI ({@code /v3/api-docs}, route publique).
 *
 * <p>Seuls les champs {@code info.title} et {@code info.version} du JSON nous intéressent.</p>
 */
@Component
public class EtatApiClient {

    private final RestClient restClient;

    public EtatApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Interroge l'API. Ne lève jamais d'exception : une API arrêtée donne simplement un état « indisponible ».
     *
     * @return l'état de l'API, à afficher sur la page d'accueil
     */
    public EtatApi consulter() {
        try {
            DocumentationApi documentation = restClient.get()
                    .uri("/v3/api-docs")
                    .retrieve()
                    .body(DocumentationApi.class);
            return EtatApi.disponible(documentation.info().title(), documentation.info().version());
        } catch (ResourceAccessException | ApiException e) {
            // l'API est arrêtée, ou répond mais sans sa documentation : on l'indique sans faire planter la page
            return EtatApi.indisponible();
        }
    }

    // structure minimale du JSON OpenAPI : {"openapi":"3.1.0","info":{"title":"...","version":"..."},"paths":{...}}
    record DocumentationApi(Info info) {
        record Info(String title, String version) {
        }
    }
}
