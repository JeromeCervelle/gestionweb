package fr.limoges.valadon.gestionfioul.web.api;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.json.JsonMapper;

/**
 * Transforme toute réponse d'erreur de l'API en {@link ApiException} portant un message lisible.
 *
 * <ul>
 *   <li>Si l'API a renvoyé un ProblemDetail (400, 404, 409 levées par les services), on reprend son {@code detail}.</li>
 *   <li>Sinon (401 et 403 de Spring Security, qui n'ont pas de corps), on fournit un message par défaut.</li>
 * </ul>
 */
public class ApiErrorHandler implements RestClient.ResponseSpec.ErrorHandler {

    private final JsonMapper jsonMapper;

    /**
     * Crée le gestionnaire d'erreurs.
     *
     * @param jsonMapper lecteur JSON utilisé pour décoder le ProblemDetail
     */
    public ApiErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Lève systématiquement une {@link ApiException} : appelée par le {@code RestClient} pour toute réponse 4xx ou 5xx.
     *
     * @param request  requête envoyée à l'API
     * @param response réponse d'erreur reçue
     * @throws IOException si la réponse ne peut pas être lue
     */
    @Override
    public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        HttpStatusCode statut = response.getStatusCode();
        String detail = lireDetail(response);
        throw new ApiException(statut, detail != null ? detail : messageParDefaut(statut));
    }

    private String lireDetail(ClientHttpResponse response) {
        try {
            byte[] corps = response.getBody().readAllBytes();
            if (corps.length == 0) {
                return null;
            }
            ProblemeApi probleme = jsonMapper.readValue(corps, ProblemeApi.class);
            return probleme.detail() == null || probleme.detail().isBlank() ? null : probleme.detail();
        } catch (IOException | RuntimeException e) {
            // corps absent ou qui n'est pas du JSON (page HTML d'un proxy, par exemple)
            return null;
        }
    }

    private String messageParDefaut(HttpStatusCode statut) {
        if (statut.value() == HttpStatus.UNAUTHORIZED.value()) {
            return "Authentification refusée par l'API : identifiants absents ou invalides.";
        }
        if (statut.value() == HttpStatus.FORBIDDEN.value()) {
            return "Vous n'avez pas les droits nécessaires pour cette action.";
        }
        if (statut.value() == HttpStatus.NOT_FOUND.value()) {
            return "L'élément demandé est introuvable.";
        }
        return "L'API a renvoyé une erreur (code " + statut.value() + ").";
    }
}
