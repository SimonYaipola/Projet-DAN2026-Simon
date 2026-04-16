module com.craftboard.craftboard {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.craftboard.craftboard to javafx.fxml;
    exports com.craftboard.craftboard;
}