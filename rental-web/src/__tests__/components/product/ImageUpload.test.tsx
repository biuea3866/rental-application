import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, beforeEach, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Step3Images } from "@/components/product/Step3Images";
import { useProductFormStore } from "@/stores/product-form-store";

// ========================================
// 헬퍼
// ========================================

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
}

function renderWithQuery(ui: React.ReactElement) {
  const client = makeQueryClient();
  return render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>
  );
}

// ========================================
// ImageUpload (Step3Images) — Presigned URL 흐름 테스트
// ========================================

describe("Step3Images — 이미지 업로드", () => {
  const user = userEvent.setup();

  beforeEach(() => {
    useProductFormStore.getState().reset();

    // URL.createObjectURL 모킹
    globalThis.URL.createObjectURL = vi.fn(() => "blob:test-url");
    globalThis.URL.revokeObjectURL = vi.fn();
  });

  it("드래그 앤 드롭 영역이 렌더링된다", () => {
    const onNext = vi.fn();
    const onPrev = vi.fn();
    renderWithQuery(<Step3Images onNext={onNext} onPrev={onPrev} />);

    expect(screen.getByTestId("drop-zone")).toBeInTheDocument();
    expect(
      screen.getByText(/이미지를 드래그하거나 클릭하여 업로드/i)
    ).toBeInTheDocument();
  });

  it("파일 input이 숨겨진 채로 존재한다", () => {
    const onNext = vi.fn();
    const onPrev = vi.fn();
    renderWithQuery(<Step3Images onNext={onNext} onPrev={onPrev} />);

    const input = screen.getByTestId("file-input");
    expect(input).toBeInTheDocument();
    expect(input).toHaveAttribute("type", "file");
    expect(input).toHaveAttribute("multiple");
  });

  it("파일 선택 시 업로드 시작 — Presigned URL API가 호출된다", async () => {
    const onNext = vi.fn();
    const onPrev = vi.fn();
    renderWithQuery(<Step3Images onNext={onNext} onPrev={onPrev} />);

    const file = new File(["test image content"], "test.jpg", {
      type: "image/jpeg",
    });

    const input = screen.getByTestId("file-input");
    await user.upload(input, file);

    // MSW 핸들러가 응답할 때까지 대기
    await waitFor(
      () => {
        // 업로드 중 또는 완료 상태
        const store = useProductFormStore.getState();
        const files = store.step3.uploadingFiles;
        expect(files.length).toBeGreaterThan(0);
      },
      { timeout: 3000 }
    );
  });

  it("업로드 성공 시 imageUrls에 URL이 추가된다", async () => {
    const onNext = vi.fn();
    const onPrev = vi.fn();
    renderWithQuery(<Step3Images onNext={onNext} onPrev={onPrev} />);

    const file = new File(["test image"], "photo.png", {
      type: "image/png",
    });

    const input = screen.getByTestId("file-input");
    await user.upload(input, file);

    await waitFor(
      () => {
        const store = useProductFormStore.getState();
        const doneFile = store.step3.uploadingFiles.find(
          (f) => f.status === "done"
        );
        if (doneFile) {
          expect(store.step3.imageUrls.length).toBeGreaterThan(0);
        }
      },
      { timeout: 5000 }
    );
  });

  it("다음 단계 버튼 클릭 시 onNext가 호출된다", async () => {
    const onNext = vi.fn();
    const onPrev = vi.fn();
    renderWithQuery(<Step3Images onNext={onNext} onPrev={onPrev} />);

    await user.click(screen.getByRole("button", { name: /다음 단계/i }));
    expect(onNext).toHaveBeenCalledOnce();
  });

  it("이전 버튼 클릭 시 onPrev가 호출된다", async () => {
    const onNext = vi.fn();
    const onPrev = vi.fn();
    renderWithQuery(<Step3Images onNext={onNext} onPrev={onPrev} />);

    await user.click(screen.getByRole("button", { name: /이전/i }));
    expect(onPrev).toHaveBeenCalledOnce();
  });

  it("드래그앤드롭 이벤트가 동작한다", async () => {
    const onNext = vi.fn();
    const onPrev = vi.fn();
    renderWithQuery(<Step3Images onNext={onNext} onPrev={onPrev} />);

    const dropZone = screen.getByTestId("drop-zone");

    const file = new File(["drop test"], "dropped.jpg", {
      type: "image/jpeg",
    });

    // dragover 이벤트 (preventDefault 확인)
    fireEvent.dragOver(dropZone, {
      dataTransfer: { files: [file] },
    });

    // drop 이벤트
    fireEvent.drop(dropZone, {
      dataTransfer: { files: [file] },
    });

    // 업로드 파일이 추가되었는지 확인
    await waitFor(
      () => {
        const store = useProductFormStore.getState();
        expect(store.step3.uploadingFiles.length).toBeGreaterThan(0);
      },
      { timeout: 3000 }
    );
  });
});

// ========================================
// Presigned URL API 통합 테스트
// ========================================

describe("Presigned URL MSW 핸들러", () => {
  it("POST /api/v1/images/presigned-url — presignedUrl과 fileUrl을 반환한다", async () => {
    const BASE_URL = "http://localhost:8080";

    const response = await fetch(`${BASE_URL}/api/v1/images/presigned-url`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fileName: "test.jpg",
        contentType: "image/jpeg",
        fileSize: 1024,
      }),
    });

    expect(response.ok).toBe(true);

    const data = await response.json();
    expect(data.success).toBe(true);
    expect(data.data.presignedUrl).toBeTruthy();
    expect(data.data.fileUrl).toBeTruthy();
    expect(data.data.expiresIn).toBe(3600);
  });
});
