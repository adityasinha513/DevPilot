"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";

import { Spinner } from "@/components/ui/spinner";
import { authQueryKey, fetchCurrentUser } from "@/lib/auth";

export default function AuthCallbackPage() {
  const router = useRouter();
  const queryClient = useQueryClient();

  useEffect(() => {
    let cancelled = false;

    async function completeSignIn() {
      const user = await fetchCurrentUser();
      if (cancelled) return;

      if (user) {
        queryClient.setQueryData(authQueryKey, user);
        router.replace("/dashboard");
      } else {
        router.replace("/login?error=oauth_failed");
      }
    }

    void completeSignIn();

    return () => {
      cancelled = true;
    };
  }, [queryClient, router]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3">
      <Spinner className="size-8" />
      <p className="text-sm text-muted-foreground">Completing GitHub sign-in…</p>
    </div>
  );
}
