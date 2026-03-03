module com.example.appmessagerie {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.hibernate.orm.core;
    requires java.persistence;
    requires jbcrypt;


    opens com.example.appmessagerie to javafx.fxml;
    exports com.example.appmessagerie;
}