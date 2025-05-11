package userRest.graphql;

import java.util.List;

import graphql.kickstart.tools.GraphQLQueryResolver;
import userRest.connection.RoleDAO;
import userRest.model.Role;

public class RoleQueryResolver implements GraphQLQueryResolver {

    // Query: Get all roles
    public List<Role> getAllRoles() {
        return RoleDAO.getAllRoles();
    }

    // Query: Get role by ID
    public Role getRoleById(Long id) {
        return RoleDAO.getRoleById(id);
    }
}