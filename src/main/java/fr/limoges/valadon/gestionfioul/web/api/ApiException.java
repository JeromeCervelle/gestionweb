package fr.limoges.valadon.gestionfioul.web.api;

import org.springframework.http.HttpStatusCode;

/**
 * L'API a répondu par une erreur (4xx ou 5xx).
 *
 * <p>Le message est celui de l'API (champ {@code detail} du ProblemDetail), directement affichable à l'utilisateur.</p>
 */
public class ApiException extends RuntimeException {

    private final HttpStatusCode statut;

    /**
     * Crée l'exception.
     *
     * @param statut  code HTTP renvoyé par l'API
     * @param message message affichable à l'utilisateur
     */
    public ApiException(HttpStatusCode statut, String message) {
        super(message);
        this.statut = statut;
    }

    /**
     * Code HTTP de la réponse, pour distinguer par exemple un 401 (identifiants faux) d'un 409 (conflit).
     *
     * @return le code HTTP renvoyé par l'API
     */
    public HttpStatusCode getStatut() {
        return statut;
    }
}
