import { DevPilotIcon } from "@/components/icons/devpilot-icon";
import { cn } from "@/lib/utils";

export function BrandMark({ className }: { className?: string }) {
  return (
    <div className={cn("flex items-center justify-center gap-2", className)}>
      <DevPilotIcon className="h-6 w-6" />
      <span className="text-base font-semibold tracking-tight text-foreground">DevPilot</span>
    </div>
  );
}

export default BrandMark;
