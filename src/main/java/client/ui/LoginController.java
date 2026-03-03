package client.ui;

import client.Client;
import common.Packet;
import common.Protocol;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         messageLabel;
    @FXML private Button        loginButton;
    @FXML private Button        registerButton;

    private Client client;

    @FXML
    public void initialize() {
        client = new Client();
        if (!client.connecter()) {
            afficherErreur("❌ Impossible de se connecter au serveur.");
            loginButton.setDisable(true);
            registerButton.setDisable(true);
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Validation des champs
        if (username.isEmpty() || password.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }

        // Désactiver le bouton pendant la requête
        loginButton.setDisable(true);
        messageLabel.setText("Connexion en cours...");
        messageLabel.setStyle("-fx-text-fill: #00d4aa;");

        // Envoyer dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            Packet login = new Packet(Protocol.LOGIN, username, null, password);
            client.envoyer(login);
            Packet reponse = client.recevoir();

            Platform.runLater(() -> {
                loginButton.setDisable(false);
                if (reponse != null && Protocol.SUCCESS.equals(reponse.getType())) {
                    ouvrirChatScreen(username);
                } else {
                    String msg = reponse != null ? reponse.getContent() : "Erreur inconnue";
                    afficherErreur(msg);
                }
            });
        }).start();
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }

        registerButton.setDisable(true);
        messageLabel.setText("Inscription en cours...");
        messageLabel.setStyle("-fx-text-fill: #00d4aa;");

        new Thread(() -> {
            Packet register = new Packet(Protocol.REGISTER, username, null, password);
            client.envoyer(register);
            Packet reponse = client.recevoir();

            Platform.runLater(() -> {
                registerButton.setDisable(false);
                if (reponse != null && Protocol.SUCCESS.equals(reponse.getType())) {
                    messageLabel.setStyle("-fx-text-fill: #00d4aa;");
                    messageLabel.setText("✅ " + reponse.getContent());
                } else {
                    String msg = reponse != null ? reponse.getContent() : "Erreur inconnue";
                    afficherErreur(msg);
                }
            });
        }).start();
    }

    private void ouvrirChatScreen(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/chat.fxml"));
            Parent root = loader.load();

            // Passer le client et le username au ChatController
            ChatController chatController = loader.getController();
            chatController.initialiser(client, username);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setTitle("💬 Messagerie — " + username);
            stage.setScene(new Scene(root, 800, 600));
            stage.setResizable(true);

        } catch (Exception e) {
            afficherErreur("Erreur ouverture chat : " + e.getMessage());
        }
    }

    private void afficherErreur(String message) {
        messageLabel.setStyle("-fx-text-fill: #ff6b6b;");
        messageLabel.setText(message);
    }
}