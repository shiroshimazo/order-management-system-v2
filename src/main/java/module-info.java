module com.order.ordermanagementsystem {
    requires javafx.controls;
    requires javafx.fxml;

    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;

    opens com.order.ordermanagementsystem to javafx.fxml;
    exports com.order.ordermanagementsystem;
}