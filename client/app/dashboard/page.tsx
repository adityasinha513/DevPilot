"use client";

import { RequireAuth } from "@/components/auth/require-auth";
import { AppShell } from "@/components/layout/app-shell";
import { RepositoryPanel } from "@/components/repositories/repository-panel";
import { useAuth } from "@/hooks/use-auth";

function DashboardContent() {
  const { user } = useAuth();

  return (
    <AppShell>
      <div className="space-y-8">
        <div className="border-b pb-7">
          <p className="text-xs font-medium uppercase tracking-[0.18em] text-muted-foreground">Repository workspace</p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight">Welcome back{user ? `, ${user.displayName.split(" ")[0]}` : ""}.</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">Connect a GitHub repository, let DevPilot build its code map, then ask grounded questions with source-level citations.</p>
        </div>

        <RepositoryPanel />
      </div>
    </AppShell>
  );
}

export default function DashboardPage() {
  return (
    <RequireAuth>
      <DashboardContent />
    </RequireAuth>
  );
}
