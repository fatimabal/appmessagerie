package server.repository;

import entity.Message;
import entity.MessageStatus;
import org.hibernate.Session;
import org.hibernate.Transaction;
import persistence.HibernateUtil;

import java.util.List;

public class MessageRepository {

    /**
     * Sauvegarde un message en BDD
     */
    public Message sauvegarder(Message message) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(message);
            transaction.commit();
            return message;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            System.err.println("❌ Erreur sauvegarde message : " + e.getMessage());
            return null;
        }
    }

    /**
     * Récupère la conversation entre deux utilisateurs
     * Ordonnée chronologiquement — RG8
     */
    public List<Message> trouverConversation(String username1, String username2) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Message m WHERE " +
                                    "(m.sender.username = :u1 AND m.receiver.username = :u2) OR " +
                                    "(m.sender.username = :u2 AND m.receiver.username = :u1) " +
                                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("u1", username1)
                    .setParameter("u2", username2)
                    .list();
        } catch (Exception e) {
            System.err.println("❌ Erreur récupération conversation : " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Récupère les messages en attente pour un utilisateur — RG6
     * (messages envoyés quand il était hors ligne)
     */
    public List<Message> trouverMessagesEnAttente(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM Message m WHERE " +
                                    "m.receiver.username = :username AND " +
                                    "m.statut = :statut " +
                                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("username", username)
                    .setParameter("statut", MessageStatus.ENVOYE)
                    .list();
        } catch (Exception e) {
            System.err.println("❌ Erreur messages en attente : " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Met à jour le statut d'un message (ENVOYE → RECU → LU)
     */
    public void mettreAJourStatut(Long messageId, MessageStatus statut) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.createQuery(
                            "UPDATE Message SET statut = :statut WHERE id = :id")
                    .setParameter("statut", statut)
                    .setParameter("id", messageId)
                    .executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            System.err.println("❌ Erreur mise à jour statut message : " + e.getMessage());
        }
    }
}