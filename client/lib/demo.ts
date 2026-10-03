import type {
  ChatMessage,
  Conversation,
  GitHubRepositorySummary,
  StoredRepository,
  User,
} from "./api";

const DEMO_STORAGE_KEY = "devpilot_demo_mode";

export function isDemoMode(): boolean {
  if (typeof window === "undefined") {
    return process.env.NEXT_PUBLIC_DEMO_MODE === "true";
  }
  return (
    process.env.NEXT_PUBLIC_DEMO_MODE === "true" ||
    window.localStorage.getItem(DEMO_STORAGE_KEY) === "true"
  );
}

export function enableDemoMode(): void {
  if (typeof window !== "undefined") {
    window.localStorage.setItem(DEMO_STORAGE_KEY, "true");
  }
}

export function disableDemoMode(): void {
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(DEMO_STORAGE_KEY);
  }
}

export const DEMO_USER: User = {
  id: "demo-user-devpilot",
  githubUsername: "adityasinha513",
  displayName: "Aditya Sinha (Demo Mode)",
  avatarUrl: "https://avatars.githubusercontent.com/u/74384261?v=4",
};

export const DEMO_REPOSITORIES: StoredRepository[] = [
  {
    id: "demo-devpilot",
    githubRepoId: 987654321,
    owner: "adityasinha513",
    name: "DevPilot",
    fullName: "adityasinha513/DevPilot",
    defaultBranch: "main",
    isPrivate: false,
    htmlUrl: "https://github.com/adityasinha513/DevPilot",
    description: "AI-powered repository assistant using Spring Boot, pgvector, and Next.js",
    processingStatus: "READY",
    processingError: null,
    indexedCommitSha: "bf23813",
    indexedBranch: "main",
    filesDiscovered: 48,
    filesProcessed: 48,
    filesSkipped: 3,
    filesFailed: 0,
    progressPercent: 100,
    createdAt: "2026-10-01T10:00:00Z",
    updatedAt: "2026-10-03T12:00:00Z",
  },
  {
    id: "sample-petclinic",
    githubRepoId: 123456789,
    owner: "spring-projects",
    name: "spring-petclinic",
    fullName: "spring-projects/spring-petclinic",
    defaultBranch: "main",
    isPrivate: false,
    htmlUrl: "https://github.com/spring-projects/spring-petclinic",
    description: "Sample Spring Boot application demonstrating Spring Data, MVC, and Actuator",
    processingStatus: "READY",
    processingError: null,
    indexedCommitSha: "7e4f9b2",
    indexedBranch: "main",
    filesDiscovered: 36,
    filesProcessed: 36,
    filesSkipped: 2,
    filesFailed: 0,
    progressPercent: 100,
    createdAt: "2026-09-28T08:00:00Z",
    updatedAt: "2026-09-28T08:15:00Z",
  },
];

export const DEMO_GITHUB_CATALOG: GitHubRepositorySummary[] = [
  {
    githubRepoId: 987654321,
    owner: "adityasinha513",
    name: "DevPilot",
    fullName: "adityasinha513/DevPilot",
    defaultBranch: "main",
    isPrivate: false,
    htmlUrl: "https://github.com/adityasinha513/DevPilot",
    description: "AI-powered repository assistant using Spring Boot, pgvector, and Next.js",
  },
  {
    githubRepoId: 123456789,
    owner: "spring-projects",
    name: "spring-petclinic",
    fullName: "spring-projects/spring-petclinic",
    defaultBranch: "main",
    isPrivate: false,
    htmlUrl: "https://github.com/spring-projects/spring-petclinic",
    description: "Sample Spring Boot application demonstrating Spring Data, MVC, and Actuator",
  },
  {
    githubRepoId: 555666777,
    owner: "adityasinha513",
    name: "microservices-toolkit",
    fullName: "adityasinha513/microservices-toolkit",
    defaultBranch: "main",
    isPrivate: false,
    htmlUrl: "https://github.com/adityasinha513/microservices-toolkit",
    description: "Cloud-native microservices starter kit with Docker and Kubernetes configurations",
  },
];

