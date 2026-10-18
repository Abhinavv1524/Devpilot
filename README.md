# DevPilot

**AI-assisted engineering workflows, with human approval before code
changes are applied.**

DevPilot turns a concrete software change request into a structured
workflow: it researches the project, proposes file-by-file changes,
waits for approval, applies the approved changes, runs tests, and can
attempt automatic repairs when tests fail.

The platform consists of a React + Vite frontend and Spring Boot
microservices routed through Spring Cloud Gateway and registered with
Eureka.

## Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services and Ports](#services-and-ports)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Running DevPilot](#running-devpilot)
- [Task Workflow](#task-workflow)
- [API Reference](#api-reference)
- [Frontend Behavior](#frontend-behavior)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Configuration Reference](#configuration-reference)
- [Known Limitations and Roadmap](#known-limitations-and-roadmap)
- [Security Notes](#security-notes)

## Overview

DevPilot helps developers move from a requested change to a reviewed,
tested implementation.

### Core capabilities

- **AI-assisted research:** inspect the target workspace and retrieve
  relevant project guidance.
- **Change proposals:** produce a proposed set of file-level changes
  before applying them.
- **Human-in-the-loop approval:** require user approval before
  proposed changes are applied.
- **Automated testing:** run Maven tests or supported Java/C++ smoke
  tests.
- **Repair attempts:** when testing fails, make up to two automatic
  repair attempts by default.
- **Task tracking:** follow task progress, cancel eligible tasks, and
  inspect terminal task history.
- **Grounded knowledge retrieval:** use RAG with PostgreSQL and
  pgvector.
- **Tool integration:** expose workspace tools and MCP-based
  capabilities through the AI agent service.

## Architecture

```text
┌──────────────────────────────────┐
│ Frontend                         │
│ React + Vite + Material UI       │
│ http://localhost:5173            │
└────────────────┬─────────────────┘
                 │ /api/** and /tasks/history/**
                 ▼
┌──────────────────────────────────┐
│ Gateway Service                  │
│ Spring Cloud Gateway             │
│ Port 8080                        │
│ Routing · CORS · JWT validation  │
└────────────────┬─────────────────┘
                 │
                 ▼
┌──────────────────────────────────┐
│ Eureka Discovery Server          │
│ Port 8761                        │
└─────────┬─────────────┬──────────┘
          │             │
          ▼             ▼
┌────────────────┐  ┌─────────────────────┐
│ Auth Service   │  │ Orchestrator Service │
│ Port 8083      │  │ Port 8081            │
│ Signup / Login│  │ Workflow / Approval  │
│ JWT issuance   │  │ Cancellation/History │
└────────────────┘  └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ AI Agent Service    │
                    │ Port 8082            │
                    │ Research / Coding    │
                    │ Testing / RAG / MCP  │
                    └──────────┬──────────┘
                               │
                 ┌─────────────┴────────────┐
                 ▼                          ▼
       ┌──────────────────┐      ┌──────────────────┐
       │ PostgreSQL       │      │ Target Workspace │
       │ task_history     │      │ Project files    │
       │ + pgvector       │      │ inspected/edited │
       └──────────────────┘      └──────────────────┘
```

### Architecture notes

- The frontend sends API requests through the gateway.
- Eureka provides service discovery for the backend services.
- The orchestrator manages task lifecycle, routing, approval, repair
  attempts, and task history.
- The orchestrator forwards the caller's `Authorization` header to the
  AI agent service.
- Task state is held in memory while a task is running. Terminal
  states (`COMPLETED`, `FAILED`, and `CANCELLED`) are persisted to
  PostgreSQL in `task_history`.
- Spring AI sends model requests to an OpenAI-compatible endpoint,
  configured through environment variables.
- The AI agent service uses workspace tools and RAG backed by
  pgvector. Guideline PDFs are ingested at startup, according to the
  current project configuration.

## Services and Ports

---

Component Port Responsibility

---

Frontend `5173` React UI, workspace
selection, task
details and approval

Gateway service `8080` API entry point,
routing, CORS, JWT
validation and
OpenAPI aggregation

Discovery server `8761` Eureka service
registry; does not
self-register

Orchestrator service `8081` Task lifecycle,
AI/fallback routing,
approval, repair loop
and history

AI agent service `8082` Research, coding
proposals, testing,
workspace tools, RAG
and MCP

Auth service `8083` Signup, login and JWT
generation

---

Swagger UI is available through the gateway at:

`http://localhost:8080/swagger-ui.html`

## Technology Stack

Area Technologies

---

Frontend React, Vite, Material UI
Backend Java 21, Spring Boot, Spring Cloud
API gateway Spring Cloud Gateway
Service discovery Netflix Eureka
AI integration Spring AI, OpenAI-compatible API
Authentication JWT
Data storage PostgreSQL, pgvector
Build and tests Maven Wrapper, npm
Agent tools Workspace tools and MCP integration

## Prerequisites

Install or configure the following before starting the application:

- **Java 21**
- **Node.js 20 or later** and npm
- **PostgreSQL 16 or later**
- The PostgreSQL **pgvector** extension enabled
- An API key and base URL for an OpenAI-compatible model endpoint
- Git, if cloning the repository

The repository includes Maven Wrapper scripts, so a separate Maven
installation should not be necessary.

## Getting Started

### 1. Clone the repository

```bash
git clone <repository-url>
cd DevPilot
```

Replace `<repository-url>` with the URL of your repository.

### 2. Create the database

Create a PostgreSQL database named `devpilot` unless you choose a
different name in your configuration. Ensure pgvector is installed and
enabled for the database.

### 3. Configure environment variables

Copy the root environment template to a local environment file:

**PowerShell**

```powershell
Copy-Item .env.example .env
```

If you prefer per-service configuration, copy each service's
`.env.example` to `.env` in that service directory as well.

Review the copied files and replace example values with your actual
local configuration. See [Configuration
Reference](#configuration-reference).

> **Important:** The services are configured to import `.env` using
> `optional:file:.env[.properties]`. Keep each file in the location
> expected by the service's working directory. Do not assume a root
> `.env` is automatically visible to every service when they are
> launched from their own directories.

### 4. Keep local secrets private

- Do not commit `.env` files.
- Keep `.env.example` as a template containing variable names and safe
  placeholder values.
- Generate a strong random `JWT_SECRET`. For example, with OpenSSL:

```bash
openssl rand -base64 64
```

Use the generated value consistently wherever services that validate the
same JWT require it.

### 5. Install frontend dependencies

```powershell
cd frontend
npm install
cd ..
```

## Running DevPilot

### Option A: Start everything on Windows

From the repository root, run:

```bat
start-devpilot.bat
```

The script starts the discovery server, gateway, auth service,
orchestrator service, AI agent service, and frontend in separate command
windows.

To stop the services using the provided script:

```bat
stop-devpilot.bat
```

### Option B: Start services manually

For easier troubleshooting, start the backend services in this order.
Open a separate terminal for each long-running service.

**1. Discovery server**

```powershell
cd discovery-server
.\mvnw.cmd spring-boot:run
```

**2. Gateway service**

```powershell
cd gateway-service
.\mvnw.cmd spring-boot:run
```

**3. Auth service**

```powershell
cd auth-service
.\mvnw.cmd spring-boot:run
```

**4. Orchestrator service**

```powershell
cd orchestrator-service
.\mvnw.cmd spring-boot:run
```

**5. AI agent service**

```powershell
cd ai-agent-service
.\mvnw.cmd spring-boot:run
```

**6. Frontend**

```powershell
cd frontend
npm install
npm run dev
```

Open the frontend at **http://localhost:5173**. API requests are routed
through **http://localhost:8080**.

### Local startup checklist

Before investigating application errors, verify that:

- PostgreSQL is running and the configured database exists.
- Required environment variables are available to each service.
- `JWT_SECRET` is configured and consistent across the services that
  validate the same tokens.
- `OPENAI_API_KEY` and `OPENAI_BASE_URL` are valid for the chosen
  model provider.
- The configured workspace path exists and points to the project you
  intend the agent to inspect or modify.
- The services have started successfully before testing end-to-end
  flows.

## Task Workflow

A typical task moves through the following stages:

1.  **Receive** --- The task is created with status `RECEIVED`.
2.  **Route** --- Spring AI attempts to select the appropriate agent or
    intent. A keyword-based router is used as a fallback.
3.  **Research** --- The research agent inspects the workspace and
    retrieves relevant RAG context from pgvector.
4.  **Propose** --- The coding agent returns a file-level change
    proposal. Proposed paths are validated against `projectPath`, and
    the task moves to `AWAITING_APPROVAL`.
5.  **Approve** --- The user reviews and approves the proposal before
    changes are applied.
6.  **Apply and test** --- Workspace tools apply the approved changes,
    then the testing agent runs the configured Maven tests or supported
    smoke tests.
7.  **Repair if needed** --- If tests fail, the workflow can make up to
    `max-repair-attempts=2` repair attempts. Each attempt proposes
    changes, applies them, and tests again.
8.  **Finish** --- The task ends in `COMPLETED` or `FAILED`. A cancelled
    task ends in `CANCELLED`.

### Cancellation and history

- Cancellation is cooperative: the orchestrator checks
  `cancelRequested` between workflow stages.
- Cancellation does not interrupt an agent HTTP call that is already
  in progress.
- Terminal tasks (`COMPLETED`, `FAILED`, `CANCELLED`) are persisted to
  `task_history`.
- Non-terminal task state is held in orchestrator memory and is not
  persisted as task history.

## API Reference

The following are key endpoints exposed by the application. Use the
gateway as the API entry point unless an endpoint is explicitly intended
for internal service-to-service use.

---

Method Path Purpose

---

`POST` `/api/auth/signup` Register a user

`POST` `/api/auth/login` Log in and receive a
JWT `token`

`POST` `/api/tasks` Create and start a
workflow asynchronously

`GET` `/api/tasks/{taskId}` Retrieve task state

`POST` `/api/tasks/{taskId}/approve` Approve a proposal and
continue to apply/test

`POST` `/api/tasks/{taskId}/cancel` Request cancellation of
a non-terminal task

`GET` `/tasks/history` Retrieve the latest 20
terminal tasks for the
current owner

`GET` `/tasks/history/{taskId}` Retrieve details for a
historical task

`POST` `/api/agents/research` Run the research agent

`POST` `/api/agents/code/plan` Create a coding or
repair proposal

`POST` `/api/agents/code/apply` Apply approved changes

`POST` `/api/agents/test` Run Maven or smoke
tests

`POST` `/api/knowledge/ask` Ask a question using
grounded RAG

`POST` `/api/knowledge/search` Search the knowledge
store

---

Authentication, request bodies, response schemas, and endpoint
availability may depend on the implementation and gateway configuration.
Use the aggregated Swagger UI for the current API documentation.

## Frontend Behavior

- **Asynchronous task creation:** `POST /api/tasks` returns
  immediately; the workflow continues in the background.
- **Task polling:** the Task Details page polls
  `GET /api/tasks/{taskId}` every three seconds while the task is
  non-terminal.
- **Polling states:** `RECEIVED`, `RESEARCHING`, `CODING`,
  `AWAITING_APPROVAL`, and `TESTING`.
- **Approval:** the Approve button is shown when the task reaches
  `AWAITING_APPROVAL`.
- **Cancellation:** the Cancel button is available for non-terminal
  tasks.
- **Workspace data:** workspace information and mirrored task status
  are stored client-side in `localStorage`, per username.
- **Authentication token:** the JWT is stored in `sessionStorage`.
  Axios adds the `Authorization: Bearer` header, and a `401` response
  triggers logout.

## Project Structure

```text
DevPilot/
├── discovery-server/       # Eureka service registry
├── gateway-service/        # Gateway, JWT filter, routes and API docs
├── auth-service/            # Signup, login and JWT issuance
├── orchestrator-service/    # Workflow, async execution, approval, cancel and history
├── ai-agent-service/        # Agents, MCP, RAG and workspace tools
├── frontend/                # React + Vite frontend
├── start-devpilot.bat       # Windows startup script
├── stop-devpilot.bat        # Windows shutdown script
├── .env.example             # Root environment template
└── README.md                # Project documentation
```

Each service also has its own `.env.example` and `.gitignore`. The root
`.gitignore` excludes local environment files, build outputs, frontend
dependencies, IDE artifacts, and logs.

## Testing

Run backend tests with the Maven Wrapper from each service directory:

```powershell
cd orchestrator-service
.\mvnw.cmd test

cd ..\ai-agent-service
.\mvnw.cmd test

cd ..\auth-service
.\mvnw.cmd test

cd ..\gateway-service
.\mvnw.cmd test

cd ..\discovery-server
.\mvnw.cmd test
```

Current backend test coverage includes:

- Orchestrator cancellation behavior, including immediate cancellation
  for `RECEIVED`, rejecting cancellation of terminal tasks, owner
  checks, and cooperative stopping.
- Asynchronous workflow semantics.
- History persistence for `CANCELLED` tasks and exclusion of
  non-terminal tasks.
- Task controller cancellation endpoint behavior.

The frontend currently has no automated tests configured.

## Configuration Reference

The exact values and defaults should be checked against the
`.env.example` files and each service's configuration.

---

Variable Purpose

---

`OPENAI_API_KEY` API key for the configured model
provider

`OPENAI_BASE_URL` Base URL for the OpenAI-compatible
API

`JWT_SECRET` Secret used for JWT signing and/or
validation, as configured by the
services

`DB_URL` JDBC URL for PostgreSQL

`DB_USERNAME` PostgreSQL username

`DB_PASSWORD` PostgreSQL password

`EUREKA_URL` Eureka endpoint; default
`http://localhost:8761/eureka/`

`CORS_ALLOWED_ORIGINS` Allowed frontend origin; default
`http://localhost:5173`

`DEVPILOT_WORKSPACE_ROOT` Absolute path to the workspace the
agent may inspect or modify; a
`./workspace` default is used for
development/tests

---

### JWT secret troubleshooting

If startup fails with an error such as:

```text
Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}"
```

the application cannot find the required variable in its Spring
configuration environment.

1.  Ensure `JWT_SECRET` exists in the relevant `.env` file or process
    environment.
2.  Check that the service loads the file from the expected working
    directory.
3.  For a quick PowerShell test, set the variable in the same terminal
    before starting the service:

```powershell
$env:JWT_SECRET = "paste-your-generated-secret-here"
.\mvnw.cmd spring-boot:run
```

Setting a PowerShell variable only affects that terminal session and its
child processes. Do not commit the secret.

## Known Limitations and Roadmap

- **Database migrations:** Hibernate currently uses `ddl-auto=update`;
  Flyway or Liquibase migrations are not configured.
- **CI:** GitHub Actions workflows are not configured.
- **Authentication hardening:** refresh tokens, role-based
  authorization, and account lockout are not implemented.
- **Live task updates:** the frontend polls every three seconds;
  SSE/WebSocket updates are not implemented.
- **Documentation agent:** a stub exists, but the agent is not
  implemented.
- **Frontend quality checks:** automated frontend tests and linting
  are not configured.
- **Cancellation:** cancellation is cooperative between workflow
  stages and cannot interrupt an in-flight agent HTTP call.

## Security Notes

DevPilot can inspect and modify files in the configured workspace. Use
it only with a workspace you are authorized to access.

Before pushing changes or deploying:

- Keep `.env` files and all real credentials out of version control.
- Keep `.env.example` limited to variable names and safe placeholder
  values.
- Avoid hardcoding API keys, passwords, JWT secrets, or
  machine-specific absolute paths.
- Rotate credentials if they were ever committed or shared
  unintentionally.
- Review proposed file changes before approving them.
- Restrict workspace access and permissions to the minimum required.
- Add production-grade secrets management, rate limiting, database
  migrations, and appropriate security controls before production
  deployment.

---

## License

No license information was provided in the current project description.
Add a `LICENSE` file and update this section if you intend to publish
the repository.
