package userRest.function;

import java.util.*;
import java.util.logging.Logger;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import userRest.connection.UserDAO;
import userRest.model.User;

public class UserRestFunction {
    private static final Logger logger = Logger.getLogger(UserRestFunction.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @FunctionName("userRest")
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
        // Verificar si se solicita un usuario específico por ID
        String idParam = request.getQueryParameters().get("id");
        
        if (idParam != null && !idParam.isEmpty()) {
            try {
                long userId = Long.parseLong(idParam);
                User user = UserDAO.getUserById(userId);
                
                if (user != null) {
                    return request.createResponseBuilder(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body(user)
                            .build();
                } else {
                    return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                            .body("Usuario con ID " + userId + " no encontrado")
                            .build();
                }
            } catch (NumberFormatException e) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El ID debe ser un número válido")
                        .build();
            }
        } else {
            // Obtener todos los usuarios
            List<User> users = UserDAO.getAllUsers();
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(users)
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
            
            User newUser = objectMapper.readValue(requestBody, User.class);
            
            if (newUser.getEmail() == null || newUser.getPassword() == null) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("Email y password son obligatorios")
                        .build();
            }
            
            // Usar el método que implementa la asignación de rol por defecto mediante eventos
            boolean success = UserDAO.createUserWithDefaultRole(newUser.getEmail(), newUser.getPassword(), newUser.getRoleId());
            
            if (success) {
                return request.createResponseBuilder(HttpStatus.CREATED)
                        .header("Content-Type", "application/json")
                        .body("{'message': 'Usuario creado correctamente'}")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al crear el usuario")
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
            
            User updateUser = objectMapper.readValue(requestBody, User.class);
            
            if (updateUser.getId() == null) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("El ID del usuario es obligatorio para actualizar")
                        .build();
            }
            
            boolean success = UserDAO.updateUser(
                updateUser.getId(), 
                updateUser.getEmail(), 
                updateUser.getPassword(), 
                updateUser.getRoleId()
            );
            
            if (success) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("{'message': 'Usuario actualizado correctamente'}")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al actualizar el usuario")
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
            // Verificar si el ID viene como parámetro de consulta
            String idParam = request.getQueryParameters().get("id");
            
            long userId;
            
            if (idParam != null && !idParam.isEmpty()) {
                // ID como parámetro de consulta
                userId = Long.parseLong(idParam);
            } else {
                // ID en el cuerpo de la solicitud
                String requestBody = request.getBody().orElse("");
                if (requestBody.isEmpty()) {
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                            .body("Se requiere el ID del usuario para eliminar")
                            .build();
                }
                
                Map<String, Object> bodyMap = objectMapper.readValue(requestBody, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                if (!bodyMap.containsKey("id")) {
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                            .body("El campo 'id' es obligatorio")
                            .build();
                }
                
                userId = Long.parseLong(bodyMap.get("id").toString());
            }
            
            boolean success = UserDAO.deleteUser(userId);
            
            if (success) {
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("{'message': 'Usuario eliminado correctamente'}")
                        .build();
            } else {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error al eliminar el usuario")
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