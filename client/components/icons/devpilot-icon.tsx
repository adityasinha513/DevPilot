import * as React from "react";

import { cn } from "@/lib/utils";

export type IconProps = React.SVGProps<SVGSVGElement>;

export function DevPilotIcon({ className, ...props }: IconProps) {
	return (
		<svg
			viewBox="0 0 64 64"
			fill="none"
			aria-hidden="true"
			className={cn("h-8 w-8", className)}
			{...props}
		>
			<defs>
				<linearGradient id="devpilot-gradient" x1="12" y1="12" x2="52" y2="52" gradientUnits="userSpaceOnUse">
					<stop stopColor="#7C3AED" />
					<stop offset="0.5" stopColor="#2563EB" />
					<stop offset="1" stopColor="#14B8A6" />
				</linearGradient>
			</defs>
			<rect x="8" y="8" width="48" height="48" rx="14" fill="url(#devpilot-gradient)" />
			<path d="M22 18h9.5c7.9 0 14.5 6.2 14.5 14s-6.6 14-14.5 14H22V18Zm8 8v12h1.5c4 0 7-3.1 7-6s-3-6-7-6H30Z" fill="white" />
			<path d="M47 20.5c2.2 0 4 1.8 4 4v15c0 2.2-1.8 4-4 4h-1.5v-7.5h1.5c1.1 0 2-.9 2-2v-1.5c0-1.1-.9-2-2-2h-1.5V24.5c0-2.2 1.8-4 4-4Z" fill="white" opacity="0.92" />
			<circle cx="45.5" cy="18.5" r="3.5" fill="#F8FAFC" opacity="0.9" />
		</svg>
	);
}

export default DevPilotIcon;
