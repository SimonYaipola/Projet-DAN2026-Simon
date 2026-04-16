module com.craftboard.craftboarddesktop {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.craftboard.craftboarddesktop to javafx.fxml;
    exports com.craftboard.craftboarddesktop;
}