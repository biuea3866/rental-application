"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

// ========================================
// 상수
// ========================================

const VERIFICATION_CODE_LENGTH = 6;
const TIMER_SECONDS = 180; // 3분

// ========================================
// 타이머 포맷
// ========================================

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60)
    .toString()
    .padStart(2, "0");
  const s = (seconds % 60).toString().padStart(2, "0");
  return `${m}:${s}`;
}

// ========================================
// PhoneVerification 컴포넌트
// ========================================

interface PhoneVerificationProps {
  phone: string;
  onVerify: (code: string) => Promise<{ success: boolean; error?: string }>;
  onResend: () => Promise<{ success: boolean; error?: string }>;
  isLoading?: boolean;
}

export function PhoneVerification({
  phone,
  onVerify,
  onResend,
  isLoading = false,
}: PhoneVerificationProps) {
  const [code, setCode] = useState("");
  const [timeLeft, setTimeLeft] = useState(TIMER_SECONDS);
  const [isExpired, setIsExpired] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [resendMessage, setResendMessage] = useState<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // 인터벌 시작 (상태 변경 없이 인터벌만 설정)
  const startInterval = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
    }

    timerRef.current = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timerRef.current!);
          setIsExpired(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, []);

  // 재전송 시 타이머 리셋 + 시작
  const startTimer = useCallback(() => {
    setTimeLeft(TIMER_SECONDS);
    setIsExpired(false);
    startInterval();
  }, [startInterval]);

  useEffect(() => {
    startInterval();
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [startInterval]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setResendMessage(null);

    if (code.length !== VERIFICATION_CODE_LENGTH) {
      setError(`인증코드 ${VERIFICATION_CODE_LENGTH}자리를 입력해주세요.`);
      return;
    }

    if (isExpired) {
      setError("인증 시간이 만료되었습니다. 재전송을 눌러주세요.");
      return;
    }

    const result = await onVerify(code);
    if (!result.success && result.error) {
      setError(result.error);
    }
  };

  const handleResend = async () => {
    setError(null);
    setResendMessage(null);
    setCode("");

    const result = await onResend();
    if (result.success) {
      setResendMessage("인증코드가 재전송되었습니다.");
      startTimer();
    } else if (result.error) {
      setError(result.error);
    }
  };

  return (
    <div className="space-y-4">
      <div className="text-sm text-muted-foreground">
        <span className="font-medium text-foreground">{phone}</span>으로 전송된
        인증코드를 입력하세요.
      </div>

      <form onSubmit={handleSubmit} noValidate aria-label="휴대폰 인증 폼">
        {resendMessage && (
          <div
            role="status"
            className="mb-3 rounded-md bg-green-50 p-3 text-sm text-green-700"
          >
            {resendMessage}
          </div>
        )}

        <div className="space-y-1">
          <div className="flex items-center justify-between">
            <label htmlFor="verification-code" className="text-sm font-medium">
              인증코드
            </label>
            {!isExpired ? (
              <span
                aria-live="polite"
                aria-label={`남은 시간 ${formatTime(timeLeft)}`}
                className="text-sm font-medium text-destructive tabular-nums"
              >
                {formatTime(timeLeft)}
              </span>
            ) : (
              <span className="text-sm text-muted-foreground">만료됨</span>
            )}
          </div>
          <Input
            id="verification-code"
            type="text"
            inputMode="numeric"
            pattern="\d*"
            maxLength={VERIFICATION_CODE_LENGTH}
            placeholder="000000"
            value={code}
            onChange={(e) => {
              const val = e.target.value.replace(/\D/g, "");
              setCode(val);
              setError(null);
            }}
            disabled={isExpired}
            aria-invalid={!!error}
            aria-describedby={error ? "verify-error" : undefined}
          />
          {error && (
            <p
              id="verify-error"
              role="alert"
              className="text-xs text-destructive"
            >
              {error}
            </p>
          )}
        </div>

        <div className="mt-4 flex gap-2">
          <Button
            type="button"
            variant="outline"
            className="flex-1"
            onClick={handleResend}
            disabled={isLoading}
            aria-label="인증코드 재전송"
          >
            재전송
          </Button>
          <Button
            type="submit"
            className="flex-1"
            disabled={isLoading || isExpired}
            aria-busy={isLoading}
          >
            {isLoading ? "확인 중..." : "인증 확인"}
          </Button>
        </div>
      </form>
    </div>
  );
}
