package userRest.connection;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import userRest.event.EventGridProducer;
import userRest.model.Role;

public class RoleDAO {
    private static final EventGridProducer eventProducer = new EventGridProducer();

    // Logging methods
    private static void logInfo(String message) {
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

    // Get all roles
    public static List<Role> getAllRoles() {
        List<Role> roles = new ArrayList<>();
        String query = "SELECT ID, TITLE, DESCRIPTION FROM ROLES";

        logInfo("Iniciando consulta para obtener todos los roles: " + query);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            logInfo("Consulta ejecutada: " + query);
            
            while (rs.next()) {
                roles.add(new Role(
                        rs.getLong("ID"),
                        rs.getString("TITLE"),
                        rs.getString("DESCRIPTION")));
            }
            
            if (roles.isEmpty()) {
                logInfo("No se encontraron roles en la base de datos.");
            } else {
                logInfo("Roles obtenidos: " + roles.size());
            }

        } catch (SQLException e) {
            logError("Error en la consulta", e);
        }
        return roles;
    }

    // Get role by ID
    public static Role getRoleById(long id) {
        String query = "SELECT ID, TITLE, DESCRIPTION FROM ROLES WHERE ID = ?";
        logInfo("Consultando rol con ID: " + id);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Role role = new Role(
                        rs.getLong("ID"),
                        rs.getString("TITLE"),
                        rs.getString("DESCRIPTION"));
                logInfo("Rol encontrado: ID=" + id);
                return role;
            } else {
                logInfo("No se encontró rol con ID=" + id);
            }

        } catch (SQLException e) {
            logError("Error al consultar rol por ID", e);
        }
        return null;
    }

    // Create role
    public static boolean createRole(String title, String description) {
        String query = "INSERT INTO ROLES (TITLE, DESCRIPTION) VALUES (?, ?)";
        logInfo("Creando nuevo rol: title=" + title);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, new String[]{"ID"})) {
            
            stmt.setString(1, title);
            stmt.setString(2, description);
            
            int rowsAffected = stmt.executeUpdate();
            
            Long roleId = null;
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    roleId = rs.getLong(1);
                }
            }
            
            if (rowsAffected > 0 && roleId != null) {
                logInfo("Rol creado con ID: " + roleId + ", Filas afectadas: " + rowsAffected);
                return true;
            } else {
                logInfo("Creación de rol fallida, no se generó ID");
                return false;
            }
            
        } catch (SQLException e) {
            logError("Error al crear rol", e);
            return false;
        }
    }

    // Update role
    public static boolean updateRole(long id, String title, String description) {
        String query = "UPDATE ROLES SET TITLE = ?, DESCRIPTION = ? WHERE ID = ?";
        logInfo("Actualizando rol ID=" + id + ": title=" + title);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setLong(3, id);
            
            int rowsAffected = stmt.executeUpdate();
            logInfo("Rol actualizado. Filas afectadas: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logError("Error al actualizar rol", e);
            return false;
        }
    }

    // Delete role and trigger event for consumer to update associated users
    public static boolean deleteRole(long id) {
        logInfo("Iniciando proceso para eliminación del rol con ID=" + id);

        // Check if the role exists (optional, but good for context)
        Role role = getRoleById(id);
        if (role == null) {
            logInfo("No se encontró rol con ID=" + id + ". No se enviará evento de eliminación.");
            return false; // Or true, depending on whether not finding a role is an error for event sending
        }

        // Log how many users might be affected (for informational purposes)
        int affectedUsers = countUsersByRoleId(id);
        logInfo("La eliminación del rol con ID=" + id + " (título: " + role.getTitle() + ") afectará a " + affectedUsers + " usuarios. Enviando evento para que el consumidor procese la eliminación.");

        try {
            // 1. Send event to consumer. Consumer is responsible for all DB changes.
            eventProducer.sendRoleDeletedEvent(id);
            logInfo("Evento de eliminación de rol enviado para ID=" + id + ". El consumidor se encargará de las operaciones de base de datos.");
            return true; // Event sent successfully

        } catch (Exception e) {
            logError("Error al enviar evento de eliminación de rol para ID=" + id, e);
            return false;
        }
    }
    
    // Count users by role ID
    public static int countUsersByRoleId(long roleId) {
        String query = "SELECT COUNT(*) FROM USUARIOS WHERE ROL = ?";
        logInfo("Contando usuarios con rol ID: " + roleId);
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setLong(1, roleId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt(1);
                logInfo("Encontrados " + count + " usuarios con rol ID " + roleId);
                return count;
            }
            
        } catch (SQLException e) {
            logError("Error al contar usuarios por rol ID", e);
        }
        
        return 0;
    }
}
