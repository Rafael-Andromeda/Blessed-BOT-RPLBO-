module com.rplbo.app.rplboblessedbot {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.rplbo.app.rplboblessedbot to javafx.fxml;
    exports com.rplbo.app.rplboblessedbot;
}