package fr.limoges.valadon.gestionfioul.web.api;

/**
 * Corps d'erreur renvoyé par l'API au format ProblemDetail (RFC 9457).
 *
 * <p>Exemple : {@code {"type":"about:blank","title":"Conflict","status":409,"detail":"Ce client participe déjà...","instance":"..."}}.
 * On ne garde que les champs utiles ; les autres sont ignorés par Jackson.</p>
 *
 * @param title  intitulé standard du code HTTP ({@code Conflict}...)
 * @param status code HTTP
 * @param detail explication rédigée par l'API, affichée à l'utilisateur
 */
public record ProblemeApi(String title, int status, String detail) {
}
