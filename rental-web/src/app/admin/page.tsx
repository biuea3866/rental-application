"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { RentalStatusCards } from "@/components/admin/RentalStatusCards";
import { RevenueChart } from "@/components/admin/RevenueChart";
import { AdminRentalTable } from "@/components/admin/AdminRentalTable";
import { getAdminDashboardApi, getAdminRentalsApi } from "@/lib/api/admin";
import type { RentalStatus } from "@/lib/api/types";

// ========================================
// 관리자 대시보드 페이지
// RC-FE-328 ~ RC-FE-330
// ========================================

export default function AdminDashboardPage() {
  const [rentalStatusFilter, setRentalStatusFilter] = useState<
    RentalStatus | undefined
  >(undefined);

  const {
    data: dashboardData,
    isLoading: dashboardLoading,
    isError: dashboardError,
    refetch: refetchDashboard,
  } = useQuery({
    queryKey: ["admin-dashboard"],
    queryFn: getAdminDashboardApi,
  });

  const {
    data: rentalsData,
    isLoading: rentalsLoading,
    refetch: refetchRentals,
  } = useQuery({
    queryKey: ["admin-rentals", rentalStatusFilter],
    queryFn: () =>
      getAdminRentalsApi({
        status: rentalStatusFilter,
        page: 0,
        size: 20,
      }),
  });

  const dashboard = dashboardData?.data;
  const rentals = rentalsData?.data?.content ?? [];

  const handleStatusFilterChange = (status: RentalStatus | undefined) => {
    setRentalStatusFilter(status);
  };

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">대시보드</h1>
        <button
          onClick={() => {
            refetchDashboard();
            refetchRentals();
          }}
          className="px-3 py-1.5 text-sm text-gray-600 border border-gray-300 rounded-lg hover:bg-gray-50"
        >
          새로고침
        </button>
      </div>

      {/* 대여 상태 카드 */}
      <section>
        <h2 className="text-base font-semibold text-gray-700 mb-3">
          대여 현황
        </h2>
        {dashboardLoading && (
          <div
            data-testid="dashboard-loading"
            className="grid grid-cols-2 sm:grid-cols-3 gap-3"
          >
            {[...Array(6)].map((_, i) => (
              <div
                key={i}
                className="h-24 bg-gray-200 rounded-xl animate-pulse"
              />
            ))}
          </div>
        )}
        {dashboardError && !dashboardLoading && (
          <div
            data-testid="dashboard-error"
            className="text-center py-8 text-sm text-gray-500"
          >
            데이터를 불러오지 못했습니다.
          </div>
        )}
        {!dashboardLoading && dashboard && (
          <RentalStatusCards counts={dashboard.rentalCounts} />
        )}
      </section>

      {/* 매출 차트 */}
      {!dashboardLoading && dashboard && (
        <section className="bg-white rounded-xl border border-gray-200 p-5">
          <h2 className="text-base font-semibold text-gray-700 mb-4">
            매출 현황
          </h2>
          <RevenueChart
            dailyRevenue={dashboard.dailyRevenue}
            weeklyRevenue={dashboard.weeklyRevenue}
          />
        </section>
      )}

      {/* 대여 목록 */}
      <section>
        <h2 className="text-base font-semibold text-gray-700 mb-3">
          전체 대여 목록
        </h2>
        <AdminRentalTable
          rentals={rentals}
          isLoading={rentalsLoading}
          onStatusFilterChange={handleStatusFilterChange}
        />
      </section>
    </div>
  );
}
