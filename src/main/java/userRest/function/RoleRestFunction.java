package userRest.function;

import java.util.*;
import java.util.logging.Logger;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import userRest.connection.RoleDAO;
import userRest.model.Role;

public class RoleRestFunction {
    private static final Logger logger = Logger.getLogger(RoleRestFunction.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @FunctionName("roleRest")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", 
                         methods = { HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE }, 
                         authLevel = AuthorizationLevel.ANONYMOUS) 
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("REST API request received: " + request.getHttpMethod());

        try {
            switch (request.getHttpMethod()) {
                case GET:
                    return handleGet(request, context);
                case POST:
                    return handlePost(request, context);
                case PUT:
                    return handlePut(request, context);
                case DELETE:
                    return handleDelete(request, context);
                default:
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                            .body("Error: Método HTTP no soportado.")
                            .build();
            }
        } catch (Exception e) {
            context.getLogger().severe("Error procesando la solicitud: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno del servidor: " + e.getMessage())
                    .build();
        }
    }

    private HttpResponseMessage handleGet(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        // Check if a specific role is requested by ID
        String idParam = request.getQueryParameters().get("id");
        
        if (idParam != null && !idParam.isEmpty()) {
            try {
                long roleId = Long.parseLong(idParam);
                Role role = RoleDAO.getRoleById(roleId);
                
                if (role != null) {
                    return request.createResponseBuilder(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body(role)
                            .build();
                } else {
                    return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                            .body("Rol con ID " + roleId + " no encontrado")
                            .build();
                }
            } catch (NumberFormatException e) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El ID debe ser un número válido")
                        .build();
            }
        } else {
            // Get all roles
            List<Role> roles = RoleDAO.getAllRoles();
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(roles)
                    .build();
        }
    }

    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        try {
            String requestBody = request.getBody().orElse("");
            if (requestBody.isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El cuerpo de la solicitud está vacío")
                        .build();
            }
            
            Role newRole = objectMapper.readValue(requestBody, Role.class);
            
            if (newRole.getTitle() == null) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El título es obligatorio")
                        .build();
            }
            
            boolean success = RoleDAO.createRole(newRole.getTitle(), newRole.getDescription());
            
            if (success) {
                return request.createResponseBuilder(HttpStatus.CREATED)
                        .header("Content-Type", "application/json")
                        .body("{'message': 'Rol creado correctamente'}")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al crear el rol")
                        .build();
            }
        } catch (Exception e) {
            context.getLogger().severe("Error al procesar la solicitud POST: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Error al procesar la solicitud: " + e.getMessage())
                    .build();
        }
    }

    private HttpResponseMessage handlePut(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        try {
            String requestBody = request.getBody().orElse("");
            if (requestBody.isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El cuerpo de la solicitud está vacío")
                        .build();
            }
            
            Role updateRole = objectMapper.readValue(requestBody, Role.class);
            
            if (updateRole.getId() == null) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El ID del rol es obligatorio para actualizar")
                        .build();
            }
            
            boolean success = RoleDAO.updateRole(
                updateRole.getId(), 
                updateRole.getTitle(), 
                updateRole.getDescription()
            );
            
            if (success) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("{'message': 'Rol actualizado correctamente'}")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al actualizar el rol")
                        .build();
            }
        } catch (Exception e) {
            context.getLogger().severe("Error al procesar la solicitud PUT: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Error al procesar la solicitud: " + e.getMessage())
                    .build();
        }
    }

    private HttpResponseMessage handleDelete(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        try {
            // Check if ID is provided as a query parameter
            String idParam = request.getQueryParameters().get("id");
            
            long roleId;
            
            if (idParam != null && !idParam.isEmpty()) {
                // ID as query parameter
                roleId = Long.parseLong(idParam);
            } else {
                // ID in request body
                String requestBody = request.getBody().orElse("");
                if (requestBody.isEmpty()) {
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                            .body("Se requiere el ID del rol para eliminar")
                            .build();
                }
                
                Map<String, Object> bodyMap = objectMapper.readValue(requestBody, 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                if (!bodyMap.containsKey("id")) {
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                            .body("El campo 'id' es obligatorio")
                            .build();
                }
                
                roleId = Long.parseLong(bodyMap.get("id").toString());
            }
            
            boolean success = RoleDAO.deleteRole(roleId);
            
            if (success) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("{'message': 'Rol eliminado correctamente'}")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al eliminar el rol")
                        .build();
            }
        } catch (NumberFormatException e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("El ID debe ser un número válido")
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Error al procesar la solicitud DELETE: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Error al procesar la solicitud: " + e.getMessage())
                    .build();
        }
    }
}