module com.example.appmessagerie {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.appmessagerie to javafx.fxml;
    exports com.example.appmessagerie;
}