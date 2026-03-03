package server.repository;

import entity.User;
import entity.UserStatus;
import org.hibernate.Session;
import org.hibernate.Transaction;
import persistence.HibernateUtil;

import javax.persistence.NoResultException;

public class UserRepository {

    /**
     * Sauvegarde un nouvel utilisateur en BDD
     */
    public User sauvegarder(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(user);
            session.flush();          // ← FORCE l'écriture en BDD
            transaction.commit();
            System.out.println("✅ Utilisateur sauvegardé : " + user.getUsername());
            return user;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            System.err.println("❌ Erreur sauvegarde utilisateur : " + e.getMessage());
            return null;
        }
    }

    /**
     * Cherche un utilisateur par son username
     * @return User si trouvé, null sinon
     */
    public User trouverParUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "FROM User WHERE username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResult();
        } catch (NoResultException e) {
            return null;
        } catch (Exception e) {
            System.err.println("❌ Erreur recherche utilisateur : " + e.getMessage());
            return null;
        }
    }

    /**
     * Met à jour le statut ONLINE/OFFLINE d'un utilisateur — RG4
     */
    public void mettreAJourStatut(String username, UserStatus statut) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.createQuery(
                            "UPDATE User SET status = :statut WHERE username = :username")
                    .setParameter("statut", statut)
                    .setParameter("username", username)
                    .executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            System.err.println("❌ Erreur mise à jour statut : " + e.getMessage());
        }
    }

    /**
     * Vérifie si un username existe déjà en BDD — RG1
     */
    public boolean usernameExiste(String username) {
        return trouverParUsername(username) != null;
    }
}