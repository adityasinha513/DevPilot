"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";

import { api } from "@/lib/api";
import { authQueryKey, fetchCurrentUser } from "@/lib/auth";

export function useAuth() {
  const queryClient = useQueryClient();
  const router = useRouter();

  const session = useQuery({
    queryKey: authQueryKey,
    queryFn: fetchCurrentUser,
    staleTime: 60_000,
    retry: false,
  });

  const logoutMutation = useMutation({
    mutationFn: () => api.logout(),
    onSettled: async () => {
      queryClient.setQueryData(authQueryKey, null);
      await queryClient.invalidateQueries({ queryKey: authQueryKey });
      router.replace("/login");
    },
  });

  return {
    user: session.data ?? null,
    isLoading: session.isLoading,
    isAuthenticated: session.data != null,
    error: session.error,
    refetch: session.refetch,
    logout: logoutMutation.mutate,
    isLoggingOut: logoutMutation.isPending,
  };
}
