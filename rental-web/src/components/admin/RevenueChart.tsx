"use client";

// ========================================
// 매출 차트 컴포넌트 (CSS-only 바 차트)
// ========================================

interface DailyRevenue {
  date: string;
  amount: number;
}

interface WeeklyRevenue {
  weekStart: string;
  amount: number;
}

interface RevenueChartProps {
  dailyRevenue: DailyRevenue[];
  weeklyRevenue: WeeklyRevenue[];
}

function formatAmount(amount: number): string {
  if (amount >= 1_000_000) return `${(amount / 1_000_000).toFixed(1)}M`;
  if (amount >= 1_000) return `${(amount / 1_000).toFixed(0)}K`;
  return String(amount);
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr);
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

function BarChart({
  data,
  labelKey,
  labelFormatter,
}: {
  data: { label: string; amount: number }[];
  labelKey: string;
  labelFormatter: (label: string) => string;
}) {
  const maxAmount = Math.max(...data.map((d) => d.amount), 1);

  return (
    <div
      data-testid={`bar-chart-${labelKey}`}
      className="flex items-end gap-1 h-32 w-full"
    >
      {data.map((item, idx) => {
        const heightPct = (item.amount / maxAmount) * 100;
        return (
          <div
            key={idx}
            className="flex-1 flex flex-col items-center gap-1 min-w-0"
          >
            <span className="text-xs text-gray-500 truncate w-full text-center">
              {formatAmount(item.amount)}
            </span>
            <div
              className="w-full bg-blue-500 rounded-t transition-all"
              style={{ height: `${Math.max(heightPct, 2)}%` }}
              title={`${labelFormatter(item.label)}: ${item.amount.toLocaleString()}원`}
            />
            <span className="text-xs text-gray-400 truncate w-full text-center">
              {labelFormatter(item.label)}
            </span>
          </div>
        );
      })}
    </div>
  );
}

export function RevenueChart({ dailyRevenue, weeklyRevenue }: RevenueChartProps) {
  const dailyData = dailyRevenue.slice(-7).map((d) => ({
    label: d.date,
    amount: d.amount,
  }));

  const weeklyData = weeklyRevenue.slice(-4).map((w) => ({
    label: w.weekStart,
    amount: w.amount,
  }));

  return (
    <div
      data-testid="revenue-chart"
      className="space-y-6"
    >
      {/* 일별 매출 */}
      <div>
        <h3 className="text-sm font-semibold text-gray-700 mb-3">
          일별 매출 (최근 7일)
        </h3>
        {dailyData.length > 0 ? (
          <BarChart
            data={dailyData}
            labelKey="daily"
            labelFormatter={formatDate}
          />
        ) : (
          <p className="text-sm text-gray-400 text-center py-8">
            데이터 없음
          </p>
        )}
      </div>

      {/* 주별 매출 */}
      <div>
        <h3 className="text-sm font-semibold text-gray-700 mb-3">
          주별 매출 (최근 4주)
        </h3>
        {weeklyData.length > 0 ? (
          <BarChart
            data={weeklyData}
            labelKey="weekly"
            labelFormatter={(label) => `${formatDate(label)}~`}
          />
        ) : (
          <p className="text-sm text-gray-400 text-center py-8">
            데이터 없음
          </p>
        )}
      </div>
    </div>
  );
}
