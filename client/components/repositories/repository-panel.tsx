"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, Link2, Loader2, Lock, RefreshCw } from "lucide-react";
import { useState } from "react";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import { ConnectedRepositoryCard } from "@/components/repositories/connected-repository-card";
import { ApiError, api, type StoredRepository } from "@/lib/api";

const storedReposKey = ["repositories", "stored"] as const;
const catalogKey = ["repositories", "catalog"] as const;

function hasActiveIngestion(repositories: StoredRepository[]) {
  return repositories.some(
    (repo) => ["QUEUED", "INGESTING", "CHUNKING", "EMBEDDING"].includes(repo.processingStatus),
  );
}

export function RepositoryPanel() {
  const queryClient = useQueryClient();
  const [reference, setReference] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const storedQuery = useQuery({
    queryKey: storedReposKey,
    queryFn: () => api.listStoredRepositories(),
    refetchInterval: (query) =>
      hasActiveIngestion(query.state.data ?? []) ? 2000 : false,
  });

  const catalogQuery = useQuery({
    queryKey: catalogKey,
    queryFn: () => api.listGitHubCatalog(1, 30),
  });

  const connectMutation = useMutation({
    mutationFn: (value: string) => api.connectRepository(value),
    onSuccess: async () => {
      setReference("");
      setFormError(null);
      await queryClient.invalidateQueries({ queryKey: storedReposKey });
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        setFormError(error.message);
      } else {
        setFormError("Could not connect repository.");
      }
    },
  });

  function connectFromCatalog(fullName: string) {
    setFormError(null);
    connectMutation.mutate(fullName);
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);
    if (!reference.trim()) {
      setFormError("Enter owner/name or a GitHub URL.");
      return;
    }
    connectMutation.mutate(reference.trim());
  }

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Add repository</CardTitle>
          <CardDescription>
            Paste a URL or <span className="font-mono">owner/name</span>. DevPilot validates access
            with your GitHub token on the server.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form onSubmit={handleSubmit} className="flex flex-col gap-2 sm:flex-row">
            <Input
              value={reference}
              onChange={(event) => setReference(event.target.value)}
              placeholder="https://github.com/owner/repo or owner/repo"
              disabled={connectMutation.isPending}
            />
            <Button type="submit" disabled={connectMutation.isPending}>
              {connectMutation.isPending ? (
                <Loader2 className="mr-2 size-4 animate-spin" />
              ) : (
                <Link2 className="mr-2 size-4" />
              )}
              Connect
            </Button>
          </form>
          {formError ? (
            <Alert variant="destructive">
              <AlertCircle className="size-4" />
              <AlertTitle>Could not connect</AlertTitle>
              <AlertDescription>{formError}</AlertDescription>
            </Alert>
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-start justify-between gap-2">
          <div>
            <CardTitle className="text-lg">Your GitHub repositories</CardTitle>
            <CardDescription>Recently updated repositories you can access.</CardDescription>
          </div>
          <Button
            variant="outline"
            size="icon-sm"
            onClick={() => catalogQuery.refetch()}
            disabled={catalogQuery.isFetching}
            aria-label="Refresh catalog"
          >
            <RefreshCw className={catalogQuery.isFetching ? "size-4 animate-spin" : "size-4"} />
          </Button>
        </CardHeader>
        <CardContent>
          {catalogQuery.isLoading ? (
            <div className="flex justify-center py-8">
              <Spinner className="size-6" />
            </div>
          ) : catalogQuery.isError ? (
            <Alert variant="destructive">
              <AlertTitle>Could not load GitHub repositories</AlertTitle>
              <AlertDescription>
                {catalogQuery.error instanceof ApiError
                  ? catalogQuery.error.message
                  : "Try again in a moment."}
              </AlertDescription>
            </Alert>
          ) : (
            <ul className="max-h-80 space-y-2 overflow-y-auto pr-1">
              {(catalogQuery.data ?? []).map((repo) => (
                <li
                  key={repo.githubRepoId}
                  className="flex items-start justify-between gap-3 rounded-lg border p-3"
                >
                  <div className="min-w-0">
                    <p className="truncate font-medium">{repo.fullName}</p>
                    {repo.description ? (
                      <p className="line-clamp-2 text-xs text-muted-foreground">{repo.description}</p>
                    ) : null}
                    <div className="mt-1 flex flex-wrap gap-1">
                      {repo.isPrivate ? (
                        <Badge variant="secondary" className="text-[10px]">
                          <Lock className="mr-1 size-3" />
                          Private
                        </Badge>
                      ) : (
                        <Badge variant="outline" className="text-[10px]">
                          Public
                        </Badge>
                      )}
                    </div>
                  </div>
                  <Button
                    size="sm"
                    variant="secondary"
                    disabled={connectMutation.isPending}
                    onClick={() => connectFromCatalog(repo.fullName)}
                  >
                    Connect
                  </Button>
                </li>
              ))}
              {(catalogQuery.data ?? []).length === 0 ? (
                <p className="py-4 text-center text-sm text-muted-foreground">
                  No repositories returned from GitHub.
                </p>
              ) : null}
            </ul>
          )}
        </CardContent>
      </Card>

      <Card className="lg:col-span-2">
        <CardHeader>
          <CardTitle className="text-lg">Connected repositories</CardTitle>
          <CardDescription>
            Repositories registered with DevPilot. Analysis runs automatically after you connect.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {storedQuery.isLoading ? (
            <div className="flex justify-center py-6">
              <Spinner className="size-6" />
            </div>
          ) : (
            <ul className="space-y-3">
              {(storedQuery.data ?? []).map((repo) => (
                <ConnectedRepositoryCard key={repo.id} repository={repo} />
              ))}
              {(storedQuery.data ?? []).length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  No repositories connected yet. Add one above to get started.
                </p>
              ) : null}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
