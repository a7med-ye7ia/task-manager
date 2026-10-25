# Task Manager

A web application for managing team tasks. A Manager creates tasks, assigns employees and follows each task through review until it is done. A task can have subtasks, checkpoints, a Lead, comments and an activity log. Finished tasks are kept in an archive for 30 days and then deleted automatically.

The backend uses Spring Boot and PostgreSQL. The frontend uses React with Vite.

## What you need

- JDK 21 (17 is the minimum. Very new versions such as 26 can break the test tooling)
- Node.js 20 or newer
- Docker Desktop (it runs PostgreSQL and a fake mail server for development)
- Git

## First time setup

1. Clone the repository and open the project folder.

2. Create your local settings file. The defaults work for development.

   ```
   copy .env.example .env        (Windows)
   cp .env.example .env          (macOS or Linux)
   ```

   Never commit the `.env` file. It is already ignored by Git.

3. Start the database and the mail catcher.

   ```
   docker compose up -d
   ```

   Run `docker ps` and wait until both containers show as Up.

4. Start the backend in a new terminal.

   ```
   cd backend
   .\mvnw.cmd spring-boot:run        (Windows)
   ./mvnw spring-boot:run            (macOS or Linux)
   ```

   On the first start it creates the tables and one Manager account. The backend listens on http://localhost:8080.

5. Start the frontend in another terminal.

   ```
   cd frontend
   npm install
   npm run dev
   ```

   Open http://localhost:5173.

## Signing in

The first Manager account comes from the seed Manager settings in `.env` (see `.env.example`). Sign in with it, then invite employees from the Users page.

People can also sign up on their own. They confirm their email, and then a Manager approves them from the Pending Requests page. Only Gmail addresses are accepted.

## Emails in development

No real email is sent while you develop. Every message goes to Mailpit, a local inbox at http://localhost:8025. Invitation, verification and password reset links show up there.

To send real email through Gmail you need a Gmail App Password. The Gmail section in `.env.example` explains the settings. Use only addresses you own while testing.

## Tests

The backend tests need the PostgreSQL container running. They use a separate database called `taskmanagesys_test`, so your development data is not touched.

```
cd backend
.\mvnw.cmd -B test
```

Frontend tests:

```
cd frontend
npx vitest run
```

## Common problems

- Port 8080 is already in use. Another copy of the backend is still running. Find it with `netstat -ano | findstr ":8080"`, stop that process, and start the backend again.
- The backend cannot connect to the database. Check that Docker Desktop is running and that `docker ps` lists the postgres container.
- You want an empty database again. Run `docker compose down -v`, then `docker compose up -d`, then restart the backend. This deletes all local data.

## Project layout

```
backend/             Spring Boot application, database migrations and tests
frontend/            React application
docs/                Project documentation
docker-compose.yml   PostgreSQL and Mailpit for development
.env.example         Template for the local settings
```

## Documentation

`docs/SRS.md` describes every rule of the system. The rules are numbered (BR-1, BR-2 and so on), and the code and tests refer to those numbers.

`AGENTS.md` holds instructions for AI coding assistants. You can ignore it if you do not use one.