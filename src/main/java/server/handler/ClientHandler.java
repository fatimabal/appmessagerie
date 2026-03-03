package server.handler;

import common.Packet;
import common.Protocol;
import entity.Message;
import entity.MessageStatus;
import server.logger.ServerLogger;
import server.service.MessageService;
import server.service.UserService;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Map<String, ClientHandler> clientsConnectes;

    // Les services métier
    private final UserService    userService    = new UserService();
    private final MessageService messageService = new MessageService();

    private ObjectInputStream  in;
    private ObjectOutputStream out;
    private String usernameConnecte;

    public ClientHandler(Socket socket, Map<String, ClientHandler> clientsConnectes) {
        this.socket           = socket;
        this.clientsConnectes = clientsConnectes;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in  = new ObjectInputStream(socket.getInputStream());

            Packet packet;
            while ((packet = (Packet) in.readObject()) != null) {
                traiterPacket(packet);
            }

        } catch (IOException e) {
            deconnecter("Perte de connexion réseau");
        } catch (ClassNotFoundException e) {
            ServerLogger.erreur("Packet invalide : " + e.getMessage());
        }
    }

    private void traiterPacket(Packet packet) {
        switch (packet.getType()) {
            case Protocol.LOGIN       -> traiterLogin(packet);
            case Protocol.REGISTER    -> traiterInscription(packet);
            case Protocol.SEND_MSG    -> traiterMessage(packet);
            case Protocol.LOGOUT      -> deconnecter("Déconnexion normale");
            case Protocol.GET_HISTORY -> traiterHistorique(packet); // ← NOUVEAU
            default -> ServerLogger.erreur("Type inconnu : " + packet.getType());
        }
    }

    // ← NOUVELLE MÉTHODE
    private void traiterHistorique(Packet packet) {
        String user1 = packet.getSender();
        String user2 = packet.getReceiver();

        List<Message> historique = messageService.getHistorique(user1, user2);

        // Sérialiser l'historique en une chaîne
        // Format : "expediteur|contenu|timestamp||expediteur|contenu|timestamp"
        StringBuilder sb = new StringBuilder();
        for (Message msg : historique) {
            if (!sb.isEmpty()) sb.append("||");
            sb.append(msg.getSender().getUsername())
                    .append("|")
                    .append(msg.getContenu())
                    .append("|")
                    .append(msg.getDateEnvoi().toString());
        }

        Packet reponse = new Packet();
        reponse.setType(Protocol.HISTORY);
        reponse.setReceiver(user2);
        reponse.setContent(sb.toString());
        envoyer(reponse);
    }

    private void traiterLogin(Packet packet) {
        boolean dejaConnecte = clientsConnectes.containsKey(packet.getSender());
        Packet reponse = userService.connecter(packet, dejaConnecte);
        envoyer(reponse);

        if (Protocol.SUCCESS.equals(reponse.getType())) {
            usernameConnecte = packet.getSender();
            clientsConnectes.put(usernameConnecte, this);

            // Envoyer les messages en attente — RG6
            envoyerMessagesEnAttente();

            // Informer les autres clients du nouveau connecté
            diffuserStatut(usernameConnecte, "ONLINE");

            // ← AJOUTE CECI : envoyer la liste complète des connectés au nouveau
            envoyerListeUsers();
        }
    }

    private void envoyerListeUsers() {
        // Construire la liste de tous les connectés
        String liste = String.join(",", clientsConnectes.keySet());

        Packet packet = new Packet();
        packet.setType(Protocol.USER_LIST);
        packet.setContent(liste);
        envoyer(packet);
    }

    private void traiterInscription(Packet packet) {
        Packet reponse = userService.inscrire(packet);
        envoyer(reponse);
    }

    private void traiterMessage(Packet packet) {
        // Sauvegarder en BDD
        Message message = messageService.envoyer(packet);
        if (message == null) return;

        String destinataire = packet.getReceiver();
        ClientHandler handlerDestinataire = clientsConnectes.get(destinataire);

        if (handlerDestinataire != null) {
            // Livraison immédiate
            Packet msg = new Packet(Protocol.RECEIVE_MSG,
                    packet.getSender(),
                    destinataire,
                    packet.getContent());
            msg.setStatus(String.valueOf(message.getId()));
            handlerDestinataire.envoyer(msg);

            // ✅ NOUVEAU : notifier l'expéditeur → ✓✓
            notifierStatutMessage(packet.getSender(), message.getId(), "RECU");

        } else {
            ServerLogger.info("Destinataire " + destinataire + " hors ligne.");
        }
    }

    // ✅ NOUVELLE MÉTHODE
    private void notifierStatutMessage(String expediteur, Long messageId, String statut) {
        ClientHandler handlerExpediteur = clientsConnectes.get(expediteur);
        if (handlerExpediteur != null) {
            Packet notification = new Packet();
            notification.setType(Protocol.MSG_STATUS);
            notification.setContent(messageId + "|" + statut);
            handlerExpediteur.envoyer(notification);
        }
    }
    private void envoyerMessagesEnAttente() {
        List<Message> enAttente = messageService.getMessagesEnAttente(usernameConnecte);

        for (Message msg : enAttente) {
            Packet packet = new Packet(
                    Protocol.RECEIVE_MSG,
                    msg.getSender().getUsername(),
                    usernameConnecte,
                    msg.getContenu()
            );
            packet.setStatus(String.valueOf(msg.getId()));
            envoyer(packet);
        }

        if (!enAttente.isEmpty()) {
            ServerLogger.info(enAttente.size() + " messages en attente livrés à " + usernameConnecte);
        }
    }

    private void deconnecter(String raison) {
        if (usernameConnecte != null) {
            userService.deconnecter(usernameConnecte, raison);
            clientsConnectes.remove(usernameConnecte);
            diffuserStatut(usernameConnecte, "OFFLINE");
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    private void diffuserStatut(String username, String statut) {
        Packet statusPacket = new Packet();
        statusPacket.setType(Protocol.STATUS_UPDATE);
        statusPacket.setSender(username);
        statusPacket.setContent(statut);

        for (ClientHandler handler : clientsConnectes.values()) {
            if (!handler.usernameConnecte.equals(username)) {
                handler.envoyer(statusPacket);
            }
        }
    }

    public synchronized void envoyer(Packet packet) {
        try {
            out.writeObject(packet);
            out.flush();
        } catch (IOException e) {
            ServerLogger.erreur("Erreur envoi à " + usernameConnecte + " : " + e.getMessage());
        }
    }

    public String getUsernameConnecte() {
        return usernameConnecte;
    }
}