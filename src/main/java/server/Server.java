package server;

import common.Protocol;
import persistence.HibernateUtil;
import server.handler.ClientHandler;
import server.logger.ServerLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private final Map<String, ClientHandler> clientsConnectes = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;

    public void demarrer() {
        try {
            // Initialisation Hibernate
            ServerLogger.info("Initialisation Hibernate...");
            HibernateUtil.getSessionFactory();
            ServerLogger.info("✅ Hibernate connecté à PostgreSQL !");

            // Démarrage du ServerSocket
            serverSocket = new ServerSocket(Protocol.PORT);
            ServerLogger.info("🚀 Serveur démarré sur le port " + Protocol.PORT);

            // ⚠️ Cette boucle doit être ICI — elle ne doit jamais s'arrêter
            while (true) {
                // Attente d'un client (bloquant)
                Socket clientSocket = serverSocket.accept();
                ServerLogger.info("🔌 Nouvelle connexion : " + clientSocket.getInetAddress());

                // Un thread par client
                ClientHandler handler = new ClientHandler(clientSocket, clientsConnectes);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            ServerLogger.erreur("Erreur serveur : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Server().demarrer();
    }
}