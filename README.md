# Speaking Club Bot

**Speaking Club Bot** — это масштабируемый телеграм-бот для записи на встречи разговорных клубов. 
Он предоставляет удобный интерфейс для пользователей, чтобы записываться на различные встречи разговорных клубов, а также предоставляет возможность для администраторов управлять процессом записи.

## Требования

- Docker
- Docker Compose

## Настройка

1. **Клонировать репозиторий**

    Склонируйте репозиторий на вашу машину:
    ```bash
    git clone https://github.com/yourusername/yourrepository.git
    cd yourrepository
    ```

2. **Настроить переменные окружения**

    Скопируйте файл `.env.example` в `.env`:
    ```bash
    cp .env.example .env
    ```

    Обновите значения в файле `.env` (например, для `BOT_TOKEN`, `POSTGRES_PASSWORD`, и других чувствительных данных).

    ```plaintext
    # Пример содержимого файла .env

    # Dispatcher
    DISPATCHER_PORT=8081

    # Node
    NODE_PORT=8082

    # Telegram Bot API
    BOT_TOKEN=YOUR_TELEGRAM_BOT_TOKEN
    BOT_USERNAME=your_bot_username
    MANAGER_ID=your_manager_id
    CONTEXT_LIMIT=20000

    # Rabbit MQ
    RABBIT_HOST=rabbit-mq
    RABBIT_PORT=5672

    RABBIT_AMQP_PORT_MAPPING=5673:5672
    RABBIT_GUI_PORT_MAPPING=15673:15672

    RABBIT_USERNAME=your_rabbit_username
    RABBIT_PASSWORD=your_rabbit_password

    RABBIT_TEXT_MESSAGE_UPDATE_QUEUE=message_update
    RABBIT_ANSWER_MESSAGE_QUEUE=message_answer

    # Postgres
    POSTGRES_PORT_MAPPING=5433:5432
    POSTGRES_USER=your_postgres_user
    POSTGRES_PASSWORD=your_postgres_password
    POSTGRES_DATABASE=your_database_name
    POSTGRES_URL=jdbc:postgresql://postgres-db:5432/your_database_name
    ```

3. **Собрать контейнеры**

    Для того чтобы собрать контейнеры и запустить приложение в Docker, выполните следующую команду:
    ```bash
    docker-compose up --build
    ```

4. **Проверить статус**

    Чтобы проверить, что контейнеры запущены и работают, используйте:
    ```bash
    docker-compose ps
    ```

5. **Остановить контейнеры**

    Чтобы остановить запущенные контейнеры, выполните:
    ```bash
    docker-compose down
    ```

## Используемые сервисы

- **Dispatcher** — Сервер, который слушает запросы на порту 8081.
- **Node** — Сервис, который слушает запросы на порту 8082.
- **Telegram Bot API** — Телеграм-бот для взаимодействия с пользователями.
- **RabbitMQ** — Очереди сообщений для обработки.
- **Postgres** — Система управления базами данных для хранения данных.

## Логирование

Вы можете следить за логами контейнеров с помощью команды:

```bash
docker-compose logs -f
