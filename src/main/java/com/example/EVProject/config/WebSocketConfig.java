// // WebSocketConfig.java - Minimal version
// package com.example.EVProject.config;

// import com.example.EVProject.handler.OcppWebSocketHandler;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.messaging.simp.config.MessageBrokerRegistry;
// import org.springframework.web.socket.config.annotation.EnableWebSocket;
// import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
// import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
// import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
// import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
// import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
// import com.example.EVProject.services.OcppWebSocketService;
// import com.example.EVProject.repositories.SmartPlugRepository;

// // // @Configuration
// // // @EnableWebSocket
// // // @EnableWebSocketMessageBroker
// // // public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

// // //     @Autowired
// // //     private OcppWebSocketHandler ocppWebSocketHandler;
    
// // //     @Override
// // //     public void configureMessageBroker(MessageBrokerRegistry config) {
// // //         config.enableSimpleBroker("/topic", "/queue");
// // //         config.setApplicationDestinationPrefixes("/app", "/ws");
// // //     }
    
// // //     @Override
// // //     public void registerStompEndpoints(StompEndpointRegistry registry) {
// // //         // For frontend STOMP connection
// // //         registry.addEndpoint("/ws-stomp")
// // //                 .setAllowedOriginPatterns("*")
// // //                 .withSockJS();
// // //     }

// // //     @Bean
// // //     public OcppWebSocketHandler ocppWebSocketHandler(
// // //             OcppWebSocketService ocppWebSocketService,
// // //             SmartPlugRepository smartPlugRepository) {
        
// // //         return new OcppWebSocketHandler(ocppWebSocketService, smartPlugRepository);
// // //     }

// // //     @Override
// // //     public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
// // //         registry.addHandler(ocppWebSocketHandler, "/ws-ocpp/{deviceId}")
// // //                 .setAllowedOriginPatterns("*");
// // //     }
// // // }

// // @Configuration
// // @EnableWebSocket
// // @EnableWebSocketMessageBroker
// // public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

// //     private final OcppWebSocketHandler ocppWebSocketHandler;

// //     // ✅ Constructor injection (NO circular dependency)
// //     public WebSocketConfig(OcppWebSocketHandler ocppWebSocketHandler) {
// //         this.ocppWebSocketHandler = ocppWebSocketHandler;
// //     }

// //     @Override
// //     public void configureMessageBroker(MessageBrokerRegistry config) {
// //         config.enableSimpleBroker("/topic", "/queue");
// //         config.setApplicationDestinationPrefixes("/app", "/ws");
// //     }

// //     @Override
// //     public void registerStompEndpoints(StompEndpointRegistry registry) {
// //         registry.addEndpoint("/ws-stomp")
// //                 .setAllowedOriginPatterns("*")
// //                 .withSockJS();
// //     }

// //     @Override
// //     public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
// //         registry.addHandler(ocppWebSocketHandler, "/ws-ocpp/{deviceId}")
// //                 .setAllowedOriginPatterns("*");
// //     }
// // }

// // // package com.example.EVProject.config;

// // // import com.example.EVProject.handler.OcppWebSocketHandler;
// // // import org.springframework.context.annotation.Configuration;
// // // import org.springframework.web.socket.config.annotation.EnableWebSocket;
// // // import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
// // // import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// // // @Configuration
// // // @EnableWebSocket
// // // public class WebSocketConfig implements WebSocketConfigurer {

// // //     private final OcppWebSocketHandler ocppWebSocketHandler;

// // //     // Constructor injection avoids circular dependency
// // //     public WebSocketConfig(OcppWebSocketHandler ocppWebSocketHandler) {
// // //         this.ocppWebSocketHandler = ocppWebSocketHandler;
// // //     }

// // //     @Override
// // //     public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
// // //         // Register handler for heartbeat API
// // //         registry.addHandler(ocppWebSocketHandler, "/ws-ocpp/{deviceId}")
// // //                 .setAllowedOriginPatterns("*"); // allow connections from any origin
// // //     }
// // // }

// // // WebSocketConfig.java - Minimal version
// // package com.example.EVProject.config;

// // import com.example.EVProject.handler.OcppWebSocketHandler;
// // import org.springframework.beans.factory.annotation.Autowired;
// // import org.springframework.context.annotation.Configuration;
// // import org.springframework.messaging.simp.config.MessageBrokerRegistry;
// // import org.springframework.web.socket.config.annotation.EnableWebSocket;
// // import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
// // import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
// // import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
// // import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
// // import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// @Configuration
// @EnableWebSocket
// @EnableWebSocketMessageBroker
// public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {
    
//     @Autowired
//     private OcppWebSocketHandler ocppWebSocketHandler;
    
//     @Override
//     public void configureMessageBroker(MessageBrokerRegistry config) {
//         config.enableSimpleBroker("/topic", "/queue");
//         config.setApplicationDestinationPrefixes("/app", "/ws");
//     }
    
//     @Override
//     public void registerStompEndpoints(StompEndpointRegistry registry) {
//         // For frontend STOMP connection
//         registry.addEndpoint("/ws-stomp")
//                 .setAllowedOriginPatterns("*")
//                 .withSockJS();
//     }
    
//     @Override
//     public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//         // For OCPP charger connection (raw WebSocket)
//         registry.addHandler(ocppWebSocketHandler, "/ws-ocpp/{deviceId}")
//                 .setAllowedOriginPatterns("*");
//     }
// }

package com.example.EVProject.config;

import com.example.EVProject.handler.OcppWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {
    
    private OcppWebSocketHandler ocppWebSocketHandler;
    
    @Autowired
    public void setOcppWebSocketHandler(@Lazy OcppWebSocketHandler ocppWebSocketHandler) {
        this.ocppWebSocketHandler = ocppWebSocketHandler;
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app", "/ws");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(ocppWebSocketHandler, "/ws-ocpp/{deviceId}")
                .setAllowedOriginPatterns("*");
    }
}