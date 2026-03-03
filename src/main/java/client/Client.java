package client;

import common.Packet;
import common.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    /**
     * Établit la connexion au serveur
     * @return true si connexion réussie, false sinon
     */
    public boolean connecter() {
        try {
            socket = new Socket(Protocol.HOST, Protocol.PORT);

            // ⚠️ IMPORTANT : toujours créer OUT avant IN
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // vider le buffer tout de suite

            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("✅ Connecté au serveur " + Protocol.HOST + ":" + Protocol.PORT);
            return true;

        } catch (IOException e) {
            System.err.println("❌ Connexion échouée : " + e.getMessage());
            return false;
        }
    }

    /**
     * Envoie un Packet au serveur
     */
    public void envoyer(Packet packet) {
        try {
            out.writeObject(packet);
            out.flush();
        } catch (IOException e) {
            System.err.println("❌ Erreur envoi : " + e.getMessage());
        }
    }

    /**
     * Reçoit un Packet du serveur (bloquant)
     */
    public Packet recevoir() {
        try {
            return (Packet) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Erreur réception : " + e.getMessage());
            return null;
        }
    }

    /**
     * Ferme la connexion proprement
     */
    public void deconnecter() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("🔌 Déconnecté du serveur.");
            }
        } catch (IOException ignored) {}
    }

    public boolean estConnecte() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public ObjectInputStream getIn()    { return in; }
    public ObjectOutputStream getOut()  { return out; }
    public Socket getSocket()           { return socket; }
}
