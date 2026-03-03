package persistence;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import entity.User;
import entity.Message;

public class HibernateUtil {

    private static SessionFactory sessionFactory;

    // Empêche l'instanciation
    private HibernateUtil() {}

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration config = new Configuration();
                config.configure("hibernate.cfg.xml");

                // Enregistrer les entités
                config.addAnnotatedClass(User.class);
                config.addAnnotatedClass(Message.class);

                ServiceRegistry registry = new StandardServiceRegistryBuilder()
                        .applySettings(config.getProperties())
                        .build();

                sessionFactory = config.buildSessionFactory(registry);

            } catch (Exception e) {
                System.err.println("❌ Erreur initialisation Hibernate : " + e.getMessage());
                e.printStackTrace();
            }
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            System.out.println("✅ SessionFactory fermée.");
        }
    }
}
