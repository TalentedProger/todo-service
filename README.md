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

Если порт `8080` уже занят, сервер можно запустить на другом порту:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

В таком случае в командах ниже нужно заменить `localhost:8080` на `localhost:8081`.

## Куда вводить JSON

JSON отправляется на работающий сервер как тело HTTP-запроса. Удобнее всего открыть два окна PowerShell:

1. В первом окне перейти в папку проекта и запустить сервер:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

2. Не закрывая сервер, открыть второе окно PowerShell, перейти в эту же папку и выполнять команды ниже.

Создание пользователя:

```powershell
$userJson = @{
    firstName = "Иван"
    lastName  = "Иванов"
    email     = "ivan@example.com"
    age       = 25
} | ConvertTo-Json

$user = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/users" `
    -ContentType "application/json; charset=utf-8" `
    -Body $userJson

$user
```

Ответ будет содержать созданного пользователя и его `id`. Переменная `$user.id` ниже позволяет сразу назначить этого пользователя на новую задачу:

```powershell
$taskJson = @{
    title       = "Выучить Java"
    description = "Сделать учебный проект"
    status      = "TODO"
    assigneeIds = @($user.id)
} | ConvertTo-Json

$task = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/tasks" `
    -ContentType "application/json; charset=utf-8" `
    -Body $taskJson

$task
```

Получение всех задач и изменение созданной задачи:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/v1/tasks"

$changes = @{ status = "DONE" } | ConvertTo-Json
Invoke-RestMethod `
    -Method Patch `
    -Uri "http://localhost:8080/api/v1/tasks/$($task.id)" `
    -ContentType "application/json; charset=utf-8" `
    -Body $changes
```

Удаление задачи и пользователя:

```powershell
Invoke-RestMethod -Method Delete -Uri "http://localhost:8080/api/v1/tasks/$($task.id)"
Invoke-RestMethod -Method Delete -Uri "http://localhost:8080/api/v1/users/$($user.id)"
```

В Postman нужно выбрать метод (`POST`, `PATCH` и так далее), указать URL, открыть `Body` → `raw` → `JSON`, вставить JSON и нажать `Send`. Для `GET` и `DELETE` тело запроса не требуется.

Подробное назначение всех файлов описано в [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md).

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
