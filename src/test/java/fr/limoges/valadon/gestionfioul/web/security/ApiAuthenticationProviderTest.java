package fr.limoges.valadon.gestionfioul.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.ResourceAccessException;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.user.Role;
import fr.limoges.valadon.gestionfioul.web.user.Utilisateur;
import fr.limoges.valadon.gestionfioul.web.user.UtilisateurClient;

/**
 * Test unitaire pur (Mockito, sans Spring) : l'API est simulée par un mock de UtilisateurClient.
 */
@ExtendWith(MockitoExtension.class)
class ApiAuthenticationProviderTest {

    @Mock
    private UtilisateurClient utilisateurClient;

    @InjectMocks
    private ApiAuthenticationProvider provider;

    private static Authentication saisie(String email, String motDePasse) {
        return UsernamePasswordAuthenticationToken.unauthenticated(email, motDePasse);
    }

    @Test
    void identifiants_acceptes_par_l_api_donnent_un_utilisateur_connecte() {
        when(utilisateurClient.moi("alice@test.fr", "motdepasse1")).thenReturn(
                new Utilisateur("u1", "alice@test.fr", "Durand", "Alice", null, Role.CLIENT, "1 rue A", null));

        Authentication resultat = provider.authenticate(saisie("alice@test.fr", "motdepasse1"));

        assertThat(resultat.isAuthenticated()).isTrue();
        assertThat(resultat.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_CLIENT");
        UtilisateurConnecte principal = (UtilisateurConnecte) resultat.getPrincipal();
        assertThat(principal.id()).isEqualTo("u1");
        assertThat(principal.role()).isEqualTo(Role.CLIENT);
        // gardé pour les appels suivants à l'API
        assertThat(principal.motDePasse()).isEqualTo("motdepasse1");
        // mais jamais affiché par toString()
        assertThat(principal.toString()).doesNotContain("motdepasse1");
    }

    @Test
    void api_401_donne_bad_credentials() {
        when(utilisateurClient.moi("alice@test.fr", "faux"))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Authentification refusée"));

        assertThatThrownBy(() -> provider.authenticate(saisie("alice@test.fr", "faux")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email ou mot de passe incorrect.");
    }

    @Test
    void autre_erreur_de_l_api_donne_une_erreur_de_service() {
        when(utilisateurClient.moi("alice@test.fr", "motdepasse1"))
                .thenThrow(new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "L'API a renvoyé une erreur (code 500)."));

        assertThatThrownBy(() -> provider.authenticate(saisie("alice@test.fr", "motdepasse1")))
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessageContaining("code 500");
    }

    @Test
    void api_injoignable_donne_une_erreur_de_service() {
        when(utilisateurClient.moi("alice@test.fr", "motdepasse1"))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> provider.authenticate(saisie("alice@test.fr", "motdepasse1")))
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessageContaining("momentanément indisponible");
    }

    @Test
    void ne_traite_que_les_connexions_par_identifiant_et_mot_de_passe() {
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(Authentication.class)).isFalse();
    }
}
