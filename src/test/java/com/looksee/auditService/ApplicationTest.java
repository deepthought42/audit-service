package com.looksee.auditService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ApplicationTest {
    @Test
    void applicationClassCanBeInstantiated() {
        Application application = new Application();
        assertNotNull(application);
    }
}
