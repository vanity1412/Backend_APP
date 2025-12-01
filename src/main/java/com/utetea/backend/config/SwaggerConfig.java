package com.utetea.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";
        
        return new OpenAPI()
                .info(new Info()
                        .title("UTE Tea API Documentation")
                        .version("1.0.0")
                        .description("""
                                ## 🍵 UTE Tea - Milk Tea Ordering Application
                                
                                Backend API cho ứng dụng đặt trà sữa UTE Tea.
                                
                                ### 🔐 Authentication
                                - Sử dụng JWT Bearer Token
                                - Login tại `/api/auth/login` để lấy token
                                - Click nút **Authorize** ở trên, nhập: `Bearer <your-token>`
                                
                                ### 👥 Roles
                                - **GUEST**: Public endpoints (drinks, stores, categories)
                                - **USER**: Order management, profile
                                - **MANAGER**: Dashboard, order management, CRUD menu
                                
                                ### 📱 Test Accounts
                                - Manager: `manager_ute` / `123456`
                                - User: `ute_student_01` / `123456`
                                """)
                        .contact(new Contact()
                                .name("UTE Tea Team")
                                .email("support@utetea.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://192.168.1.100:" + serverPort)
                                .description("Network Server (Replace with your IP)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token (without 'Bearer' prefix)")));
    }
}
