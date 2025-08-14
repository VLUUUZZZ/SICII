package org.example.sici1.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    private static final String UBICACION_WALLET = "C:/Users/VICTOR UZZIEL/IdeaProjects/SICI1/src/main/Wallet_CN4PI23N1E6J6TZS";
    private static final String JDBC_URL = "jdbc:oracle:thin:@cn4pi23n1e6j6tzs_high"; // Usa el alias de tu tnsnames.ora
    private static final String USER = "ADMIN";
    private static final String PASS = "Knoxotics_Kashima50";

    static {
        System.setProperty("oracle.net.tns_admin", UBICACION_WALLET);
    }

    public static Connection conectar() throws SQLException, ClassNotFoundException {
        Class.forName("oracle.jdbc.OracleDriver");
        return DriverManager.getConnection(JDBC_URL, USER, PASS);
    }
}