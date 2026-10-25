# Project rules
- The source of truth is docs/SRS.md. Read it fully before any task. BR-n numbers refer to its rules.
- Stack: Java 17+, Spring Boot 3, PostgreSQL, React with Vite. Do not add other frameworks without asking.
- Work on one phase at a time: only the phase named in the prompt. Do not start later phases.
- The database schema changes only through Flyway migrations. Section 4 of the SRS is the target schema.
- Enforce permissions on the server (SRS section 3). Never rely on the UI to hide actions.
- Business rules live in service classes with unit tests. Keep controllers thin.
- Never commit secrets. Use environment variables and keep .env.example up to date.
- If the SRS is ambiguous or contradicts itself, stop and ask. Do not guess.
- When finished: run all tests, list what was built, and list every assumption you made.

# Commands (fill in after Phase 0)
- Start database and mail catcher: docker compose up -d
- Run backend: cd backend && .\mvnw.cmd spring-boot:run
- Run frontend: cd frontend && npm run dev
- Run all tests: cd backend && .\mvnw.cmd test; cd ../frontend && npm test

## Scope guard (strict)
- Before editing anything, list the files you plan to change, with one line of reason each. Change only those files and their tests.
- Do not refactor, rename, reformat, reorder imports, change line endings, upgrade dependencies, or "clean up" any file that the task does not require. No formatting-only changes.
- Never modify these unless the prompt names them: shared UI components (Button, Input, Card, Modal, Badge, UserAvatar), design tokens, the security configuration, Flyway migrations that already exist (add a new migration instead), AGENTS.md, and code that belongs to other phases or features. If a change in one of them seems necessary, stop and ask, naming the file and the reason. Prefer a new component or wrapper over editing them.
- Do not edit an existing test only to make it pass. Change a test only when the SRS rule it checks has changed, and say so in the report.
- Make small commits, one per logical step, so any step can be reverted alone.
- Before the final report run git diff --stat against the starting commit. Every changed file that was not in your plan must be justified in the report or reverted.
