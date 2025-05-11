package userRest.graphql;

import java.util.List;

import graphql.kickstart.tools.GraphQLQueryResolver;
import userRest.connection.UserDAO;
import userRest.model.User;

public class UserQueryResolver implements GraphQLQueryResolver {

    // Query: Obtener todos los usuarios
    public List<User> getAllUsers() {
        return UserDAO.getAllUsers();
    }

    // Query: Obtener usuario por ID
    public User getUserById(Long id) {
        return UserDAO.getUserById(id);
    }
}
