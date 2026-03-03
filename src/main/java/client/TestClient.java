package client;

import common.Packet;
import common.Protocol;

public class TestClient {
    public static void main(String[] args) throws InterruptedException {

        Client client = new Client();
        if (!client.connecter()) return;

        // Test 1 : Inscription
        Packet inscription = new Packet(Protocol.REGISTER, "alice", null, "motdepasse123");
        client.envoyer(inscription);
        Packet rep1 = client.recevoir();
        System.out.println("Inscription : " + rep1.getType() + " → " + rep1.getContent());

        // Test 2 : Connexion
        Packet login = new Packet(Protocol.LOGIN, "alice", null, "motdepasse123");
        client.envoyer(login);
        Packet rep2 = client.recevoir();
        System.out.println("Login : " + rep2.getType() + " → " + rep2.getContent());

        client.deconnecter();
    }
}
