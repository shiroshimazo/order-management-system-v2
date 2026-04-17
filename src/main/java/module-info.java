module com.shiro.ordermanagementsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires jbcrypt;

    opens com.shiro.ordermanagementsystem to javafx.graphics;
    opens com.shiro.ordermanagementsystem.Controller to javafx.fxml;

    exports com.shiro.ordermanagementsystem;
}
