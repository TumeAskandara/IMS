package com.ims.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.dto.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String message = "Authentication failed: ";
        
        // Check for specific error types
        if (request.getAttribute("expired") != null) {
            message += "Token has expired. Please login again.";
        } else if (request.getAttribute("invalid") != null) {
            message += "Invalid token. Please login again.";
        } else {
            message += "You are not authenticated. Please login.";
        }

        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .success(false)
                .message(message)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
