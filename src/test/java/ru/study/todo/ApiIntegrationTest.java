package ru.study.todo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:todo_test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void requiredRoutesAndDatabaseRules() throws Exception {
        long firstUser = createUser("ada@example.com");
        long secondUser = createUser("bob@example.com");

        mvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/users/{id}", firstUser))
            .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Ada"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Other\",\"lastName\":\"Person\",\"email\":\"ADA@example.com\",\"age\":30}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.error").isNotEmpty());

        String taskJson = "{\"title\":\"Study Java\",\"assigneeIds\":[" + firstUser + "," + secondUser + "]}";
        var created = mvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(taskJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.assignees.length()").value(2))
            .andReturn();
        long taskId = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/v1/tasks"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/tasks/{id}", taskId))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Study Java"));
        mvc.perform(get("/api/v1/tasks/{id}/assignees", taskId))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        mvc.perform(patch("/api/v1/tasks/{id}", taskId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\",\"description\":\"Finished\",\"assigneeIds\":[" + firstUser + "]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DONE"))
            .andExpect(jsonPath("$.description").value("Finished"))
            .andExpect(jsonPath("$.assignees.length()").value(1));
        mvc.perform(patch("/api/v1/tasks/{id}", taskId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INVALID\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isNotEmpty());

        mvc.perform(delete("/api/v1/users/{id}", firstUser))
            .andExpect(status().isOk()).andExpect(jsonPath("$.deleted").value(true));
        mvc.perform(get("/api/v1/tasks/{id}/assignees", taskId))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/v1/tasks/{id}", taskId))
            .andExpect(status().isOk()).andExpect(jsonPath("$.deleted").value(true));
        mvc.perform(get("/api/v1/tasks/{id}", taskId))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").isNotEmpty());
    }

    private long createUser(String email) throws Exception {
        var result = mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"email\":\"" + email + "\",\"age\":28}"))
            .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
