module hr.algebra.kviz {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.rmi;
    requires java.naming;
    requires jdk.naming.rmi;
    requires java.xml;

    exports hr.algebra.kviz;
    exports hr.algebra.kviz.model;
    exports hr.algebra.kviz.rmi;
    exports hr.algebra.kviz.network.message;

    opens hr.algebra.kviz.controller to javafx.fxml;
}
