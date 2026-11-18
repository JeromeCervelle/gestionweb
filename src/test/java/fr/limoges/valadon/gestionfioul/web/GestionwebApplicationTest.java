package fr.limoges.valadon.gestionfioul.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import fr.limoges.valadon.gestionfioul.web.accueil.EtatApi;
import fr.limoges.valadon.gestionfioul.web.accueil.EtatApiClient;
import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.campagne.CampagneClient;
import fr.limoges.valadon.gestionfioul.web.campagne.StatutCampagne;
import fr.limoges.valadon.gestionfioul.web.user.Role;
import fr.limoges.valadon.gestionfioul.web.user.Utilisateur;
import fr.limoges.valadon.gestionfioul.web.user.UtilisateurClient;

/**
 * Test d'INTÉGRATION : l'application ENTIÈRE démarre, avec un vrai serveur Tomcat sur un port libre choisi au hasard.
 * On l'interroge comme le ferait un navigateur (client HTTP du JDK, avec cookies), sans MockMvc.
 *
 * Seuls les appels à l'API REST sont simulés (@MockitoBean) : le test ne dépend pas de l'API ni de MongoDB.
 *
 * Ce que ce test voit et que MockMvc ne voit pas : les pages d'erreur de Spring Boot (error/403.html),
 * le cookie de session JSESSIONID, les vraies redirections, le paramètre "?erreur" tel que Tomcat le transmet,
 * le fichier CSS servi depuis le disque.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class GestionwebApplicationTest {

    /** Le jeton CSRF caché dans les formulaires : <input type="hidden" name="_csrf" value="..."/>. */
    private static final Pattern JETON_CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @MockitoBean
    private EtatApiClient etatApiClient;
    @MockitoBean
    private UtilisateurClient utilisateurClient;
    @MockitoBean
    private CampagneClient campagneClient;

    /** Un "navigateur" : il garde les cookies (session) mais ne suit PAS les redirections, pour les vérifier. */
    private HttpClient navigateur;

    @BeforeEach
    void preparer() {
        navigateur = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        when(etatApiClient.consulter()).thenReturn(EtatApi.disponible("Gestion Fioul", "1.0"));
        when(utilisateurClient.moi("alice@test.fr", "motdepasse1")).thenReturn(
                new Utilisateur("u1", "alice@test.fr", "Durand", "Alice", null, Role.CLIENT, null, null));
        when(utilisateurClient.moi("alice@test.fr", "faux"))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Authentification refusée"));
        when(campagneClient.listerParStatut(StatutCampagne.OUVERTE)).thenReturn(List.of());
    }

    // ---------------------------------------------------------------- outils "navigateur"

    private HttpResponse<String> get(String chemin) throws IOException, InterruptedException {
        HttpRequest requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                .header("Accept", "text/html")   // comme un navigateur : pages d'erreur en HTML, pas en JSON
                .GET().build();
        return navigateur.send(requete, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String chemin, Map<String, String> champs) throws IOException, InterruptedException {
        String corps = champs.entrySet().stream()
                .map(champ -> champ.getKey() + "=" + URLEncoder.encode(champ.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + chemin))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html")
                .POST(HttpRequest.BodyPublishers.ofString(corps)).build();
        return navigateur.send(requete, HttpResponse.BodyHandlers.ofString());
    }

    private static String jetonCsrf(HttpResponse<String> page) {
        Matcher matcher = JETON_CSRF.matcher(page.body());
        assertThat(matcher.find()).as("la page doit contenir un formulaire avec un jeton CSRF").isTrue();
        return matcher.group(1);
    }

    private static String redirection(HttpResponse<String> reponse) {
        assertThat(reponse.statusCode()).isEqualTo(302);
        return reponse.headers().firstValue("Location").orElseThrow();
    }

    /** Ouvre la page de connexion (pour obtenir session + jeton) puis poste le formulaire. */
    private HttpResponse<String> seConnecter(String email, String motDePasse) throws Exception {
        String jeton = jetonCsrf(get("/connexion"));
        return post("/connexion", Map.of("email", email, "motDePasse", motDePasse, "_csrf", jeton));
    }

    // ---------------------------------------------------------------- tests

    @Test
    void l_application_demarre_et_sert_l_accueil_avec_sa_feuille_de_style() throws Exception {
        HttpResponse<String> accueil = get("/");
        assertThat(accueil.statusCode()).isEqualTo(200);
        assertThat(accueil.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type).startsWith("text/html"));
        assertThat(accueil.body()).contains("Connecté à l'API", "href=\"/css/app.css\"");

        HttpResponse<String> css = get("/css/app.css");
        assertThat(css.statusCode()).isEqualTo(200);
        assertThat(css.body()).contains(".bg-amber-600");
    }

    @Test
    void parcours_complet_connexion_page_protegee_puis_deconnexion() throws Exception {
        // 1. page protégée sans être connecté : redirection vers la connexion
        assertThat(redirection(get("/campagnes"))).endsWith("/connexion");

        // 2. connexion réussie : Spring Security a mémorisé la page demandée à l'étape 1 (requête sauvegardée,
        //    "RequestCache") et y renvoie l'utilisateur, avec le paramètre ?continue (et non vers l'accueil)
        assertThat(redirection(seConnecter("alice@test.fr", "motdepasse1"))).endsWith("/campagnes?continue");
        assertThat(get("/").body()).contains("Alice Durand", "Déconnexion");

        // 3. la page protégée est maintenant accessible (le cookie JSESSIONID identifie la session)
        HttpResponse<String> campagnes = get("/campagnes?continue");
        assertThat(campagnes.statusCode()).isEqualTo(200);
        assertThat(campagnes.body()).contains("Aucune campagne n'est ouverte pour le moment.");

        // 4. déconnexion (POST avec le jeton CSRF de la page courante), puis la page protégée est refermée
        String jeton = jetonCsrf(get("/"));
        assertThat(redirection(post("/deconnexion", Map.of("_csrf", jeton)))).endsWith("/connexion?deconnexion=true");
        assertThat(get("/connexion?deconnexion=true").body()).contains("Vous êtes déconnecté.");
        assertThat(redirection(get("/campagnes"))).endsWith("/connexion");
    }

    @Test
    void mauvais_mot_de_passe_message_d_erreur_sur_la_page_de_connexion() throws Exception {
        String adresse = redirection(seConnecter("alice@test.fr", "faux"));
        assertThat(adresse).endsWith("/connexion?erreur=true");

        // la page suivante lit le message d'échec rangé en session par Spring Security
        assertThat(get("/connexion?erreur=true").body()).contains("Email ou mot de passe incorrect.");
    }

    @Test
    void un_client_sur_une_page_d_administration_voit_la_page_403_du_site() throws Exception {
        seConnecter("alice@test.fr", "motdepasse1");

        HttpResponse<String> reponse = get("/admin/campagnes");

        // error/403.html : seule une vraie exécution (Tomcat + contrôleur d'erreur de Spring Boot) l'affiche
        assertThat(reponse.statusCode()).isEqualTo(403);
        assertThat(reponse.body()).contains("Accès refusé", "Cette page n'est pas accessible avec votre type de compte.");
    }

    @Test
    void adresse_inconnue_page_404_du_site_et_non_la_page_whitelabel() throws Exception {
        seConnecter("alice@test.fr", "motdepasse1");   // un visiteur, lui, serait renvoyé vers la connexion

        HttpResponse<String> reponse = get("/page-qui-n-existe-pas");

        assertThat(reponse.statusCode()).isEqualTo(404);
        assertThat(reponse.body())
                .contains("Page introuvable", "/page-qui-n-existe-pas", "Gestion Fioul")   // gabarit du site
                .doesNotContain("Whitelabel");
    }

    @Test
    void autre_erreur_page_generique_sans_details_techniques() throws Exception {
        seConnecter("alice@test.fr", "motdepasse1");

        // l'annulation n'existe qu'en POST : un GET donne 405 (Method Not Allowed), qui n'a pas de page dédiée
        HttpResponse<String> reponse = get("/participations/p1/annuler");

        assertThat(reponse.statusCode()).isEqualTo(405);
        assertThat(reponse.body())
                .contains("Une erreur est survenue", "405")
                .doesNotContain("Whitelabel", "Exception");
    }

    @Test
    void sans_jeton_csrf_la_connexion_est_refusee() throws Exception {
        get("/connexion");   // obtient une session, mais on "oublie" le jeton

        HttpResponse<String> reponse = post("/connexion", Map.of("email", "alice@test.fr", "motDePasse", "motdepasse1"));

        assertThat(reponse.statusCode()).isEqualTo(403);
    }
}
