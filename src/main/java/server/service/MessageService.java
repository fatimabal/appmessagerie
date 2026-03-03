package server.service;

import common.Packet;
import entity.Message;
import entity.MessageStatus;
import entity.User;
import server.logger.ServerLogger;
import server.repository.MessageRepository;
import server.repository.UserRepository;

import java.util.List;

public class MessageService {

    private final MessageRepository messageRepository = new MessageRepository();
    private final UserRepository    userRepository    = new UserRepository();

    /**
     * Envoie un message — RG5, RG7
     *
     * RG5 : l'expéditeur doit être connecté (vérifié dans ClientHandler)
     * RG7 : le message ne doit pas être vide et < 1000 caractères
     *
     * @return le Message sauvegardé, ou null si erreur
     */
    public Message envoyer(Packet packet) {
        String contenu        = packet.getContent();
        String usernameEmett  = packet.getSender();
        String usernameDestin = packet.getReceiver();

        // RG7 : validation du contenu
        if (contenu == null || contenu.trim().isEmpty()) {
            ServerLogger.erreur("Message vide rejeté de : " + usernameEmett);
            return null;
        }
        if (contenu.length() > 1000) {
            ServerLogger.erreur("Message trop long rejeté de : " + usernameEmett);
            return null;
        }

        // Vérifier que le destinataire existe — RG5
        User destinataire = userRepository.trouverParUsername(usernameDestin);
        if (destinataire == null) {
            ServerLogger.erreur("Destinataire introuvable : " + usernameDestin);
            return null;
        }

        User expediteur = userRepository.trouverParUsername(usernameEmett);

        // Créer et sauvegarder le message
        Message message = new Message(expediteur, destinataire, contenu.trim());
        messageRepository.sauvegarder(message);

        ServerLogger.message(usernameEmett, usernameDestin);
        return message;
    }

    /**
     * Récupère l'historique entre deux utilisateurs — RG8
     */
    public List<Message> getHistorique(String username1, String username2) {
        return messageRepository.trouverConversation(username1, username2);
    }

    /**
     * Récupère et retourne les messages en attente — RG6
     * Appelé quand un utilisateur se reconnecte
     */
    public List<Message> getMessagesEnAttente(String username) {
        List<Message> messages = messageRepository.trouverMessagesEnAttente(username);

        // Marquer ces messages comme RECU
        for (Message msg : messages) {
            messageRepository.mettreAJourStatut(msg.getId(), MessageStatus.RECU);
        }

        return messages;
    }

    /**
     * Marque un message comme LU
     */
    public void marquerCommeLu(Long messageId) {
        messageRepository.mettreAJourStatut(messageId, MessageStatus.LU);
    }
}