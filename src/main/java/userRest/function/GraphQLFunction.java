package userRest.function;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.kickstart.tools.SchemaParser;
import graphql.schema.GraphQLSchema;
//import userRest.event.EventGridProducer;
import userRest.graphql.UserMutationResolver;
import userRest.graphql.UserQueryResolver;
import userRest.model.User;
import com.fasterxml.jackson.core.type.TypeReference;

public class GraphQLFunction {

    private static final GraphQL graphQL;
    
    static {
        // Cargar schema.graphqls desde resources
        System.out.println("Pase por Aca 1");

        // Crear una instancia de EventGridProducer
        //EventGridProducer eventPublisher = new EventGridProducer();


        try {
            // Cargar schema.graphqls desde resources
            InputStream schemaStream = GraphQLFunction.class
                .getClassLoader()
                .getResourceAsStream("graphql/schema.graphqls");
                System.out.println("Pase por Aca 2 "+schemaStream);
            if (schemaStream == null) {
                throw new RuntimeException("No se encontró el archivo schema.graphqls en /resources/graphql");
            }

            String sdl = new String(schemaStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("Pase por Aca 3 "+sdl);
            //UserQueryResolver userResolver = new UserQueryResolver();

            GraphQLSchema schema = SchemaParser.newParser()
                .schemaString(sdl)
                //.resolvers(new UserResolver())
                .resolvers(
                    new UserQueryResolver(),    // Resolver para consultas
                    new UserMutationResolver()  // Resolver para mutaciones
                    //new UserMutationResolver(eventPublisher)  // Resolver para mutaciones
                    )
                .dictionary(User.class)
                .build()
                .makeExecutableSchema();
            System.out.println("Pase por Aca 4"+schema);
            graphQL = GraphQL.newGraphQL(schema).build();
            System.out.println("Pase por Aca 5 "+graphQL);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al construir el esquema GraphQL: " + e.getMessage(), e);
        }
    }

    @FunctionName("graphql")
    public HttpResponseMessage handleRequest(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<String>> request,
        final ExecutionContext context
        
        ) {
            System.out.println("Pase por Aca 6 "+request);
            context.getLogger().info("Request recibido en GRAPHQL"); // Agregar log

            try {
                /*ObjectMapper objectMapper = new ObjectMapper();
                JsonNode json = objectMapper.readTree(request.getBody().orElse("{}"));
                System.out.println("Pase por Aca 7 ");
                context.getLogger().info("Request recibido en GRAPHQL" + json + objectMapper); // Agregar log
                

                String query = json.get("query").asText();
                JsonNode variablesNode = json.get("variables");

                Map<String, Object> variables = variablesNode != null && !variablesNode.isNull()
                ? objectMapper.convertValue(variablesNode, new TypeReference<Map<String, Object>>() {})
                : Map.of();*/
                ObjectMapper objectMapper = new ObjectMapper();
                String requestBody = request.getBody().orElse("{}");
                context.getLogger().info("Raw Body: " + requestBody);
                
                JsonNode json = objectMapper.readTree(requestBody);
                
                JsonNode queryNode = json.get("query");
                if (queryNode == null || queryNode.isNull()) {
                    context.getLogger().severe("No se encontró 'query' en el body.");
                    return request
                        .createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("{\"error\":\"Missing 'query' field\"}")
                        .build();
                }
                String query = queryNode.asText();
                
                JsonNode variablesNode = json.get("variables");
                Map<String, Object> variables = (variablesNode != null && !variablesNode.isNull())
                    ? objectMapper.convertValue(variablesNode, new TypeReference<>() {})
                    : Map.of();
                
                System.out.println("Pase por Aca 8");
                context.getLogger().info("Parsed GraphQL query: " + query);



                System.out.println("Pase por Aca 8 ");
                // Agregar un log aquí para ver si esta parte del código se ejecuta correctamente.
                context.getLogger().info("Parsed GraphQL query: " + query);

                ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                        .query(query)
                        .variables(variables)
                        .build();

                ExecutionResult result = graphQL.execute(executionInput);
                System.out.println("Pase por Aca 9 ");
                // Verificar la respuesta de la ejecución de GraphQL
                context.getLogger().info("Execution Result: " + result.getData().toString());


                Map<String, Object> response = result.toSpecification();

                return request
                        .createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(objectMapper.writeValueAsString(response))
                        .build();

            } catch (Exception e) {
                context.getLogger().severe("Error ejecutando la consulta GraphQL: " + e.getMessage());
                return request
                        .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"error\":\"Error procesando la petición GraphQL\"}")
                        .build();
            }
    }
}
