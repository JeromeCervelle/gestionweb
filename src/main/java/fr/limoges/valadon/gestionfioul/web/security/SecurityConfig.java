package fr.limoges.valadon.gestionfioul.web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sécurité du SITE (à ne pas confondre avec celle de l'API).
 *
 * <ul>
 *   <li>Connexion par formulaire : l'utilisateur connecté est gardé en session (cookie {@code JSESSIONID}).</li>
 *   <li>Protection CSRF ACTIVÉE (comportement par défaut) : contrairement à l'API, le navigateur envoie
 *       automatiquement le cookie de session, un site malveillant pourrait donc déclencher des actions.</li>
 *   <li>Droits par rôle : espace client, espace fournisseur, espace administrateur.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    /**
     * Règles d'accès aux pages, formulaire de connexion et déconnexion.
     *
     * <table>
     *   <caption>Droits d'accès</caption>
     *   <tr><th>Pages</th><th>Accès</th></tr>
     *   <tr><td>{@code /}, {@code /connexion}, {@code /inscription}, {@code /css/**}</td><td>tout le monde</td></tr>
     *   <tr><td>{@code /mes-participations}, <code>/campagnes/&#42;/participer</code>...</td><td>rôle {@code CLIENT}</td></tr>
     *   <tr><td>{@code /mes-propositions}, <code>/campagnes/&#42;/proposer</code></td><td>rôle {@code FOURNISSEUR}</td></tr>
     *   <tr><td>{@code /admin/**}</td><td>rôle {@code ADMIN}</td></tr>
     *   <tr><td>tout le reste</td><td>utilisateur connecté</td></tr>
     * </table>
     *
     * @param http configuration de la sécurité HTTP, fournie par Spring Security
     * @return la chaîne de filtres de sécurité
     * @throws Exception si la configuration est invalide
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // pages accessibles sans être connecté ; /error : page d'erreur de Spring Boot
                        .requestMatchers("/", "/connexion", "/inscription", "/error").permitAll()
                        // fichiers statiques (CSS généré par Tailwind, étape 8) : sinon la page de connexion serait sans style
                        .requestMatchers("/css/**").permitAll()
                        // espace client (étape 5) ; * = un segment d'URL quelconque (l'id)
                        .requestMatchers("/mes-participations", "/campagnes/*/participer", "/participations/*/annuler")
                                .hasRole("CLIENT")
                        // espace fournisseur (étape 6)
                        .requestMatchers("/mes-propositions", "/campagnes/*/proposer").hasRole("FOURNISSEUR")
                        // espace administrateur (étape 7) ; ** = n'importe quelle suite de segments
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // les règles sont lues dans l'ordre : la première qui correspond s'applique
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/connexion")              // notre page (GET), et l'URL où le formulaire est posté (POST)
                        .usernameParameter("email")           // noms des champs du formulaire
                        .passwordParameter("motDePasse")
                        .failureUrl("/connexion?erreur=true") // en cas d'échec
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/deconnexion")            // POST uniquement (protégé par CSRF)
                        .logoutSuccessUrl("/connexion?deconnexion=true"));
        return http.build();
    }
}
