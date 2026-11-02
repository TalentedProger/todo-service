# Сервис списка задач

Учебный Java 21 проект: запускается из консоли и принимает HTTP-запросы с JSON на `http://localhost:8080`. Данные сохраняются в локальной файловой БД H2 (`todo-data.mv.db`). ID и `createdAt` создаёт сама БД.

## Запуск

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Для запуска нужен JDK 21 и интернет при первой сборке. Устанавливать Maven отдельно не нужно. Остановить сервер: `Ctrl+C`. Проверка: `.\mvnw.cmd test` (Linux/macOS: `./mvnw test`).

## Маршруты

| Метод | Путь | Действие |
| --- | --- | --- |
| `POST` | `/api/v1/users` | Создать пользователя |
| `GET` | `/api/v1/users` | Получить пользователей |
| `GET` | `/api/v1/users/{id}` | Получить пользователя |
| `DELETE` | `/api/v1/users/{id}` | Удалить пользователя |
| `POST` | `/api/v1/tasks` | Создать задачу |
| `GET` | `/api/v1/tasks` | Получить задачи |
| `GET` | `/api/v1/tasks/{id}` | Получить задачу |
| `PATCH` | `/api/v1/tasks/{id}` | Изменить поля задачи |
| `DELETE` | `/api/v1/tasks/{id}` | Удалить задачу |
| `GET` | `/api/v1/tasks/{id}/assignees` | Получить назначенных пользователей |

`POST /api/v1/users` добавлен к заданию, чтобы пользователей можно было создать и затем назначить на задачи. Создание возвращает HTTP `201` с JSON-объектом, удаление — `200` с `{"deleted":true}`. Ошибки возвращаются как `{"error":"..."}`. Отсутствующая запись — `404`, некорректные данные — `400`, повторный email — `409`.

## Примеры JSON

Создание пользователя, `POST /api/v1/users`:

```json
{"firstName":"Иван","lastName":"Иванов","email":"ivan@example.com","age":25}
```

Создание задачи, `POST /api/v1/tasks`:

```json
{"title":"Выучить Java","description":"Сделать учебный проект","assigneeIds":[1]}
```

`status` при создании необязателен; значение по умолчанию — `IN_PROGRESS`. Допустимые значения: `TODO`, `IN_PROGRESS`, `REVIEW`, `DONE`. `description` и `assigneeIds` необязательны. В ответе задача содержит `id`, `title`, `description`, `status`, `createdAt` и массив `assignees` с пользователями.

Изменение задачи, `PATCH /api/v1/tasks/1`:

```json
{"status":"DONE","description":null,"assigneeIds":[]}
```

Передавать можно только изменяемые поля. `description: null` очищает описание; пустой `assigneeIds` снимает все назначения. При удалении пользователя его назначения удаляются автоматически.

## Ветки Git

По упрощённой схеме Gitflow есть две постоянные ветки: `develop` для работы и `main` для проверенной версии. Изменения сделаны в `develop` и после проверки слиты в `main`.
