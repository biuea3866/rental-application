"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from "@/components/ui/card";
import { useAuthStore } from "@/stores/auth-store";

export default function HomePage() {
  const router = useRouter();
  const { user, isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (isAuthenticated && user) {
      if (user.role === "LENDER") {
        router.replace("/lender");
      } else if (user.role === "RENTER") {
        router.replace("/products");
      }
    }
  }, [isAuthenticated, user, router]);

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-8 p-6">
      <div className="text-center">
        <h1 className="text-4xl font-bold tracking-tight">
          Rental Commerce
        </h1>
        <p className="mt-2 text-lg text-muted-foreground">
          물건을 빌려주고 빌리는 대여 커머스 플랫폼
        </p>
      </div>

      <div className="grid gap-6 sm:grid-cols-2 max-w-2xl w-full">
        <Card>
          <CardHeader>
            <CardTitle>등록자 (Lender)</CardTitle>
            <CardDescription>
              사용하지 않는 물건을 등록하고 수익을 올려보세요.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Link href="/login">
              <Button className="w-full">등록자로 시작하기</Button>
            </Link>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>대여자 (Renter)</CardTitle>
            <CardDescription>
              필요한 물건을 합리적인 가격에 빌려보세요.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Link href="/login">
              <Button variant="secondary" className="w-full">
                대여자로 시작하기
              </Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
