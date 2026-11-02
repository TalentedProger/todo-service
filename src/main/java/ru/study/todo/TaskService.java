package ru.study.todo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {
    public enum Status { TODO, IN_PROGRESS, REVIEW, DONE }

    public record User(long id, String firstName, String lastName, String email, int age, Instant createdAt) { }
    public record Task(long id, String title, String description, Status status, Instant createdAt,
                       List<User> assignees) { }

    private final JdbcTemplate db;

    public TaskService(JdbcTemplate db) {
        this.db = db;
    }

    public List<User> users() {
        return db.query("SELECT * FROM users ORDER BY id", (rs, row) -> mapUser(rs));
    }

    public User user(long id) {
        try {
            return db.queryForObject("SELECT * FROM users WHERE id = ?", (rs, row) -> mapUser(rs), id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }
    }

    @Transactional
    public User createUser(String firstName, String lastName, String email, int age) {
        var key = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(
                "INSERT INTO users (first_name, last_name, email, age) VALUES (?, ?, ?, ?)",
                new String[] { "id" });
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setString(3, email.toLowerCase(Locale.ROOT));
            statement.setInt(4, age);
            return statement;
        }, key);
        return user(key.getKey().longValue());
    }

    public void deleteUser(long id) {
        if (db.update("DELETE FROM users WHERE id = ?", id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }
    }

    public List<Task> tasks() {
        return db.query("SELECT * FROM tasks ORDER BY id", (rs, row) -> mapTask(rs));
    }

    public Task task(long id) {
        try {
            return db.queryForObject("SELECT * FROM tasks WHERE id = ?", (rs, row) -> mapTask(rs), id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Задача не найдена");
        }
    }

    public List<User> assignees(long taskId) {
        return db.query("SELECT u.* FROM users u JOIN task_assignees a ON a.user_id = u.id " +
                        "WHERE a.task_id = ? ORDER BY u.id", (rs, row) -> mapUser(rs), taskId);
    }

    @Transactional
    public Task createTask(String title, String description, Status status, List<Long> assigneeIds) {
        checkUsers(assigneeIds);
        var key = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(
                "INSERT INTO tasks (title, description, status) VALUES (?, ?, ?)",
                new String[] { "id" });
            statement.setString(1, title);
            statement.setString(2, description);
            statement.setString(3, status.name());
            return statement;
        }, key);
        long id = key.getKey().longValue();
        assign(id, assigneeIds);
        return task(id);
    }

    @Transactional
    public Task updateTask(long id, String title, String description, Status status, List<Long> assigneeIds) {
        task(id);
        if (assigneeIds != null) checkUsers(assigneeIds);
        db.update("UPDATE tasks SET title = ?, description = ?, status = ? WHERE id = ?",
                  title, description, status.name(), id);
        if (assigneeIds != null) {
            db.update("DELETE FROM task_assignees WHERE task_id = ?", id);
            assign(id, assigneeIds);
        }
        return task(id);
    }

    public void deleteTask(long id) {
        if (db.update("DELETE FROM tasks WHERE id = ?", id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Задача не найдена");
        }
    }

    private void checkUsers(List<Long> ids) {
        for (long id : ids) user(id);
    }

    private void assign(long taskId, List<Long> ids) {
        for (long id : ids) {
            db.update("INSERT INTO task_assignees (task_id, user_id) VALUES (?, ?)", taskId, id);
        }
    }

    private Task mapTask(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        return new Task(id, rs.getString("title"), rs.getString("description"),
                        Status.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toInstant(), assignees(id));
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("email"), rs.getInt("age"),
                        rs.getTimestamp("created_at").toInstant());
    }
}
