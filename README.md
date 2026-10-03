# DevPilot

DevPilot is an AI-powered GitHub repository assistant that lets developers connect a repository, index its codebase, and ask grounded questions about the code using semantic retrieval and Retrieval-Augmented Generation (RAG).

---

## Architecture

```mermaid
flowchart TD
    subgraph Client ["Frontend (Next.js 16 + React 19)"]
        UI[Web UI]
        Chat[Chat Interface]
        Cat[Repository Catalog]
    end

    subgraph Backend ["Backend (Spring Boot 4 + Java 21)"]
        Auth[OAuth2 / Session Security]
        Ingest[Repository Ingestion Service]
        Chunker[Language-Aware Chunking]
        Embed[Embedding Service]
        ChatSvc[Repository Chat Service]
        Flyway[Flyway Migrations]
    end

    subgraph External ["External Services"]
        GH[GitHub REST API]
        OAI[OpenAI gpt-4o-mini & text-embedding-3-small]
    end

    subgraph Storage ["Database (Render PostgreSQL)"]
        PG[(PostgreSQL + pgvector)]
    end

    UI -->|Session Cookie + CSRF| Auth
    Cat -->|GitHub Repositories| Ingest
    Ingest -->|Fetch Tree & Blobs| GH
    Ingest -->|File Content| Chunker
    Chunker -->|Code Chunks| Embed
    Embed -->|Vector Embeddings| OAI
    Embed -->|Save Embeddings| PG
    Chat -->|Question| ChatSvc
    ChatSvc -->|Vector Similarity HNSW| PG
    ChatSvc -->|Prompt + Context| OAI
    ChatSvc -->|Grounded Answer + Citations| Chat
    Flyway -->|Apply V1__initial_schema.sql| PG
```

---

## Tech Stack

### Backend
- **Java 21** with **Spring Boot 4.1.x**
- **Spring Security** with OAuth2 Client & session cookie protection (`SameSite=None`, `Secure` in production)
- **Spring AI 2.0.1** (OpenAI Chat `gpt-4o-mini`, Embeddings `text-embedding-3-small`)
- **Spring Data JPA** with Hibernate (DDL validation mode in production)
- **Flyway** for database migrations (`flyway-database-postgresql`)
- **Spring Boot Actuator** (`/actuator/health`, liveness/readiness probes)
- **HikariCP** connection pool with URL normalization for cloud databases
- **Maven** build system

### Frontend
- **Next.js 16** (App Router, Turbopack)
- **React 19** & **TypeScript**
- **TanStack React Query v5**
- **Tailwind CSS 4** with responsive themes (dark/light)
- Self-contained UI styling (no compile-time external font downloads)

### Database & Storage
- **PostgreSQL 16+** with the **pgvector** extension
- HNSW index with cosine similarity metric (`vector_cosine_ops`)

---

## Core Features

- **GitHub OAuth2 Authentication**: Secure sign-in with GitHub. User access tokens are encrypted with AES-256 before database storage and never exposed to the client.
- **CSRF & Session Security**: Cookie-based authentication with CSRF tokens on state-changing requests, cross-site cookie support (`SameSite=None; Secure`), and Spring Security Authorization.
- **Repository Ingestion & Progress Tracking**: Resolves default branch commit SHA, discovers candidate files, filters out binaries and vendor artifacts, and tracks real-time file counts.
- **Ingestion Recovery**: In the event of service restart during indexing, `IngestionRecoveryService` automatically transitions interrupted jobs from `RUNNING`/`QUEUED` to `FAILED_INGESTION` with helpful error messages, allowing clean user retries without duplicate processing.
- **Language-Aware Chunking**: Heuristic chunking splitting files by logical boundaries with sliding window overlaps and line-number tracking.
- **Semantic Code Retrieval**: Queries are embedded and compared against stored chunk vectors using pgvector HNSW cosine distance.
- **Grounded Q&A with Citations**: Responses are constrained to authoritative source code context. Every answer includes verifiable GitHub file and line-range citations (`path#Lstart-Lend`).
- **Resilient External Integrations**: Outbound GitHub and OpenAI API calls include bounded retries, exponential backoff, connection/read timeouts, and fast-fail behavior on permanent client errors (HTTP 4xx).

