import * as React from "react";

import { cn } from "@/lib/utils";

export type IconProps = React.SVGProps<SVGSVGElement>;

export function LanguageIcon({ className, ...props }: IconProps) {
	return (
		<svg
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			strokeWidth="1.8"
			strokeLinecap="round"
			strokeLinejoin="round"
			aria-hidden="true"
			className={cn("h-4 w-4", className)}
			{...props}
		>
			<path d="M8.5 7 4 12l4.5 5" />
			<path d="M15.5 7 20 12l-4.5 5" />
			<path d="M13.5 4.5 10.5 19.5" />
		</svg>
	);
}

export default LanguageIcon;
