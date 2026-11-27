package com.just.AiAutomationTranslater.translatorservice.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.just.AiAutomationTranslater.translatorservice.res.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDenialHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(@NonNull HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        Response<?> errorResponse = Response.builder()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .message(accessDeniedException.getMessage())
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
