import type { NextConfig } from "next";

const isExport = process.env.NEXT_PUBLIC_EXPORT === "true";
const basePath = process.env.NEXT_PUBLIC_BASE_PATH || "";

const nextConfig: NextConfig = {
  output: isExport ? "export" : undefined,
  basePath: basePath ? basePath : undefined,
  images: {
    unoptimized: true,
  },
};

export default nextConfig;
