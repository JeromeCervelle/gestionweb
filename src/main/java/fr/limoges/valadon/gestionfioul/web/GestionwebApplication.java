package fr.limoges.valadon.gestionfioul.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du site web gestionfioul.
 *
 * <p>Le site n'accède jamais à MongoDB : il passe par l'API REST (port 9999).</p>
 */
@SpringBootApplication
public class GestionwebApplication {

	/**
	 * Démarre le site (Tomcat embarqué, port 9998).
	 *
	 * @param args arguments de la ligne de commande, transmis à Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(GestionwebApplication.class, args);
	}

}
