package fr.limoges.valadon.gestionfioul.web.security;

import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.connecte;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.unAdmin;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.unClient;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.unFournisseur;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import fr.limoges.valadon.gestionfioul.web.accueil.EtatApi;
import fr.limoges.valadon.gestionfioul.web.accueil.EtatApiClient;
import fr.limoges.valadon.gestionfioul.web.campagne.CampagneClient;
import fr.limoges.valadon.gestionfioul.web.participation.ParticipationClient;
import fr.limoges.valadon.gestionfioul.web.proposition.BaremeValidator;
import fr.limoges.valadon.gestionfioul.web.proposition.PropositionClient;
import fr.limoges.valadon.gestionfioul.web.user.Role;
import fr.limoges.valadon.gestionfioul.web.user.UtilisateurClient;

/**
 * La matrice des droits du site, en UN seul endroit : chaque route, et qui peut l'utiliser.
 *
 * @WebMvcTest SANS contrôleur précisé : TOUS les contrôleurs sont chargés (les clients de l'API sont simulés).
 * On vérifie les REFUS (403 pour un rôle non autorisé, redirection vers /connexion pour un visiteur) :
 * l'accès autorisé est déjà vérifié, avec des données réalistes, dans le test de chaque contrôleur.
 *
 * Le dernier test compare la matrice aux routes réellement déclarées par les contrôleurs : ajouter une route
 * sans l'ajouter ici fait échouer la build.
 */
@WebMvcTest
// BaremeValidator est un simple @Component : @WebMvcTest ne le charge pas, il faut l'importer
@Import({SecurityConfig.class, BaremeValidator.class})
class MatriceDesDroitsTest {

    /** Une route du site : méthode HTTP, motif déclaré dans le contrôleur, exemple d'URL, rôles autorisés. */
    record Route(HttpMethod methode, String motif, String url, Set<Role> autorises) {

        /** Ensemble vide : tout utilisateur connecté. */
        static final Set<Role> CONNECTE = Set.of();

        boolean publique() {
            return autorises == null;
        }

        String cle() {
            return methode.name() + " " + motif;
        }

        @Override
        public String toString() {
            return methode.name() + " " + url;
        }
    }

    private static final HttpMethod GET = HttpMethod.GET;
    private static final HttpMethod POST = HttpMethod.POST;

    private static final List<Route> ROUTES = List.of(
            // publiques (autorises = null)
            new Route(GET, "/", "/", null),
            new Route(GET, "/connexion", "/connexion", null),
            new Route(GET, "/inscription", "/inscription", null),
            new Route(POST, "/inscription", "/inscription", null),
            // tout utilisateur connecté
            new Route(GET, "/campagnes", "/campagnes", Route.CONNECTE),
            // client
            new Route(GET, "/campagnes/{campagneId}/participer", "/campagnes/c1/participer", Set.of(Role.CLIENT)),
            new Route(POST, "/campagnes/{campagneId}/participer", "/campagnes/c1/participer", Set.of(Role.CLIENT)),
            new Route(GET, "/mes-participations", "/mes-participations", Set.of(Role.CLIENT)),
            new Route(POST, "/participations/{id}/annuler", "/participations/p1/annuler", Set.of(Role.CLIENT)),
            // fournisseur
            new Route(GET, "/campagnes/{campagneId}/proposer", "/campagnes/c1/proposer", Set.of(Role.FOURNISSEUR)),
            new Route(POST, "/campagnes/{campagneId}/proposer", "/campagnes/c1/proposer", Set.of(Role.FOURNISSEUR)),
            new Route(GET, "/mes-propositions", "/mes-propositions", Set.of(Role.FOURNISSEUR)),
            // administrateur
            new Route(GET, "/admin/campagnes", "/admin/campagnes", Set.of(Role.ADMIN)),
            new Route(GET, "/admin/campagnes/nouvelle", "/admin/campagnes/nouvelle", Set.of(Role.ADMIN)),
            new Route(POST, "/admin/campagnes/nouvelle", "/admin/campagnes/nouvelle", Set.of(Role.ADMIN)),
            new Route(GET, "/admin/campagnes/{id}", "/admin/campagnes/c1", Set.of(Role.ADMIN)),
            new Route(POST, "/admin/campagnes/{id}/action", "/admin/campagnes/c1/action?action=OUVRIR", Set.of(Role.ADMIN)));

