export type User = {
    id: string;
    githubUsername: string;
    displayName: string;
    avatarUrl: string | null;
};

export class ApiError extends Error {
    status: number;

    constructor(status: number, message: string) {
        super(message);
        this.status = status;
    }
}

export function getApiBaseUrl(){
    const url = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
    return url.replace(/\/+$/, "");
}

export function getGithubLoginUrl() {
    return `${getApiBaseUrl()}/oauth2/authorization/github`;
}

let csrfToken: Promise<{ headerName: string; token: string }> | null = null;

async function getCsrfToken() {
    csrfToken ??= fetch(`${getApiBaseUrl()}/api/auth/csrf`, { credentials: "include" })
        .then(async (response) => {
            if (!response.ok) throw new ApiError(response.status, await parseError(response));
            return response.json() as Promise<{ headerName: string; token: string }>;
        });
    try { return await csrfToken; } catch (error) { csrfToken = null; throw error; }
}

async function parseError(res: Response): Promise<string> {
    try{
        const data = await res.json();
        return data.message ?? data.error ?? res.statusText;
    }catch{
        return res.statusText || "Request failed";
    }
}

export async function apiFetch<T>(
    path: string,
    options: RequestInit = {},
): Promise<T> {
    const headers = new Headers(options.headers);

    if (!["GET", "HEAD", "OPTIONS"].includes((options.method ?? "GET").toUpperCase())) {
        const csrf = await getCsrfToken();
        headers.set(csrf.headerName, csrf.token);
    }

    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`${getApiBaseUrl()}${path}`, {
        ...options,
        credentials: "include",
        headers,
    });

    if (!response.ok) {
        throw new ApiError(response.status, await parseError(response));
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return response.json() as Promise<T>;
}

export type ProcessingStatus =
    | "NOT_STARTED"
    | "QUEUED"
    | "INGESTING" | "CHUNKING" | "EMBEDDING" | "READY"
    | "FAILED_INGESTION" | "FAILED_CHUNKING" | "FAILED_EMBEDDING";

export type Citation = { chunkId: string; path: string; startLine: number; endLine: number; url: string | null };
export type ChatMessage = { id: string; role: "USER" | "ASSISTANT"; content: string; citations: Citation[]; createdAt: string };
export type Conversation = { id: string; repositoryId: string; title: string; createdAt: string; updatedAt: string };

export type StoredRepository = {
    id: string;
    githubRepoId: number | null;
    owner: string;
    name: string;
    fullName: string;
    defaultBranch: string | null;
    isPrivate: boolean;
    htmlUrl: string | null;
    description: string | null;
    processingStatus: ProcessingStatus;
    processingError: string | null;
    indexedCommitSha: string | null;
    indexedBranch: string | null;
    filesDiscovered: number;
    filesProcessed: number;
    filesSkipped: number;
    filesFailed: number;
    progressPercent: number;
    createdAt: string;
    updatedAt: string;
};

export type GitHubRepositorySummary = {
    githubRepoId: number;
    owner: string;
    name: string;
    fullName: string;
    defaultBranch: string | null;
    isPrivate: boolean;
    htmlUrl: string | null;
    description: string | null;
};

import {
    isDemoMode,
    disableDemoMode,
    DEMO_USER,
    DEMO_REPOSITORIES,
    DEMO_GITHUB_CATALOG,
    DEMO_CONVERSATIONS,
    DEMO_MESSAGES,
} from "./demo";

export const api = {
    me: () => (isDemoMode() ? Promise.resolve(DEMO_USER) : apiFetch<User>("/api/auth/me")),
    logout: () => {
        if (isDemoMode()) {
            disableDemoMode();
            return Promise.resolve();
        }
        return apiFetch<void>("/api/auth/logout", { method: "POST" });
    },
    listStoredRepositories: () =>
        isDemoMode() ? Promise.resolve(DEMO_REPOSITORIES) : apiFetch<StoredRepository[]>("/api/repositories"),
    listGitHubCatalog: (page = 1, perPage = 30) =>
        isDemoMode()
            ? Promise.resolve(DEMO_GITHUB_CATALOG)
            : apiFetch<GitHubRepositorySummary[]>(
                  `/api/repositories/catalog?page=${page}&perPage=${perPage}`,
              ),
    connectRepository: (reference: string) =>
        isDemoMode()
            ? Promise.resolve(DEMO_REPOSITORIES[0])
            : apiFetch<StoredRepository>("/api/repositories", {
                  method: "POST",
                  body: JSON.stringify({ reference }),
              }),
    retryRepositoryIngestion: (repositoryId: string) =>
        isDemoMode()
            ? Promise.resolve(DEMO_REPOSITORIES[0])
            : apiFetch<StoredRepository>(`/api/repositories/${repositoryId}/retry-ingestion`, {
                  method: "POST",
              }),
    listConversations: (repositoryId: string) =>
        isDemoMode()
            ? Promise.resolve(DEMO_CONVERSATIONS)
            : apiFetch<Conversation[]>(`/api/repositories/${repositoryId}/conversations`),
    createConversation: (repositoryId: string, title = "New conversation") =>
        isDemoMode()
            ? Promise.resolve({
                  id: `demo-conv-${Date.now()}`,
                  repositoryId,
                  title,
                  createdAt: new Date().toISOString(),
                  updatedAt: new Date().toISOString(),
              })
            : apiFetch<Conversation>(`/api/repositories/${repositoryId}/conversations`, {
                  method: "POST",
                  body: JSON.stringify({ title }),
              }),
    listMessages: (conversationId: string) =>
        isDemoMode()
            ? Promise.resolve(DEMO_MESSAGES[conversationId] ?? DEMO_MESSAGES["demo-conv-1"])
            : apiFetch<ChatMessage[]>(`/api/conversations/${conversationId}/messages`),
    sendMessage: (conversationId: string, content: string) => {
        if (isDemoMode()) {
            const userMsg: ChatMessage = {
                id: `demo-u-${Date.now()}`,
                role: "USER",
                content,
                citations: [],
                createdAt: new Date().toISOString(),
            };
            const assistantMsg: ChatMessage = {
                id: `demo-a-${Date.now()}`,
                role: "ASSISTANT",
                content: `[Demo Mode Response] You asked: "${content}"\n\nIn this static demo environment, natural language answers and pgvector similarity queries are simulated from curated repository code snippets. When deployed with the live Spring Boot backend, DevPilot vectorizes your questions with OpenAI \`text-embedding-3-small\`, executes an HNSW cosine search, and provides live answers with verified GitHub line-range citations.`,
                citations: [
                    {
                        chunkId: "demo-chunk-highlight",
                        path: "backend/src/main/java/devPilot/backend/services/RepositoryChatService.java",
                        startLine: 42,
                        endLine: 89,
                        url: "https://github.com/adityasinha513/DevPilot/blob/main/backend/src/main/java/devPilot/backend/services/RepositoryChatService.java#L42-L89",
                    },
                ],
                createdAt: new Date().toISOString(),
            };
            return Promise.resolve({ userMessage: userMsg, assistantMessage: assistantMsg });
        }
        return apiFetch<{ userMessage: ChatMessage; assistantMessage: ChatMessage }>(
            `/api/conversations/${conversationId}/messages`,
            { method: "POST", body: JSON.stringify({ content }) },
        );
    },
};
