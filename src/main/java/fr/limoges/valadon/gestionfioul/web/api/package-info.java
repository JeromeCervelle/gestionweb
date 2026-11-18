/**
 * Communication avec l'API REST gestionfioul.
 *
 * <p>{@link fr.limoges.valadon.gestionfioul.web.api.ApiConfig} déclare l'unique {@code RestClient} du site.
 * Il ajoute les identifiants de l'utilisateur connecté à chaque requête
 * ({@link fr.limoges.valadon.gestionfioul.web.api.AuthentificationApiInterceptor}) et transforme toute
 * réponse d'erreur en {@link fr.limoges.valadon.gestionfioul.web.api.ApiException}
 * ({@link fr.limoges.valadon.gestionfioul.web.api.ApiErrorHandler}).</p>
 */
package fr.limoges.valadon.gestionfioul.web.api;
