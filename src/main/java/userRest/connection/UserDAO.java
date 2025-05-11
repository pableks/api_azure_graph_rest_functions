package userRest.connection;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;

import userRest.event.EventGridProducer;
import userRest.model.User;

public class UserDAO {

    private static final EventGridProducer eventProducer = new EventGridProducer();

    // Obtener todos los usuarios
    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String query = "SELECT ID, EMAIL, PASSWORD, ROL FROM USUARIOS";

        System.out.println("Iniciando consulta para obtener todos los usuarios: " + query);

        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
            
            System.out.println("Consulta ejecutada: " + query);
            
            while (rs.next()) {
                users.add(new User(
                        rs.getLong("ID"),
                        rs.getString("EMAIL"),
                        rs.getString("PASSWORD"),
                        rs.getLong("ROL")));
            }
            
            if (users.isEmpty()) {
                System.out.println("No se encontraron usuarios en la base de datos.");
            } else {
                System.out.println("Usuarios obtenidos: " + users.size());
            }

        } catch (SQLException e) {
            System.out.println("Error en la consulta: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    // Obtener usuario por ID
    public static User getUserById(long id) {
        String query = "SELECT ID, EMAIL, PASSWORD, ROL FROM USUARIOS WHERE ID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getLong("ID"),
                        rs.getString("EMAIL"),
                        rs.getString("PASSWORD"),
                        rs.getLong("ROL"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Crear usuario - Implementación directa en base de datos
    public static boolean createUser(String email, String password, Long roleId) {
        String query = "INSERT INTO USUARIOS (EMAIL, PASSWORD, ROL) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email);
            stmt.setString(2, password);
            stmt.setLong(3, roleId);
            
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Usuario creado. Filas afectadas: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al crear usuario: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Actualizar usuario - Implementación directa en base de datos
    public static boolean updateUser(long id, String email, String password, Long roleId) {
        String query = "UPDATE USUARIOS SET EMAIL = ?, PASSWORD = ?, ROL = ? WHERE ID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email);
            stmt.setString(2, password);
            stmt.setLong(3, roleId);
            stmt.setLong(4, id);
            
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Usuario actualizado. Filas afectadas: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Eliminar usuario - Implementación directa en base de datos
    public static boolean deleteUser(long id) {
        String query = "DELETE FROM USUARIOS WHERE ID = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setLong(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Usuario eliminado. Filas afectadas: " + rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
