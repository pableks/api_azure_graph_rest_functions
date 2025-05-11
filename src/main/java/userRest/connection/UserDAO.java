package userRest.connection;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import com.microsoft.azure.functions.ExecutionContext;

import userRest.event.EventGridProducer;
import userRest.model.User;

public class UserDAO {
    private static final EventGridProducer eventProducer = new EventGridProducer();

    // Método para registrar logs usando el contexto de Azure
    private static void logInfo(String message) {
        // Usamos el mismo contexto que se configuró en DatabaseConnection
        if (DatabaseConnection.currentContext != null) {
            DatabaseConnection.currentContext.getLogger().info(message);
        } else {
            System.out.println(message);
        }
    }

    private static void logError(String message, Throwable e) {
        if (DatabaseConnection.currentContext != null) {
            DatabaseConnection.currentContext.getLogger().severe(message + ": " + e.getMessage());
            if (e.getCause() != null) {
                DatabaseConnection.currentContext.getLogger().severe("Caused by: " + e.getCause().getMessage());
            }
        } else {
            System.err.println(message + ": " + e.getMessage());
        }
    }

    // Obtener todos los usuarios
    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT ID, EMAIL, PASSWORD, ROL FROM USUARIOS";

        logInfo("Iniciando consulta para obtener todos los usuarios: " + query);

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
            
            logInfo("Consulta ejecutada: " + query);
            
            while (rs.next()) {
                users.add(new User(
                        rs.getLong("ID"),
                        rs.getString("EMAIL"),
                        rs.getString("PASSWORD"),
                        rs.getLong("ROL")));
            }
            
            if (users.isEmpty()) {
                logInfo("No se encontraron usuarios en la base de datos.");
            } else {
                logInfo("Usuarios obtenidos: " + users.size());
            }

        } catch (SQLException e) {
            logError("Error en la consulta", e);
        }
        return users;
    }

    // Obtener usuario por ID
    public static User getUserById(long id) {
        String query = "SELECT ID, EMAIL, PASSWORD, ROL FROM USUARIOS WHERE ID = ?";
        logInfo("Consultando usuario con ID: " + id);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                        rs.getLong("ID"),
                        rs.getString("EMAIL"),
                        rs.getString("PASSWORD"),
                        rs.getLong("ROL"));
                logInfo("Usuario encontrado: ID=" + id);
                return user;
            } else {
                logInfo("No se encontró usuario con ID=" + id);
            }

        } catch (SQLException e) {
            logError("Error al consultar usuario por ID", e);
        }
        return null;
    }

    // Crear usuario - Implementación directa en base de datos
    public static boolean createUser(String email, String password, Long roleId) {
        String query = "INSERT INTO USUARIOS (EMAIL, PASSWORD, ROL) VALUES (?, ?, ?)";
        logInfo("Creando nuevo usuario: email=" + email + ", roleId=" + roleId);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email);
            stmt.setString(2, password);
            stmt.setLong(3, roleId);
            
            int rowsAffected = stmt.executeUpdate();
            logInfo("Usuario creado. Filas afectadas: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logError("Error al crear usuario", e);
            return false;
        }
    }

    /**
     * Create a user and let the consumer assign the default role if needed
     */
    public static boolean createUserWithDefaultRole(String email, String password, Long roleId) {
        String query = "INSERT INTO USUARIOS (EMAIL, PASSWORD, ROL) VALUES (?, ?, ?)";
        logInfo("Creating new user: email=" + email + ", roleId=" + roleId);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, new String[]{"ID"})) {
            
            stmt.setString(1, email);
            stmt.setString(2, password);
            
            // If roleId is null or invalid, set it to NULL in the database
            if (roleId == null || roleId <= 0) {
                stmt.setNull(3, Types.BIGINT);
                logInfo("No role specified, setting to NULL (consumer will assign default)");
            } else {
                stmt.setLong(3, roleId);
            }
            
            int rowsAffected = stmt.executeUpdate();
            
            // Get the newly created user ID for event publishing
            Long userId = null;
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    userId = rs.getLong(1);
                }
            }
            
            if (rowsAffected > 0 && userId != null) {
                logInfo("User created with ID: " + userId + ", Rows affected: " + rowsAffected);
                
                // Create User object for the event (role might be null)
                User newUser = new User(userId, email, password, roleId);
                
                // Send the user created event
                EventGridProducer eventProducer = new EventGridProducer();
                eventProducer.sendEvent(EventGridProducer.EVENT_USER_CREATED, newUser);
                
                return true;
            } else {
                logInfo("User creation failed, no ID generated");
                return false;
            }
            
        } catch (SQLException e) {
            logError("Error al crear usuario", e);
            return false;
        }
    }

    // Actualizar usuario - Implementación directa en base de datos
    public static boolean updateUser(long id, String email, String password, Long roleId) {
        String query = "UPDATE USUARIOS SET EMAIL = ?, PASSWORD = ?, ROL = ? WHERE ID = ?";
        logInfo("Actualizando usuario ID=" + id + ": email=" + email + ", roleId=" + roleId);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email);
            stmt.setString(2, password);
            stmt.setLong(3, roleId);
            stmt.setLong(4, id);
            
            int rowsAffected = stmt.executeUpdate();
            logInfo("Usuario actualizado. Filas afectadas: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logError("Error al actualizar usuario", e);
            return false;
        }
    }

    // Eliminar usuario - Implementación directa en base de datos
    public static boolean deleteUser(long id) {
        String query = "DELETE FROM USUARIOS WHERE ID = ?";
        logInfo("Eliminando usuario con ID=" + id);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setLong(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            logInfo("Usuario eliminado. Filas afectadas: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logError("Error al eliminar usuario", e);
            return false;
        }
    }
}

