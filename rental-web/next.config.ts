import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // PWA 설정은 next-pwa 래퍼를 통해 적용
  // 현재는 기본 Next.js 설정으로 시작
  turbopack: {
    // workspace root 명시 — monorepo lockfile 중복 경고 제거 (BUG-S2-005 빌드 경고)
    root: __dirname,
  },
};

export default nextConfig;
