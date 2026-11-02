package ru.study.todo;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ApiController {
    private final TaskService service;

    public ApiController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public List<TaskService.User> users() { return service.users(); }

    @GetMapping("/users/{id}")
    public TaskService.User user(@PathVariable long id) { return service.user(id); }

    @PostMapping("/users")
    public ResponseEntity<TaskService.User> createUser(@RequestBody JsonNode body) {
        requireObject(body);
        String firstName = requiredText(body, "firstName");
        String lastName = requiredText(body, "lastName");
        String email = requiredText(body, "email").toLowerCase(Locale.ROOT);
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) bad("Некорректный email");
        JsonNode ageNode = body.get("age");
        if (ageNode == null || !ageNode.isIntegralNumber() || !ageNode.canConvertToInt() || ageNode.intValue() < 0) {
            bad("age должен быть неотрицательным целым числом");
        }
        var result = service.createUser(firstName, lastName, email, ageNode.intValue());
        return ResponseEntity.created(URI.create("/api/v1/users/" + result.id())).body(result);
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Boolean> deleteUser(@PathVariable long id) {
        service.deleteUser(id);
        return Map.of("deleted", true);
    }

    @GetMapping("/tasks")
    public List<TaskService.Task> tasks() { return service.tasks(); }

    @GetMapping("/tasks/{id}")
    public TaskService.Task task(@PathVariable long id) { return service.task(id); }

    @GetMapping("/tasks/{id}/assignees")
    public List<TaskService.User> assignees(@PathVariable long id) {
        service.task(id);
        return service.assignees(id);
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskService.Task> createTask(@RequestBody JsonNode body) {
        requireObject(body);
        String title = requiredText(body, "title");
        String description = optionalDescription(body);
        var status = body.has("status") ? status(body.get("status")) : TaskService.Status.IN_PROGRESS;
        List<Long> ids = body.has("assigneeIds") ? assigneeIds(body.get("assigneeIds")) : List.of();
        var result = service.createTask(title, description, status, ids);
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + result.id())).body(result);
    }

    @PatchMapping("/tasks/{id}")
    public TaskService.Task updateTask(@PathVariable long id, @RequestBody JsonNode body) {
        requireObject(body);
        var current = service.task(id);
        String title = body.has("title") ? requiredText(body, "title") : current.title();
        String description = body.has("description") ? optionalDescription(body) : current.description();
        var status = body.has("status") ? status(body.get("status")) : current.status();
        List<Long> ids = body.has("assigneeIds") ? assigneeIds(body.get("assigneeIds")) : null;
        return service.updateTask(id, title, description, status, ids);
    }

    @DeleteMapping("/tasks/{id}")
    public Map<String, Boolean> deleteTask(@PathVariable long id) {
        service.deleteTask(id);
        return Map.of("deleted", true);
    }

    private static void requireObject(JsonNode body) {
        if (body == null || !body.isObject()) bad("Ожидается JSON-объект");
    }

    private static String requiredText(JsonNode body, String name) {
        var value = body.get(name);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            bad(name + " должно быть непустой строкой");
        }
        return value.textValue().trim();
    }

    private static String optionalDescription(JsonNode body) {
        var value = body.get("description");
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) bad("description должно быть строкой или null");
        return value.textValue();
    }

    private static TaskService.Status status(JsonNode value) {
        if (value == null || !value.isTextual()) bad("Некорректный status");
        try {
            return TaskService.Status.valueOf(value.textValue());
        } catch (IllegalArgumentException e) {
            bad("status: TODO, IN_PROGRESS, REVIEW или DONE");
            throw e;
        }
    }

    private static List<Long> assigneeIds(JsonNode value) {
        if (value == null || !value.isArray()) bad("assigneeIds должен быть массивом ID");
        var ids = new LinkedHashSet<Long>();
        for (var item : value) {
            if (!item.isIntegralNumber() || !item.canConvertToLong() || item.longValue() <= 0) bad("Некорректный ID пользователя");
            ids.add(item.longValue());
        }
        return new ArrayList<>(ids);
    }

    private static void bad(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
