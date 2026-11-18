package fr.limoges.valadon.gestionfioul.web.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.limoges.valadon.gestionfioul.web.security.UtilisateurConnecte;

/**
 * Ajoute automatiquement les identifiants de l'utilisateur connecté (HTTP Basic) à chaque appel vers l'API.
 *
 * <p>Ainsi les clients ({@code CampagneClient}...) n'ont jamais à s'occuper de l'authentification.</p>
 *
 * <p>{@link SecurityContextHolder} donne l'utilisateur de la requête HTTP EN COURS de traitement :
 * chaque requête du navigateur est traitée par un thread, qui a son propre contexte de sécurité.</p>
 */
public class AuthentificationApiInterceptor implements ClientHttpRequestInterceptor {

    /**
     * Ajoute l'en-tête {@code Authorization} si un utilisateur est connecté et si la requête n'en a pas déjà un.
     *
     * @param request   requête vers l'API, modifiable
     * @param body      corps de la requête
     * @param execution suite de la chaîne : envoie réellement la requête
     * @return la réponse de l'API
     * @throws IOException en cas d'erreur réseau
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // on ne remplace pas un en-tête déjà posé (cas de la connexion, qui fournit ses propres identifiants)
        if (authentication != null
                && authentication.getPrincipal() instanceof UtilisateurConnecte utilisateur
                && request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION) == null) {
            request.getHeaders().setBasicAuth(utilisateur.email(), utilisateur.motDePasse(), StandardCharsets.UTF_8);
        }
        return execution.execute(request, body);
    }
}
