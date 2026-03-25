import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi chargingStationApi() {
        return GroupedOpenApi.builder()
                .group("charging-station-api")            // This will be part of the URL
                .pathsToMatch("/api/charging-stations")   // Only include this endpoint
                .build();
    }
}
