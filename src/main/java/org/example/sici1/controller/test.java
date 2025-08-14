package org.example.sici1.controller;

import java.sql.Connection;
import java.sql.DriverManager;

public class test {
    public static void main(String[] args) {
        // Ruta del wallet DESCOMPRIMIDO (ajusta tu ruta real)
        String ubicacionWallet = "C:/Users/VICTOR UZZIEL/IdeaProjects/SICI1/src/main/Wallet_CN4PI23N1E6J6TZS";

        // Configura propiedad JVM para que Oracle JDBC encuentre el tnsnames.ora
        System.setProperty("oracle.net.tns_admin", ubicacionWallet);

        // Usa el nombre de tu alias en tnsnames.ora (checa que coincida exactamente)
        String jdbcurl = "jdbc:oracle:thin:@cn4pi23n1e6j6tzs_high";
        String userName = "ADMIN";
        String password = "Knoxotics_Kashima50";

        try {
            // Carga el driver de Oracle (opcional si usas Java 17+)
            Class.forName("oracle.jdbc.OracleDriver");

            // Solo conexión, sin ejecutar consultas
            Connection con = DriverManager.getConnection(jdbcurl, userName, password);
            System.out.println("¡Conexión establecida correctamente!");

            // Cierra la conexión
            con.close();
        } catch (Exception e) {
            System.out.println("Error: conexión no establecida.");
            e.printStackTrace();
        }
    }
}
