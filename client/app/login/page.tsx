"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AlertCircle } from "lucide-react";

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import { cn } from "@/lib/utils";
import { BrandMark } from "@/components/brand-mark";
import { GitHubIcon } from "@/components/icons/github";
import { ModeToggle } from "@/components/mode-toggle";
import { useAuth } from "@/hooks/use-auth";
import { getGithubLoginUrl } from "@/lib/api";

const OAUTH_ERROR_MESSAGES: Record<string, string> = {
  oauth_failed: "GitHub sign-in was cancelled or failed. Please try again.",
};

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user, isLoading } = useAuth();
  const code = searchParams.get("error");
  const error = code ? (OAUTH_ERROR_MESSAGES[code] ?? "Unable to sign in. Please try again.") : "";

  useEffect(() => {
    if (!isLoading && user) {
      router.replace("/dashboard");
    }
  }, [isLoading, user, router]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner className="size-8" />
      </div>
    );
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/40 px-4 py-12">
      <div className="absolute right-4 top-4">
        <ModeToggle />
      </div>
      <Card className="w-full max-w-md shadow-lg">
        <CardContent className="p-8">
          <div className="mb-8 flex flex-col items-center text-center">
            <BrandMark />
            <h1 className="mt-6 text-2xl font-semibold tracking-tight">Sign in to DevPilot</h1>
            <p className="mt-2 text-sm text-muted-foreground">
              Connect your GitHub account to analyze and chat with your repositories.
            </p>
          </div>

          {error ? (
            <Alert variant="destructive" className="mb-6">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Unable to sign in</AlertTitle>
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          ) : null}

          <a
            href={getGithubLoginUrl()}
            className={cn(buttonVariants({ variant: "default" }), "w-full")}
          >
            <GitHubIcon className="mr-2 h-4 w-4" />
            Continue with GitHub
          </a>
          <p className="mt-6 text-center text-xs text-muted-foreground">
            DevPilot uses GitHub OAuth. Your access token stays on the server and is never
            exposed to the browser.
          </p>
        </CardContent>
      </Card>
    </main>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}
