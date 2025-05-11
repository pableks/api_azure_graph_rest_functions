package userRest.graphql;

import graphql.kickstart.tools.GraphQLMutationResolver;
import userRest.connection.UserDAO;

public class UserMutationResolver implements GraphQLMutationResolver {

    // Mutación: Crear usuario
    public boolean createUser(String email, String password, Long roleId) {
        return UserDAO.createUser(email, password, roleId);
    }

    // Mutación: Actualizar usuario
    public boolean updateUser(Long id, String email, String password, Long roleId) {
        return UserDAO.updateUser(id, email, password, roleId);
    }

    // Mutación: Eliminar usuario
    public boolean deleteUser(Long id) {
        return UserDAO.deleteUser(id);
    }
}