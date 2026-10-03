"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Send, Sparkles } from "lucide-react";
import { RequireAuth } from "@/components/auth/require-auth";
import { AppShell } from "@/components/layout/app-shell";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import { api, ApiError } from "@/lib/api";

function RepositoryChat() {
  const { repositoryId } = useParams<{ repositoryId: string }>();
  const qc = useQueryClient(); const [conversationId, setConversationId] = useState<string>(); const [question, setQuestion] = useState("");
  const repo = useQuery({ queryKey: ["repository", repositoryId], queryFn: () => apiFetchRepo(repositoryId) });
  const conversations = useQuery({ queryKey: ["conversations", repositoryId], queryFn: () => api.listConversations(repositoryId) });
  const activeConversationId = conversationId ?? conversations.data?.[0]?.id;
  const messages = useQuery({ queryKey: ["messages", activeConversationId], queryFn: () => api.listMessages(activeConversationId!), enabled: !!activeConversationId });
  const create = useMutation({ mutationFn: () => api.createConversation(repositoryId), onSuccess: (c) => { setConversationId(c.id); qc.invalidateQueries({ queryKey: ["conversations", repositoryId] }); } });
  const send = useMutation({ mutationFn: () => api.sendMessage(activeConversationId!, question), onSuccess: () => { setQuestion(""); qc.invalidateQueries({ queryKey: ["messages", activeConversationId] }); } });
  function submit(e: FormEvent) { e.preventDefault(); if (question.trim() && activeConversationId) send.mutate(); }
  if (repo.isLoading || conversations.isLoading) return <div className="flex justify-center py-20"><Spinner /></div>;
  if (repo.isError || !repo.data) return <Alert variant="destructive"><AlertDescription>Repository was not found.</AlertDescription></Alert>;
  return <div className="space-y-5"><Link className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground" href="/dashboard"><ArrowLeft className="size-4"/> Dashboard</Link><div className="flex items-start justify-between"><div><h1 className="text-2xl font-semibold">{repo.data.fullName}</h1><p className="text-sm text-muted-foreground">Ask questions grounded in the indexed repository.</p></div><Button variant="outline" onClick={() => create.mutate()} disabled={create.isPending}>New conversation</Button></div><div className="grid gap-5 lg:grid-cols-[220px_1fr]"><Card><CardHeader><CardTitle className="text-sm">Conversations</CardTitle></CardHeader><CardContent className="space-y-1">{conversations.data?.map(c=><Button key={c.id} variant={activeConversationId===c.id?"secondary":"ghost"} className="w-full justify-start truncate" onClick={()=>setConversationId(c.id)}>{c.title}</Button>)}</CardContent></Card><Card className="min-h-[560px]"><CardContent className="flex min-h-[560px] flex-col p-5"><div className="flex-1 space-y-5 overflow-auto">{messages.data?.map(m=><div key={m.id} className={m.role==="USER"?"ml-auto max-w-[85%] rounded-lg bg-primary p-3 text-primary-foreground":"max-w-[90%] rounded-lg bg-muted p-3"}><p className="whitespace-pre-wrap text-sm">{m.content}</p>{m.citations.length>0?<div className="mt-3 border-t pt-2 text-xs"><p className="mb-1 font-medium">Sources</p>{m.citations.map(c=><a key={c.chunkId} href={c.url??"#"} target="_blank" className="block text-primary hover:underline">{c.path}:{c.startLine}-{c.endLine}</a>)}</div>:null}</div>)}{send.isPending?<div className="flex items-center gap-2 text-sm text-muted-foreground"><Spinner className="size-4"/> Searching repository and composing answer…</div>:null}{!activeConversationId?<div className="py-24 text-center text-muted-foreground"><Sparkles className="mx-auto mb-3 size-8"/>Create a conversation to begin.</div>:null}</div><form onSubmit={submit} className="mt-4 flex gap-2"><Input value={question} onChange={e=>setQuestion(e.target.value)} placeholder="How does authentication work?" disabled={!activeConversationId||send.isPending}/><Button type="submit" size="icon" disabled={!question.trim()||!activeConversationId||send.isPending}><Send className="size-4"/></Button></form>{send.isError?<p className="mt-2 text-sm text-destructive">{send.error instanceof ApiError?send.error.message:"Could not send message."}</p>:null}</CardContent></Card></div></div>;
}
async function apiFetchRepo(id: string) { return apiFetchStored(id); }
import { apiFetch, type StoredRepository } from "@/lib/api";
function apiFetchStored(id: string) { return apiFetch<StoredRepository>(`/api/repositories/${id}`); }
export default function Page(){return <RequireAuth><AppShell><RepositoryChat/></AppShell></RequireAuth>}
