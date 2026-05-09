module com.rplbo.app.rplboblessedbot {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.rplbo.app.rplboblessedbot to javafx.fxml;
    exports com.rplbo.app.rplboblessedbot;
}