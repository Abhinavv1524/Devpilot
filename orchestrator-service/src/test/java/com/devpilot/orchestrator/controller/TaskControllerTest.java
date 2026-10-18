package com.devpilot.orchestrator.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.devpilot.orchestrator.model.TaskState;
import com.devpilot.orchestrator.service.OrchestrationService;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private OrchestrationService orchestrationService;

    private MockMvc mockMvc;

    private final Principal principal = () -> "alice";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(orchestrationService)).build();
    }

    @Test
    void cancelReturnsTheCancelledTask() throws Exception {
        TaskState task = new TaskState("desc", "alice");
        task.setStatus("CANCELLED");
        when(orchestrationService.cancelTask(eq(task.getTaskId()), eq("alice"))).thenReturn(task);

        mockMvc.perform(post("/api/tasks/{taskId}/cancel", task.getTaskId()).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
