package com.csports.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@ActiveProfiles("test")
class ApiVersioningTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void versionedAndLegacyRoutesRemainAvailable() {
        Set<String> patterns = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .collect(Collectors.toSet());

        assertThat(patterns).contains(
                "/api/v1/auth/login",
                "/auth/login",
                "/api/v1/sports/list",
                "/sports/list",
                "/api/v1/sessions",
                "/sessions",
                "/api/v1/users/me",
                "/users/me",
                "/api/v1/trainers/{trainerId}",
                "/trainers/{trainerId}"
        );
    }

    @Test
    void sessionUpdateUsesPatchInsteadOfPut() {
        boolean patchExists = handlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(mapping ->
                        mapping.getPatternValues().contains("/api/v1/sessions/{sessionId}")
                        && mapping.getMethodsCondition().getMethods().contains(RequestMethod.PATCH));
        boolean putExists = handlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(mapping ->
                        mapping.getPatternValues().contains("/api/v1/sessions/{sessionId}")
                        && mapping.getMethodsCondition().getMethods().contains(RequestMethod.PUT));

        assertThat(patchExists).isTrue();
        assertThat(putExists).isFalse();
    }
}
