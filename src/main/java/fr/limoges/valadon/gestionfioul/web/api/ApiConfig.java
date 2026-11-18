package fr.limoges.valadon.gestionfioul.web.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.json.JsonMapper;

/**
 * Déclare l'unique {@link RestClient} utilisé par le site pour parler à l'API gestionfioul.
 *
 * <p>Tous les clients ({@code CampagneClient}...) reçoivent ce bean par injection.</p>
 */
@Configuration
public class ApiConfig {

    /**
     * Construit le {@code RestClient} partagé par tous les clients de l'API.
     *
     * @param builder    constructeur préconfiguré par Spring Boot (convertisseurs JSON...)
     * @param apiUrl     adresse de l'API, propriété {@code gestionfioul.api.url} de {@code application.properties}
     * @param jsonMapper lecteur JSON de Spring Boot, utilisé pour lire les corps d'erreur
     * @return le client HTTP configuré
     */
    @Bean
    public RestClient apiRestClient(RestClient.Builder builder,
                                    @Value("${gestionfioul.api.url}") String apiUrl,
                                    JsonMapper jsonMapper) {
        return builder
                // toutes les URI relatives ("/api/campagnes") sont préfixées par l'adresse de l'API
                .baseUrl(apiUrl)
                // identifiants de l'utilisateur connecté ajoutés à chaque requête (étape 3)
                .requestInterceptor(new AuthentificationApiInterceptor())
                // toute réponse 4xx / 5xx devient une ApiException avec un message lisible
                .defaultStatusHandler(HttpStatusCode::isError, new ApiErrorHandler(jsonMapper))
                .build();
    }
}
