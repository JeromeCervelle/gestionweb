package fr.limoges.valadon.gestionfioul.web.common;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.ModelAndView;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;

/**
 * Filet de sécurité commun à tous les contrôleurs : une erreur non traitée sur place
 * affiche la page « erreur » au lieu de la page blanche (Whitelabel) de Spring Boot.
 *
 * <p>{@code @ControllerAdvice} (et non {@code @RestControllerAdvice} comme dans l'API) :
 * on renvoie une VUE, pas du JSON.</p>
 */
@ControllerAdvice
public class WebExceptionHandler {

    /**
     * L'API a répondu, mais par une erreur (401, 403, 404, 409...) : on affiche son message avec son code.
     *
     * @param e erreur renvoyée par l'API
     * @return la vue {@code erreur}, avec le même code HTTP que l'API
     */
    @ExceptionHandler(ApiException.class)
    public ModelAndView erreurApi(ApiException e) {
        HttpStatus statut = HttpStatus.valueOf(e.getStatut().value());
        return new ModelAndView("erreur",
                Map.of("statut", statut.value(), "message", e.getMessage()),
                statut);
    }

    /**
     * L'API n'a pas répondu du tout : arrêtée, mauvaise adresse, délai dépassé...
     *
     * @param e erreur réseau
     * @return la vue {@code erreur} avec le code 503 (service indisponible)
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ModelAndView apiInjoignable(ResourceAccessException e) {
        return new ModelAndView("erreur",
                Map.of("statut", 503, "message", "Le service est momentanément indisponible : l'API ne répond pas."),
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
