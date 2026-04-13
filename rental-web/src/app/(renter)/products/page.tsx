import type { Metadata } from "next";
import { ProductsPage } from "./_products-page";

export const metadata: Metadata = {
  title: "상품 검색 | Rental Commerce",
  description: "원하는 상품을 검색하고 필터링해 보세요",
};

interface PageProps {
  searchParams: Promise<{
    keyword?: string;
    category?: string;
    minPrice?: string;
    maxPrice?: string;
    sort?: string;
  }>;
}

export default async function Page({ searchParams }: PageProps) {
  const params = await searchParams;
  return <ProductsPage initialParams={params} />;
}
