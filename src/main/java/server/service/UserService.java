package server.service;

import common.Packet;
import common.Protocol;
import entity.User;
import entity.UserStatus;
import org.mindrot.jbcrypt.BCrypt;
import server.repository.UserRepository;
import server.logger.ServerLogger;

public class UserService {

    private final UserRepository userRepository = new UserRepository();

    /**
     * Inscription d'un nouvel utilisateur — RG1, RG9
     *
     * RG1 : username unique
     * RG9 : mot de passe hashé avec BCrypt
     */
    public Packet inscrire(Packet packet) {
        String username = packet.getSender();
        String password = packet.getContent();

        // RG1 : vérifier que le username n'existe pas déjà
        if (userRepository.usernameExiste(username)) {
            ServerLogger.erreur("Inscription échouée : username déjà pris → " + username);

            Packet erreur = new Packet();
            erreur.setType(Protocol.ERROR);
            erreur.setContent("Ce nom d'utilisateur est déjà pris.");
            return erreur;
        }

        // RG9 : hasher le mot de passe avec BCrypt
        String passwordHashe = BCrypt.hashpw(password, BCrypt.gensalt());

        // Créer et sauvegarder l'utilisateur
        User nouvelUser = new User(username, passwordHashe, null);
        userRepository.sauvegarder(nouvelUser);

        ServerLogger.info("Nouvel utilisateur inscrit : " + username);

        Packet succes = new Packet();
        succes.setType(Protocol.SUCCESS);
        succes.setContent("Inscription réussie ! Vous pouvez vous connecter.");
        return succes;
    }

    /**
     * Connexion d'un utilisateur — RG2, RG3, RG9
     *
     * RG2 : vérifier que le compte existe
     * RG3 : session unique (pas 2 connexions simultanées)
     * RG9 : vérification BCrypt
     */
    public Packet connecter(Packet packet, boolean dejaConnecte) {
        String username = packet.getSender();
        String password = packet.getContent();

        // RG3 : vérifier session unique
        if (dejaConnecte) {
            Packet erreur = new Packet();
            erreur.setType(Protocol.ERROR);
            erreur.setContent("Ce compte est déjà connecté sur un autre appareil.");
            return erreur;
        }

        // RG2 : vérifier que l'utilisateur existe
        User user = userRepository.trouverParUsername(username);
        if (user == null) {
            Packet erreur = new Packet();
            erreur.setType(Protocol.ERROR);
            erreur.setContent("Nom d'utilisateur introuvable.");
            return erreur;
        }

        // RG9 : vérifier le mot de passe avec BCrypt
        if (!BCrypt.checkpw(password, user.getPassword())) {
            Packet erreur = new Packet();
            erreur.setType(Protocol.ERROR);
            erreur.setContent("Mot de passe incorrect.");
            return erreur;
        }

        // Mettre le statut ONLINE — RG4
        userRepository.mettreAJourStatut(username, UserStatus.ONLINE);
        ServerLogger.connexion(username);

        Packet succes = new Packet();
        succes.setType(Protocol.SUCCESS);
        succes.setContent("Connexion réussie ! Bienvenue " + username);
        return succes;
    }

    /**
     * Déconnexion d'un utilisateur — RG4, RG10
     * Passe le statut en OFFLINE
     */
    public void deconnecter(String username, String raison) {
        userRepository.mettreAJourStatut(username, UserStatus.OFFLINE);
        ServerLogger.deconnexion(username, raison);
    }
}