package com.ims.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CorsLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader("Origin");
        String method = httpRequest.getMethod();

        log.info("CORS Request - Method: {}, Origin: {}, Path: {}",
                method, origin, httpRequest.getRequestURI());

        chain.doFilter(request, response);

        log.info("CORS Response - Access-Control-Allow-Origin: {}",
                httpResponse.getHeader("Access-Control-Allow-Origin"));
    }
}