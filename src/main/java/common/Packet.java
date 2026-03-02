package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Packet implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;         // LOGIN, SEND_MSG, etc.
    private String sender;       // username de l'expéditeur
    private String receiver;     // username du destinataire (null si broadcast)
    private String content;      // contenu du message ou données
    private String status;       // SUCCESS, ERROR, ou statut message
    private LocalDateTime timestamp;

    public Packet() {
        this.timestamp = LocalDateTime.now();
    }

    public Packet(String type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // Getters et Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "Packet{type='" + type + "', sender='" + sender +
                "', receiver='" + receiver + "', content='" + content + "'}";
    }
}
