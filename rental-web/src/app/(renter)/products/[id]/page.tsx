import type { Metadata } from "next";
import { ProductDetailPage } from "./_product-detail-page";

export const metadata: Metadata = {
  title: "상품 상세 | Rental Commerce",
};

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function Page({ params }: PageProps) {
  const { id } = await params;
  return <ProductDetailPage id={id} />;
}
