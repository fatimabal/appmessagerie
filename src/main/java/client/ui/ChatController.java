package client.ui;

import client.Client;
import client.network.MessageListener;
import common.Packet;
import common.Protocol;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ChatController {

    @FXML private Label currentUserLabel;
    @FXML private Label chatHeaderLabel;
    @FXML private ListView<String> usersListView;
    @FXML private VBox messagesContainer;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextField messageField;

    private Client client;
    private String currentUsername;
    private String selectedUser;
    private MessageListener messageListener;
    private final Map<String, Label> indicateursMap = new HashMap<>();
    private int messageCounter = 0;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    // ═══════════════════════════════════════════
    //   INITIALISATION
    // ═══════════════════════════════════════════

    public void initialiser(Client client, String username) {
        this.client = client;
        this.currentUsername = username;
        currentUserLabel.setText(username);

        messageListener = new MessageListener(
                client.getIn(),
                this::traiterPacketRecu,
                this::gererDeconnexion
        );
        new Thread(messageListener).start();

        usersListView.setOnMouseClicked(event -> {
            String selected = usersListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedUser = selected
                        .replace("🟢 ", "")
                        .replace("⚫ ", "")
                        .replace(" 🔴", "")
                        .trim();
                chatHeaderLabel.setText("💬 " + selectedUser);
                chatHeaderLabel.setStyle("-fx-text-fill: white;");
                messagesContainer.getChildren().clear();

                // Demander l'historique
                Packet history = new Packet(
                        Protocol.GET_HISTORY,
                        currentUsername,
                        selectedUser,
                        null
                );
                client.envoyer(history);

                // Retirer badge 🔴
                retirerBadge(selectedUser);
            }
        });
    }

    //   TRAITEMENT DES PACKETS REÇUS

    private void traiterPacketRecu(Packet packet) {
        switch (packet.getType()) {
            case Protocol.RECEIVE_MSG   -> afficherMessageRecu(packet);
            case Protocol.STATUS_UPDATE -> mettreAJourStatut(packet);  // ← UNE SEULE
            case Protocol.USER_LIST     -> mettreAJourListeUsers(packet);
            case Protocol.HISTORY       -> afficherHistorique(packet);
            case Protocol.MSG_STATUS    -> mettreAJourStatutMessage(packet);
        }
    }

    //   STATUT UTILISATEUR — UNE SEULE MÉTHODE

    private void mettreAJourStatut(Packet packet) {
        String username = packet.getSender();
        String statut   = packet.getContent();
        String emoji    = "ONLINE".equals(statut) ? "🟢 " : "⚫ ";

        Platform.runLater(() -> {
            // Mettre à jour la liste
            var items = usersListView.getItems();
            items.removeIf(item -> item.replace(" 🔴", "")
                    .replace("🟢 ", "")
                    .replace("⚫ ", "")
                    .trim()
                    .equals(username));
            items.add(emoji + username);

            // Notification dans la zone de chat
            if ("ONLINE".equals(statut)) {
                afficherNotificationStatut("🟢 " + username + " est en ligne !");
            } else {
                afficherNotificationStatut("⚫ " + username + " s'est déconnecté.");
            }
        });
    }

    private void afficherNotificationStatut(String texte) {
        Label notif = new Label(texte);
        notif.setStyle(
                "-fx-text-fill: #888888;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-style: italic;" +
                        "-fx-padding: 2 10;"
        );

        HBox box = new HBox(notif);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(4, 0, 4, 0));

        messagesContainer.getChildren().add(box);
        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    //   HISTORIQUE

    private void afficherHistorique(Packet packet) {
        messagesContainer.getChildren().clear();

        String contenu = packet.getContent();
        if (contenu == null || contenu.isEmpty()) {
            Label vide = new Label("Aucun message. Dites bonjour ! 👋");
            vide.setStyle("-fx-text-fill: #888888; -fx-font-size: 13px;");
            HBox box = new HBox(vide);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(20));
            messagesContainer.getChildren().add(box);
            return;
        }

        String[] messages = contenu.split("\\|\\|");
        for (String msgStr : messages) {
            String[] parts = msgStr.split("\\|", 3);
            if (parts.length >= 2) {
                String expediteur = parts[0];
                String texte      = parts[1];
                String timestamp  = parts.length == 3 ? parts[2] : "";
                boolean estMoi    = expediteur.equals(currentUsername);
                ajouterBulleMessage(texte, expediteur, estMoi, timestamp, true);
            }
        }
    }

    //   MESSAGES EN TEMPS RÉEL

    private void afficherMessageRecu(Packet packet) {
        if (packet.getSender().equals(selectedUser)) {
            ajouterBulleMessage(
                    packet.getContent(),
                    packet.getSender(),
                    false, null, false
            );
        } else {
            // Badge 🔴 sur l'utilisateur dans la liste
            Platform.runLater(() -> {
                var items = usersListView.getItems();
                for (int i = 0; i < items.size(); i++) {
                    String item = items.get(i);
                    if (item.contains(packet.getSender()) && !item.contains("🔴")) {
                        items.set(i, item + " 🔴");
                        break;
                    }
                }
            });
        }
    }

    private void mettreAJourListeUsers(Packet packet) {
        Platform.runLater(() -> {
            usersListView.getItems().clear();
            if (packet.getContent() != null && !packet.getContent().isEmpty()) {
                String[] users = packet.getContent().split(",");
                for (String user : users) {
                    if (!user.trim().equals(currentUsername)) {
                        usersListView.getItems().add("🟢 " + user.trim());
                    }
                }
            }
        });
    }

    //   INDICATEURS

    private void mettreAJourStatutMessage(Packet packet) {
        Platform.runLater(() -> {
            for (var node : messagesContainer.getChildren()) {
                if (node instanceof HBox ligneBox) {
                    if (ligneBox.getAlignment() == Pos.CENTER_RIGHT) {
                        for (var child : ligneBox.getChildren()) {
                            if (child instanceof VBox bulleBox) {
                                for (var bulleChild : bulleBox.getChildren()) {
                                    if (bulleChild instanceof Label label) {
                                        String texte = label.getText();
                                        if (texte != null && texte.endsWith(" ✓")
                                                && !texte.endsWith(" ✓✓")) {
                                            label.setText(texte.replace(" ✓", " ✓✓"));
                                            label.setStyle(
                                                    "-fx-text-fill: #00d4aa;" +
                                                            "-fx-font-size: 10px;");
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    //   ENVOI DE MESSAGE

    @FXML
    private void handleSend() {
        String contenu = messageField.getText().trim();

        if (contenu.isEmpty()) return;

        if (selectedUser == null) {
            afficherAlerteTemporaire("⚠️ Sélectionnez d'abord un utilisateur !");
            return;
        }

        if (contenu.length() > 1000) {
            afficherAlerteTemporaire("⚠️ Message trop long (max 1000 caractères)");
            return;
        }

        Packet packet = new Packet(
                Protocol.SEND_MSG,
                currentUsername,
                selectedUser,
                contenu
        );
        client.envoyer(packet);
        ajouterBulleMessage(contenu, currentUsername, true, null, false);
        messageField.clear();
    }

    //   BULLES DE MESSAGES

    private void ajouterBulleMessage(String contenu, String auteur,
                                     boolean estMoi, String timestamp,
                                     boolean historique) {
        Label bulleLabel = new Label(contenu);
        bulleLabel.setWrapText(true);
        bulleLabel.setMaxWidth(350);
        bulleLabel.setStyle(
                estMoi
                        ? "-fx-background-color: #00d4aa; -fx-text-fill: #1a1a2e;" +
                        "-fx-background-radius: 15 15 0 15; -fx-padding: 10 14;" +
                        "-fx-font-size: 14px;"
                        : "-fx-background-color: #2d2d44; -fx-text-fill: white;" +
                        "-fx-background-radius: 15 15 15 0; -fx-padding: 10 14;" +
                        "-fx-font-size: 14px;"
        );

        // Heure
        String heure;
        if (timestamp != null && !timestamp.isEmpty()) {
            try {
                heure = LocalDateTime.parse(timestamp).format(TIME_FORMAT);
            } catch (Exception e) {
                heure = LocalDateTime.now().format(TIME_FORMAT);
            }
        } else {
            heure = LocalDateTime.now().format(TIME_FORMAT);
        }

        // Indicateur ✓ / ✓✓
        String indicateurTexte;
        String indicateurStyle;

        if (!estMoi) {
            indicateurTexte = heure;
            indicateurStyle = "-fx-text-fill: #888888; -fx-font-size: 10px;";
        } else if (historique) {
            indicateurTexte = heure + " ✓✓";
            indicateurStyle = "-fx-text-fill: #00d4aa; -fx-font-size: 10px;";
        } else {
            indicateurTexte = heure + " ✓";
            indicateurStyle = "-fx-text-fill: #888888; -fx-font-size: 10px;";
        }

        Label heureLabel = new Label(indicateurTexte);
        heureLabel.setStyle(indicateurStyle);

        VBox bulleBox = new VBox(3, bulleLabel, heureLabel);
        bulleBox.setMaxWidth(400);

        HBox ligneBox = new HBox(bulleBox);
        ligneBox.setPadding(new Insets(3, 10, 3, 10));

        if (estMoi) {
            ligneBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            Label nomLabel = new Label(auteur);
            nomLabel.setStyle("-fx-text-fill: #00d4aa; -fx-font-size: 11px;");
            bulleBox.getChildren().add(0, nomLabel);
            ligneBox.setAlignment(Pos.CENTER_LEFT);
        }

        messagesContainer.getChildren().add(ligneBox);
        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    //   UTILITAIRES

    private void retirerBadge(String username) {
        var items = usersListView.getItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).contains(username)) {
                items.set(i, items.get(i).replace(" 🔴", ""));
                break;
            }
        }
    }

    private void afficherAlerteTemporaire(String message) {
        String ancienTexte = chatHeaderLabel.getText();
        chatHeaderLabel.setText(message);
        chatHeaderLabel.setStyle("-fx-text-fill: #ff6b6b;");
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> {
                    chatHeaderLabel.setText(ancienTexte);
                    chatHeaderLabel.setStyle("-fx-text-fill: white;");
                });
            } catch (InterruptedException ignored) {}
        }).start();
    }

    @FXML
    private void handleLogout() {
        Packet logout = new Packet(Protocol.LOGOUT, currentUsername, null, null);
        client.envoyer(logout);
        messageListener.arreter();
        client.deconnecter();
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) messageField.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 500));
            stage.setTitle("💬 Messagerie");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void gererDeconnexion() {
        Platform.runLater(() -> {
            chatHeaderLabel.setText("❌ Connexion perdue !");
            chatHeaderLabel.setStyle("-fx-text-fill: #ff6b6b;");
        });
    }
}