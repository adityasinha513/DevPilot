"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

import { Spinner } from "@/components/ui/spinner";
import { useAuth } from "@/hooks/use-auth";

export default function Home() {
  const router = useRouter();
  const { user, isLoading } = useAuth();

  useEffect(() => {
    if (isLoading) return;
    router.replace(user ? "/dashboard" : "/login");
  }, [isLoading, user, router]);

  return (
    <div className="flex min-h-screen items-center justify-center">
      <Spinner className="size-8" />
    </div>
  );
}
