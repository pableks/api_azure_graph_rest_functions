package userRest.event;

import java.time.OffsetDateTime;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

import userRest.model.User;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;

/**
 * EventGridProducer handles sending events to Azure Event Grid
 * for user operations (create, update, delete) and role operations
 */
public class EventGridProducer {
    
    // Event types
    public static final String EVENT_USER_CREATED = "UserCreated";
    public static final String EVENT_USER_UPDATED = "UserUpdated";
    public static final String EVENT_USER_DELETED = "UserDeleted";
    public static final String EVENT_ROLE_DELETED = "RoleDeleted";
    public static final String EVENT_DEFAULT_ROLE_ASSIGNED = "DefaultRoleAssigned";
    
    // Default role ID to use when creating users without a role
    public static final Long DEFAULT_ROLE_ID = 2L; // Basic user role ID
    
    private final EventGridPublisherClient<EventGridEvent> eventGridPublisherClient;
    private final String eventGridTopicEndpoint = "https://topiccloud2.eastus-1.eventgrid.azure.net/api/events";
    private final String eventGridTopicKey = "2d4V2OvN8ZQISFQxvkVsX4Of4tuv3gD4W3p4ZLM8K5APV4re0wv3JQQJ99BEACYeBjFXJ3w3AAABAZEGK7uT";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Constructor that configures the Event Grid client
     */
    public EventGridProducer() {
        // Create Event Grid client using credentials and endpoint
        this.eventGridPublisherClient = new EventGridPublisherClientBuilder()
            .credential(new AzureKeyCredential(eventGridTopicKey))
            .endpoint(eventGridTopicEndpoint)
            .buildEventGridEventPublisherClient();
        
        System.out.println("EventGridProducer initialized with endpoint: " + eventGridTopicEndpoint);
    }

    /**
     * Send an event with a User object as data
     * This is the method used for user CRUD operations
     */
    public void sendEvent(String eventType, User user) {
        try {
            // Convert User object to JSON
            String eventDataJson = objectMapper.writeValueAsString(user);
            
            // Create binary data from JSON
            BinaryData data = BinaryData.fromString(eventDataJson);

            // Create Event Grid event with necessary parameters
            EventGridEvent event = new EventGridEvent(
                "User Event: " + eventType,  // subject
                eventType,                  // eventType (UserCreated, UserUpdated, UserDeleted)
                data,                       // data (User object as JSON)
                "1.0"                       // dataVersion
            );

            // Set event time to current time
            event.setEventTime(OffsetDateTime.now());
            
            // Log the event for debugging
            System.out.println("Sending event - Type: " + eventType + ", Data: " + eventDataJson);
            
            // Send event to Event Grid
            eventGridPublisherClient.sendEvent(event);
            System.out.println("Successfully sent event to Event Grid: " + eventType);
        } catch (Exception e) {
            System.err.println("Error sending event to Event Grid: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Send a role deleted event
     */
    public void sendRoleDeletedEvent(Long roleId) {
        try {
            // Create a map with the role information
            Map<String, Object> eventData = Map.of(
                "roleId", roleId
            );
            
            // Convert to JSON
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            BinaryData data = BinaryData.fromString(eventDataJson);

            // Create and send event
            EventGridEvent event = new EventGridEvent(
                "Role Event: Role Deleted",
                EVENT_ROLE_DELETED,
                data,
                "1.0"
            );
            
            event.setEventTime(OffsetDateTime.now());
            
            // Send event to Event Grid
            eventGridPublisherClient.sendEvent(event);
            System.out.println("Role deleted event sent to Event Grid for roleId: " + roleId);
        } catch (Exception e) {
            System.err.println("Error sending role deleted event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Send an event for default role assignment
     */
    public void sendDefaultRoleAssignedEvent(User user) {
        try {
            // Create a map with the user and role information
            Map<String, Object> eventData = Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "roleId", user.getRoleId()
            );
            
            // Convert to JSON
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            BinaryData data = BinaryData.fromString(eventDataJson);

            // Create and send event
            EventGridEvent event = new EventGridEvent(
                "User Event: Default Role Assigned",
                EVENT_DEFAULT_ROLE_ASSIGNED,
                data,
                "1.0"
            );
            
            event.setEventTime(OffsetDateTime.now());
            
            // Send event to Event Grid
            eventGridPublisherClient.sendEvent(event);
            System.out.println("Default role assigned event sent for user: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Error sending default role assigned event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Legacy method for sending simple string-based events
     * Keep for backwards compatibility
     */
    public void sendEvent(String eventType, String resource, String userName) {
        try {
            // Convert event data to a map
            Map<String, String> eventData = Map.of(
                "id", resource,
                "name", userName
            );
            
            // Convert map to JSON
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            BinaryData data = BinaryData.fromString(eventDataJson);

            // Create and send event
            EventGridEvent event = new EventGridEvent(
                "User Event: " + eventType,
                eventType,
                data,
                "1.0"
            );
            
            event.setEventTime(OffsetDateTime.now());
            eventGridPublisherClient.sendEvent(event);
            System.out.println("Legacy event sent to Event Grid: " + eventType);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error sending legacy event to Event Grid.");
        }
    }
}
