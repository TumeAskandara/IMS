package com.ims.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.dto.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        String message = "Access denied: You do not have permission to access this resource. " +
                        "Required role: ADMIN";

        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .success(false)
                .message(message)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
