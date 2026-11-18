/**
 * Campagnes de commande groupée : liste des campagnes ouvertes, statuts et actions du cycle de vie.
 *
 * <p>Cycle de vie d'une campagne (imposé par l'API) :
 * {@code BROUILLON → OUVERTE → CLOTUREE → ATTRIBUEE → LIVREE}, avec une annulation possible
 * jusqu'à la livraison. Voir {@link fr.limoges.valadon.gestionfioul.web.campagne.StatutCampagne#actionsPossibles()}.</p>
 */
package fr.limoges.valadon.gestionfioul.web.campagne;
