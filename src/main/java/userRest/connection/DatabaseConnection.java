package userRest.connection;

import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("Pase por Base de datos 1");
            boolean isValid = conn.isValid(5);
            logger.info("Conexión a la base de datos probada: " + (isValid ? "Válida" : "Inválida"));
            return isValid;
        } catch (SQLException e) {
            logger.severe("Error al probar la conexión: " + e.getMessage());
            return false;
        }
    }

    // Get connection to Oracle database
    public static Connection getConnection() throws SQLException {
        try {
            // Load the Oracle JDBC driver
            Class.forName("oracle.jdbc.driver.OracleDriver");
            
            // Get credentials from environment variables (set in local.settings.json)
            String user = System.getenv("ORACLE_USER");
            String password = System.getenv("ORACLE_PASSWORD");
            String tnsName = System.getenv("ORACLE_TNS_NAME");
            String walletPath = System.getenv("ORACLE_WALLET_PATH");
            
            // Debug output
            System.out.println("Oracle connection details:");
            System.out.println("User: " + user);
            System.out.println("TNS Name: " + tnsName);
            System.out.println("Wallet Path: " + walletPath);
            
            // Use defaults if environment variables are not set (for local testing)
            if (user == null) user = "ADMIN";
            if (password == null) password = "PassCloud123";
            if (tnsName == null) tnsName = "g82idu9csvrtaymm_high";
            if (walletPath == null) walletPath = "/Users/pablojavier/Desktop/Wallet_CLOUDS8";
            
            // Build connection string
            String url = "jdbc:oracle:thin:@" + tnsName + "?TNS_ADMIN=" + walletPath;
            System.out.println("Connection URL: " + url);
            
            // Set connection properties
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", password);
            props.setProperty("oracle.net.wallet_location", "(SOURCE=(METHOD=file)(METHOD_DATA=(DIRECTORY=" + walletPath + ")))");
            
            // Connect to the database
            Connection conn = DriverManager.getConnection(url, props);
            System.out.println("Database connection established successfully");
            return conn;
            
        } catch (ClassNotFoundException e) {
            System.err.println("Oracle JDBC driver not found: " + e.getMessage());
            throw new SQLException("Oracle JDBC driver not found", e);
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            throw e;
        }
    }
}
