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
    return process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
}

export function getGithubLoginUrl() {
    return `${getApiBaseUrl()}/oauth2/authorization/github`;
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

export const api = {
    me: () => apiFetch<User>("/api/auth/me"),
    logout: () => apiFetch<void>("/api/auth/logout", { method: "POST" }),
    listStoredRepositories: () => apiFetch<StoredRepository[]>("/api/repositories"),
    listGitHubCatalog: (page = 1, perPage = 30) =>
        apiFetch<GitHubRepositorySummary[]>(
            `/api/repositories/catalog?page=${page}&perPage=${perPage}`,
        ),
    connectRepository: (reference: string) =>
        apiFetch<StoredRepository>("/api/repositories", {
            method: "POST",
            body: JSON.stringify({ reference }),
        }),
    retryRepositoryIngestion: (repositoryId: string) =>
        apiFetch<StoredRepository>(`/api/repositories/${repositoryId}/retry-ingestion`, {
            method: "POST",
        }),
    listConversations: (repositoryId: string) => apiFetch<Conversation[]>(`/api/repositories/${repositoryId}/conversations`),
    createConversation: (repositoryId: string, title = "New conversation") => apiFetch<Conversation>(`/api/repositories/${repositoryId}/conversations`, { method: "POST", body: JSON.stringify({ title }) }),
    listMessages: (conversationId: string) => apiFetch<ChatMessage[]>(`/api/conversations/${conversationId}/messages`),
    sendMessage: (conversationId: string, content: string) => apiFetch<{ userMessage: ChatMessage; assistantMessage: ChatMessage }>(`/api/conversations/${conversationId}/messages`, { method: "POST", body: JSON.stringify({ content }) }),
};
