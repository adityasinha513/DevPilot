import RepositoryChatPage from "@/components/repositories/repository-chat-view";

export function generateStaticParams() {
  return [
    { repositoryId: "demo-devpilot" },
    { repositoryId: "sample-petclinic" },
  ];
}

export default function Page() {
  return <RepositoryChatPage />;
}
