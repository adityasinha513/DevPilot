"use client";

import { FolderGit2, LogOut } from "lucide-react";
import Link from "next/link";

import { BrandMark } from "@/components/brand-mark";
import { ModeToggle } from "@/components/mode-toggle";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import { cn } from "@/lib/utils";

type AppShellProps = {
  children: React.ReactNode;
  className?: string;
};

export function AppShell({ children, className }: AppShellProps) {
  const { user, logout, isLoggingOut } = useAuth();

  const initials =
    user?.displayName
      ?.split(/\s+/)
      .map((part) => part[0])
      .join("")
      .slice(0, 2)
      .toUpperCase() ??
    user?.githubUsername.slice(0, 2).toUpperCase() ??
    "?";

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="sticky top-0 z-40 border-b bg-background/90 backdrop-blur supports-[backdrop-filter]:bg-background/75">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between gap-4 px-4 sm:px-6">
          <div className="flex items-center gap-7"><BrandMark /><nav className="hidden items-center gap-1 sm:flex"><Link href="/dashboard" className="focus-ring inline-flex items-center gap-2 rounded-md px-2.5 py-1.5 text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"><FolderGit2 className="size-4"/>Repositories</Link></nav></div>
          <div className="flex items-center gap-2">
            <ModeToggle />
            {user ? (
              <>
                <div className="hidden items-center gap-2 sm:flex">
                  <Avatar size="sm">
                    {user.avatarUrl ? (
                      <AvatarImage src={user.avatarUrl} alt={user.displayName} />
                    ) : null}
                    <AvatarFallback>{initials}</AvatarFallback>
                  </Avatar>
                  <div className="text-right text-xs leading-tight">
                    <p className="font-medium">{user.displayName}</p>
                    <p className="text-muted-foreground">@{user.githubUsername}</p>
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => logout()}
                  disabled={isLoggingOut}
                >
                  <LogOut className="mr-1.5 size-3.5" />
                  Sign out
                </Button>
              </>
            ) : null}
          </div>
        </div>
      </header>
      <main className={cn("mx-auto w-full max-w-7xl flex-1 px-4 py-8 sm:px-6", className)}>
        {children}
      </main>
    </div>
  );
}

export default AppShell;
