module com.craftboard.craftboardserver {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.craftboard.craftboardserver to javafx.fxml;
    exports com.craftboard.craftboardserver;
}