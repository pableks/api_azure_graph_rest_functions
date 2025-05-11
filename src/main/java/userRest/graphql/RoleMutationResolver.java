package userRest.graphql;

import graphql.kickstart.tools.GraphQLMutationResolver;
import userRest.connection.RoleDAO;

public class RoleMutationResolver implements GraphQLMutationResolver {

    // Mutation: Create role
    public boolean createRole(String title, String description) {
        return RoleDAO.createRole(title, description);
    }

    // Mutation: Update role
    public boolean updateRole(Long id, String title, String description) {
        return RoleDAO.updateRole(id, title, description);
    }

    // Mutation: Delete role
    public boolean deleteRole(Long id) {
        return RoleDAO.deleteRole(id);
    }
}