---

## RAG Pipeline

```text
GitHub Repository
  │
  ▼
Fetch Commit Tree & Filter Candidate Files
  │
  ▼
Extract Text & Heuristic Chunking (Language-aware, line numbers)
  │
  ▼
Generate Vector Embeddings (text-embedding-3-small, 1536 dim)
  │
  ▼
Store in PostgreSQL + pgvector (HNSW cosine similarity index)
  │
  ▼
User Query -> Embed Query -> HNSW Cosine Similarity Search
  │
  ▼
Assemble Bounded Context Bundle & Conversation History
  │
  ▼
OpenAI Prompt (System Instructions: strictly grounded, citations attached)
  │
  ▼
Assistant Message + Verified GitHub Line-Range Citations
```

---

## Local Development

### Prerequisites
- **Java 21** JDK installed
- **Node.js 20+** and **npm**
- **Docker Desktop** (for local PostgreSQL with pgvector)
- An **OpenAI API Key**
- A **GitHub OAuth Application**

### 1. Clone the Repository
```bash
git clone https://github.com/adityasinha513/DevPilot.git
cd DevPilot
```

### 2. Start Local PostgreSQL with pgvector
```bash
docker compose up -d postgres
```
This starts PostgreSQL on port `5433` with the `devpilot` database and pgvector extension pre-installed.

### 3. Configure Backend Environment
Copy the example environment file:
```bash
cp backend/.env.example backend/.env
```
Populate `backend/.env` with your values:
```properties
DATABASE_URL=jdbc:postgresql://localhost:5433/devpilot
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
OPENAI_API_KEY=your_openai_api_key
TOKEN_ENCRYPTION_PASSWORD=local-dev-secret-password-12345
TOKEN_ENCRYPTION_SALT=0123456789abcdef
APP_FRONTEND_URL=http://localhost:3000
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
```

### 4. Run the Backend
On Windows (PowerShell):
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
On Linux/macOS:
```bash
cd backend
./mvnw spring-boot:run
```
The backend starts on `http://localhost:8080`. Flyway automatically executes database migrations upon startup.

To run the test suite:
```powershell
.\mvnw.cmd test
```

### 5. Run the Frontend
In another terminal:
```bash
cd client
npm install
npm run dev
```
The frontend runs at `http://localhost:3000`.

---

## Database Setup & Flyway Migrations

DevPilot manages its database schema exclusively through **Flyway**.
In production, Hibernate DDL auto-generation is disabled (`spring.jpa.hibernate.ddl-auto=validate`) to ensure Flyway owns schema state and transitions.

The initial migration `V1__initial_schema.sql` creates:
- `vector` extension
- `users`: GitHub profile data, encrypted access tokens, timestamps
- `git_repositories`: Tracked repositories, ingestion counters, indexed commit SHA
- `repository_processing_jobs`: Ingestion run history, statuses, timestamps
- `repository_files`: Indexed file metadata, language, sizes, blob hashes
- `code_chunks`: Deterministic file chunks with start/end line bounds
- `code_chunk_embeddings`: 1536-dimensional vector column with an **HNSW index** using `vector_cosine_ops`
- `conversations` & `chat_messages`: Multi-turn chat history with citation metadata

---

## Docker Usage

Both services include multi-stage Dockerfiles optimized for production deployment:

### Build Backend Image
```bash
docker build -t devpilot-backend ./backend
```
Runs a multi-stage build using `maven:3.9-eclipse-temurin-21` and packages a minimal JRE 21 runtime container running under a dedicated non-root `devpilot` user on port `8080`.

### Build Frontend Image
```bash
docker build -t devpilot-client ./client
```
Runs a multi-stage Alpine build with Next.js Turbopack compilation and creates a minimal Node.js production runner on port `3000`.

---

## Render Deployment

DevPilot is pre-configured for automated deployment on **Render** using the provided [`render.yaml`](render.yaml) blueprint.

### Deployment Architecture on Render
1. **Database**: Render PostgreSQL database (`devpilot-db`) with `pgvector` extension support.
2. **Backend**: Render Web Service (`devpilot-api`) running Docker runtime from `backend/Dockerfile` with health checks at `/actuator/health`.
3. **Frontend**: Render Web Service (`devpilot-web`) running Docker runtime from `client/Dockerfile`.