    private static final Map<Role, UtilisateurConnecte> UTILISATEURS = Map.of(
            Role.CLIENT, unClient(),
            Role.FOURNISSEUR, unFournisseur(),
            Role.ADMIN, unAdmin());

    @Autowired
    private MockMvc mockMvc;

    // tous les contrôleurs sont chargés : il faut simuler tous les clients de l'API
    @MockitoBean
    private EtatApiClient etatApiClient;
    @MockitoBean
    private CampagneClient campagneClient;
    @MockitoBean
    private ParticipationClient participationClient;
    @MockitoBean
    private PropositionClient propositionClient;
    @MockitoBean
    private UtilisateurClient utilisateurClient;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping routesDeclarees;

    @BeforeEach
    void apiDisponible() {
        when(etatApiClient.consulter()).thenReturn(EtatApi.disponible("Gestion Fioul", "1.0"));
    }

    /** Requête vers la route, avec un jeton CSRF pour les POST : on teste les DROITS, pas le CSRF. */
    private static MockHttpServletRequestBuilder requete(Route route) {
        MockHttpServletRequestBuilder requete = request(route.methode(), route.url());
        return route.methode() == POST ? requete.with(csrf()) : requete;
    }

    static Stream<Route> routesProtegees() {
        return ROUTES.stream().filter(route -> !route.publique());
    }

    /** Toutes les combinaisons (route protégée, rôle NON autorisé). */
    static Stream<Arguments> accesInterdits() {
        return routesProtegees()
                .filter(route -> !route.autorises().isEmpty())
                .flatMap(route -> Stream.of(Role.values())
                        .filter(role -> !route.autorises().contains(role))
                        .map(role -> Arguments.of(route, role)));
    }

    @ParameterizedTest(name = "{0} interdit au rôle {1}")
    @MethodSource("accesInterdits")
    void un_role_non_autorise_recoit_403(Route route, Role role) throws Exception {
        mockMvc.perform(requete(route).with(connecte(UTILISATEURS.get(role))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0} : visiteur renvoyé vers la connexion")
    @MethodSource("routesProtegees")
    void un_visiteur_est_renvoye_vers_la_connexion(Route route) throws Exception {
        mockMvc.perform(requete(route))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion"));
    }

    static Stream<Route> postsProteges() {
        return routesProtegees().filter(route -> route.methode() == POST);
    }

    @ParameterizedTest(name = "{0} sans jeton CSRF : 403")
    @MethodSource("postsProteges")
    void un_post_sans_jeton_csrf_est_refuse_meme_au_bon_role(Route route) throws Exception {
        Role role = route.autorises().iterator().next();
        mockMvc.perform(request(POST, route.url()).with(connecte(UTILISATEURS.get(role))))
                .andExpect(status().isForbidden());
    }

    static Stream<String> pagesPubliques() {
        return Stream.concat(ROUTES.stream().filter(Route::publique).filter(r -> r.methode() == GET).map(Route::url),
                Stream.of("/css/app.css"));
    }

    @ParameterizedTest(name = "GET {0} accessible sans être connecté")
    @MethodSource("pagesPubliques")
    void les_pages_publiques_sont_accessibles_aux_visiteurs(String url) throws Exception {
        mockMvc.perform(request(GET, url)).andExpect(status().isOk());
    }

    @Test
    void la_matrice_couvre_toutes_les_routes_des_controleurs() {
        Set<String> declarees = new TreeSet<>();
        routesDeclarees.getHandlerMethods().forEach((info, methode) -> {
            // on ne garde que NOS contrôleurs (pas le /error de Spring Boot)
            if (methode.getBeanType().getPackageName().startsWith("fr.limoges.valadon.gestionfioul.web")) {
                declarees.addAll(cles(info));
            }
        });
        Set<String> dansLaMatrice = new TreeSet<>(ROUTES.stream().map(Route::cle).toList());

        assertThat(dansLaMatrice)
                .as("chaque route d'un contrôleur doit figurer dans la matrice des droits (et inversement)")
                .isEqualTo(declarees);
    }

    /** "GET /campagnes", "POST /campagnes/{campagneId}/proposer"… pour une méthode de contrôleur. */
    private static Set<String> cles(RequestMappingInfo info) {
        Set<String> cles = new TreeSet<>();
        info.getMethodsCondition().getMethods().forEach(methode ->
                info.getPathPatternsCondition().getPatternValues().forEach(motif ->
                        cles.add(methode.name() + " " + motif)));
        return cles;
    }
}
