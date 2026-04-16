module com.shiro.ordermanagementsystem {
    requires javafx.controls;
    requires javafx.fxml;

    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.shiro.ordermanagementsystem to javafx.graphics;
    opens com.shiro.ordermanagementsystem.Controller to javafx.fxml;

    exports com.shiro.ordermanagementsystem;
}