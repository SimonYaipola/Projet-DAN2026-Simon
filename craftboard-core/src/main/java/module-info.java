module com.craftboard.craftboardcore {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.craftboard.craftboardcore to javafx.fxml;
    exports com.craftboard.craftboardcore;
}