export const DEMO_CONVERSATIONS: Conversation[] = [
  {
    id: "demo-conv-1",
    repositoryId: "demo-devpilot",
    title: "RAG Retrieval & Vector Indexing",
    createdAt: "2026-10-03T12:30:00Z",
    updatedAt: "2026-10-03T12:35:00Z",
  },
  {
    id: "demo-conv-2",
    repositoryId: "demo-devpilot",
    title: "Session Authentication & CSRF",
    createdAt: "2026-10-03T13:00:00Z",
    updatedAt: "2026-10-03T13:05:00Z",
  },
];

export const DEMO_MESSAGES: Record<string, ChatMessage[]> = {
  "demo-conv-1": [
    {
      id: "demo-msg-1",
      role: "USER",
      content: "How does DevPilot retrieve relevant code chunks and ground its answers?",
      citations: [],
      createdAt: "2026-10-03T12:30:00Z",
    },
    {
      id: "demo-msg-2",
      role: "ASSISTANT",
      content:
        "DevPilot executes a multi-stage semantic retrieval and grounded generation pipeline:\n\n1. **Query Embedding**: The incoming query is vectorized using OpenAI's `text-embedding-3-small` (1536 dimensions) via `EmbeddingService` with retry and rate-limit backoff.\n2. **HNSW Cosine Similarity Search**: Spring Data queries the `chunk_embeddings` table using pgvector's HNSW index (`USING hnsw (embedding vector_cosine_ops)`) to retrieve the top-K semantically closest chunks.\n3. **Context Assembly**: The matched chunks are bounded by maximum token limits and assembled with their respective file paths and line ranges.\n4. **Grounded Generation**: `RepositoryChatService` instructs GPT-4o-mini to synthesize an answer derived strictly from the retrieved code context and generate verifiable source citations.",
      citations: [
        {
          chunkId: "chunk-101",
          path: "backend/src/main/java/devPilot/backend/services/RepositoryChatService.java",
          startLine: 42,
          endLine: 89,
          url: "https://github.com/adityasinha513/DevPilot/blob/main/backend/src/main/java/devPilot/backend/services/RepositoryChatService.java#L42-L89",
        },
        {
          chunkId: "chunk-102",
          path: "backend/src/main/resources/db/migration/V1__initial_schema.sql",
          startLine: 40,
          endLine: 44,
          url: "https://github.com/adityasinha513/DevPilot/blob/main/backend/src/main/resources/db/migration/V1__initial_schema.sql#L40-L44",
        },
      ],
      createdAt: "2026-10-03T12:30:05Z",
    },
  ],
  "demo-conv-2": [
    {
      id: "demo-msg-3",
      role: "USER",
      content: "How are GitHub OAuth access tokens stored and protected?",
      citations: [],
      createdAt: "2026-10-03T13:00:00Z",
    },
    {
      id: "demo-msg-4",
      role: "ASSISTANT",
      content:
        "DevPilot secures GitHub OAuth access tokens through defense-in-depth:\n\n1. **AES-256 Symmetric Encryption**: Upon OAuth callback, `TokenEncryptionService` encrypts raw GitHub tokens before storing them in the `users` table.\n2. **Server-Side Session Cookies**: The access token is never sent to the browser. The client receives an encrypted, HttpOnly session cookie (`DEVPILOT_SESSION`) with `SameSite=None; Secure` in production.\n3. **CSRF Protection**: State-changing endpoints require an `X-CSRF-TOKEN` header obtained via the `/api/auth/csrf` endpoint.",
      citations: [
        {
          chunkId: "chunk-201",
          path: "backend/src/main/java/devPilot/backend/config/SecurityConfig.java",
          startLine: 35,
          endLine: 82,
          url: "https://github.com/adityasinha513/DevPilot/blob/main/backend/src/main/java/devPilot/backend/config/SecurityConfig.java#L35-L82",
        },
        {
          chunkId: "chunk-202",
          path: "backend/src/main/resources/application-prod.properties",
          startLine: 5,
          endLine: 8,
          url: "https://github.com/adityasinha513/DevPilot/blob/main/backend/src/main/resources/application-prod.properties#L5-L8",
        },
      ],
      createdAt: "2026-10-03T13:00:04Z",
    },
  ],
};
