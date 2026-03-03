package server.logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ServerLogger — RG12
 * Gère la journalisation de toutes les actions du serveur.
 * Écrit à la fois dans la console ET dans le fichier server.log
 */
public class ServerLogger {

    // Nom du fichier de log généré à la racine du projet
    private static final String LOG_FILE = "server.log";

    // Format de la date : ex → 2024-03-15 14:32:05
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Empêche qu'on fasse "new ServerLogger()"
    // Toutes les méthodes sont static, pas besoin d'instance
    private ServerLogger() {}

    // ═══════════════════════════════════════════════════
    //   MÉTHODE CENTRALE : toutes les autres passent ici
    // ═══════════════════════════════════════════════════

    /**
     * Écrit une ligne de log dans la console et dans server.log
     * Format : [2024-03-15 14:32:05] [NIVEAU] message
     */
    private static void log(String niveau, String message) {

        // Construction de la ligne de log
        String ligne = "[" + LocalDateTime.now().format(formatter) + "] "
                + "[" + niveau + "] "
                + message;

        // 1. Affichage dans la console IntelliJ
        System.out.println(ligne);

        // 2. Écriture dans le fichier server.log
        // "true" = mode append (on ajoute, on n'écrase pas)
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println(ligne);
        } catch (IOException e) {
            System.err.println("❌ Impossible d'écrire dans server.log : " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════
    //   MÉTHODES PUBLIQUES — utilisées partout
    // ═══════════════════════════════════════════════════

    /**
     * Log d'information générale
     * Exemple : ServerLogger.info("Serveur démarré sur le port 8080")
     * → [2024-03-15 14:32:05] [INFO] Serveur démarré sur le port 8080
     */
    public static void info(String message) {
        log("INFO   ", message);
    }

    /**
     * Log d'erreur
     * Exemple : ServerLogger.erreur("Connexion BDD échouée")
     * → [2024-03-15 14:32:05] [ERREUR] Connexion BDD échouée
     */
    public static void erreur(String message) {
        log("ERREUR ", message);
    }

    /**
     * Log de connexion d'un utilisateur — RG12
     * Appelé dans ClientHandler quand un LOGIN réussit
     * Exemple : ServerLogger.connexion("alice")
     * → [2024-03-15 14:32:05] [CONNEXION] Utilisateur connecté : alice
     */
    public static void connexion(String username) {
        log("CONNEXION   ", "Utilisateur connecté : " + username);
    }

    /**
     * Log de déconnexion d'un utilisateur — RG12
     * Appelé dans ClientHandler quand un client se déconnecte
     *
     * @param username le nom de l'utilisateur déconnecté
     * @param raison   "Déconnexion normale" ou "Perte de connexion réseau"
     *
     * Exemple : ServerLogger.deconnexion("alice", "Déconnexion normale")
     * → [2024-03-15 14:32:05] [DECONNEXION] alice | Raison : Déconnexion normale
     */
    public static void deconnexion(String username, String raison) {
        log("DECONNEXION ", "Utilisateur déconnecté : " + username + " | Raison : " + raison);
    }

    /**
     * Log d'un message envoyé entre deux utilisateurs — RG12
     * Appelé dans ClientHandler quand un SEND_MSG est reçu
     *
     * Exemple : ServerLogger.message("alice", "bob")
     * → [2024-03-15 14:32:05] [MESSAGE] De : alice → À : bob
     */
    public static void message(String expediteur, String destinataire) {
        log("MESSAGE     ", "De : " + expediteur + " ──► À : " + destinataire);
    }
}