### Step-by-Step Render Deployment

1. **Push to GitHub**: Ensure all code is committed and pushed to your repository.
2. **Create GitHub OAuth App**:
   - Application Name: `DevPilot`
   - Homepage URL: `https://<frontend-service-name>.onrender.com`
   - Authorization callback URL:
     ```text
     https://<backend-service-name>.onrender.com/login/oauth2/code/github
     ```
3. **Deploy via Render Blueprint**:
   - Log into the [Render Dashboard](https://dashboard.render.com).
   - Click **New +** > **Blueprint**.
   - Connect your `DevPilot` repository.
   - Render will parse `render.yaml` and configure:
     - `devpilot-db` (PostgreSQL)
     - `devpilot-api` (Backend Docker Web Service)
     - `devpilot-web` (Frontend Docker Web Service)
4. **Supply Environment Variables**:
   Render will prompt for missing secret environment variables:
   - `GITHUB_CLIENT_ID`: Your GitHub OAuth App Client ID
   - `GITHUB_CLIENT_SECRET`: Your GitHub OAuth App Client Secret
   - `OPENAI_API_KEY`: Your OpenAI API Key
   - `APP_FRONTEND_URL`: `https://<frontend-service-name>.onrender.com`
   - `APP_CORS_ALLOWED_ORIGINS`: `https://<frontend-service-name>.onrender.com`
   - `NEXT_PUBLIC_API_BASE_URL`: `https://<backend-service-name>.onrender.com`
5. **Apply & Deploy**:
   Click **Apply**. Render will automatically provision PostgreSQL, compile and launch the backend with Flyway migrations, and build the frontend container.

---

## Environment Variables Reference

| Variable | Required In | Description |
|---|---|---|
| `DATABASE_URL` | Both | Database connection URL. Accepts both `jdbc:postgresql://...` and cloud URI format `postgresql://user:pass@host:port/db`. Auto-wired via `render.yaml`. |
| `DATABASE_USERNAME` | Local | PostgreSQL username (optional if encoded in `DATABASE_URL`). |
| `DATABASE_PASSWORD` | Local | PostgreSQL password (optional if encoded in `DATABASE_URL`). |
| `GITHUB_CLIENT_ID` | Both | GitHub OAuth application client ID. |
| `GITHUB_CLIENT_SECRET` | Both | GitHub OAuth application client secret. |
| `OPENAI_API_KEY` | Both | OpenAI API key for embeddings and Q&A chat. |
| `OPENAI_CHAT_MODEL` | Optional | Chat model name (default: `gpt-4o-mini`). |
| `OPENAI_EMBEDDING_MODEL` | Optional | Embedding model name (default: `text-embedding-3-small`). |
| `TOKEN_ENCRYPTION_PASSWORD` | Both | Secret key used to encrypt stored GitHub access tokens (auto-generated in Render). |
| `TOKEN_ENCRYPTION_SALT` | Both | 16-character hex salt string (default: `0123456789abcdef`). |
| `APP_FRONTEND_URL` | Prod | Frontend domain used for post-login OAuth redirects (e.g. `https://devpilot-web.onrender.com`). |
| `APP_CORS_ALLOWED_ORIGINS` | Both | Comma-separated allowed CORS origins (e.g. `https://devpilot-web.onrender.com`). |
| `NEXT_PUBLIC_API_BASE_URL` | Frontend | Browser-accessible backend API base URL (e.g. `https://devpilot-api.onrender.com`). |
| `PORT` | Optional | Port for the backend service (default: `8080`). |

---

## Known V1 Limitations

- **File Chunking**: Employs heuristic sliding-window chunking by line count rather than AST-based parsing.
- **In-Process Ingestion**: Ingestion tasks execute asynchronously on a dedicated thread pool rather than a distributed broker (such as Kafka or RabbitMQ).
- **Single-Host Concurrency**: Suitable for single-instance or active-passive instances. Multi-instance horizontal scaling for ingestion would require a distributed task coordinator.
- **Repository Size Limit**: Default maximum file size threshold is 100 KB to avoid embedding large generated assets or bundle outputs.

---

## License

MIT License. See [LICENSE](LICENSE) for details.
