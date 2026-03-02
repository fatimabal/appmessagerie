package client.network;

import common.Packet;
import common.Protocol;
import javafx.application.Platform;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.function.Consumer;

public class MessageListener implements Runnable {

    private final ObjectInputStream  in;
    private final Consumer<Packet>   onMessage;      // appelé quand message reçu
    private final Runnable           onDisconnect;   // appelé si connexion perdue
    private volatile boolean         running = true;

    public MessageListener(ObjectInputStream in,
                           Consumer<Packet> onMessage,
                           Runnable onDisconnect) {
        this.in           = in;
        this.onMessage    = onMessage;
        this.onDisconnect = onDisconnect;
    }

    @Override
    public void run() {
        try {
            while (running) {
                Packet packet = (Packet) in.readObject();
                if (packet != null) {
                    // Dispatch vers l'UI thread
                    Platform.runLater(() -> onMessage.accept(packet));
                }
            }
        } catch (IOException e) {
            // Connexion perdue — RG10
            if (running) {
                Platform.runLater(onDisconnect);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Packet invalide reçu : " + e.getMessage());
        }
    }

    public void arreter() {
        running = false;
    }
}