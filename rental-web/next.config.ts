import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // PWA 설정은 next-pwa 래퍼를 통해 적용
  // 현재는 기본 Next.js 설정으로 시작
};

export default nextConfig;
