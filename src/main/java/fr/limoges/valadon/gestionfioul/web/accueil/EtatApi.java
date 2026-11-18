package fr.limoges.valadon.gestionfioul.web.accueil;

/**
 * Ce que la page d'accueil affiche sur l'API : joignable ou non, et si oui son nom et sa version.
 *
 * @param disponible {@code true} si l'API a répondu
 * @param nom        nom de l'API (titre de sa documentation OpenAPI), {@code null} si indisponible
 * @param version    version de l'API, {@code null} si indisponible
 */
public record EtatApi(boolean disponible, String nom, String version) {

    /**
     * État d'une API qui a répondu.
     *
     * @param nom     nom de l'API
     * @param version version de l'API
     * @return un état « disponible »
     */
    public static EtatApi disponible(String nom, String version) {
        return new EtatApi(true, nom, version);
    }

    /**
     * État d'une API arrêtée ou injoignable.
     *
     * @return un état « indisponible », sans nom ni version
     */
    public static EtatApi indisponible() {
        return new EtatApi(false, null, null);
    }
}
