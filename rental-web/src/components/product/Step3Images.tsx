"use client";

import { useCallback, useRef } from "react";
import { useProductFormStore } from "@/stores/product-form-store";
import { getPresignedUrlApi, uploadToS3 } from "@/lib/api/draft";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

// ========================================
// Step3Images — 이미지 업로드 (Presigned URL)
// ========================================

const MAX_IMAGES = 10;
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

interface Step3ImagesProps {
  onNext: () => void;
  onPrev: () => void;
}

export function Step3Images({ onNext, onPrev }: Step3ImagesProps) {
  const { step3, addUploadingFile, updateUploadingFile, removeUploadingFile, isSubmitting } =
    useProductFormStore();
  const inputRef = useRef<HTMLInputElement>(null);

  /** Presigned URL 방식 이미지 업로드 */
  const uploadImage = useCallback(async (file: File): Promise<string> => {
    const presignedRes = await getPresignedUrlApi({
      fileName: file.name,
      contentType: file.type,
      fileSize: file.size,
    });
    await uploadToS3(presignedRes.data.presignedUrl, file);
    return presignedRes.data.fileUrl;
  }, []);

  const handleFileSelect = useCallback(
    async (files: FileList | null) => {
      if (!files) return;

      const remaining = MAX_IMAGES - step3.imageUrls.length - step3.uploadingFiles.filter(f => f.status !== 'error').length;
      const filesToUpload = Array.from(files).slice(0, remaining);

      for (const file of filesToUpload) {
        // 파일 유효성 검사
        if (!ALLOWED_TYPES.includes(file.type)) {
          continue;
        }
        if (file.size > MAX_FILE_SIZE) {
          continue;
        }

        const id = `upload-${Date.now()}-${Math.random().toString(36).slice(2)}`;
        const previewUrl = URL.createObjectURL(file);

        addUploadingFile({
          id,
          file,
          progress: 0,
          status: "uploading",
          previewUrl,
        });

        // 비동기 업로드
        try {
          updateUploadingFile(id, { progress: 30 });
          const fileUrl = await uploadImage(file);
          updateUploadingFile(id, { progress: 100, status: "done", uploadedUrl: fileUrl });

          // 업로드된 URL을 imageUrls에 추가
          useProductFormStore.getState().updateStep3({
            imageUrls: [...useProductFormStore.getState().step3.imageUrls, fileUrl],
          });
        } catch {
          updateUploadingFile(id, {
            status: "error",
            error: "업로드에 실패했습니다.",
          });
        }
      }
    },
    [step3.imageUrls, step3.uploadingFiles, addUploadingFile, updateUploadingFile, uploadImage]
  );

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      handleFileSelect(e.dataTransfer.files);
    },
    [handleFileSelect]
  );

  const handleRemoveUploaded = (url: string) => {
    useProductFormStore.getState().updateStep3({
      imageUrls: step3.imageUrls.filter((u) => u !== url),
    });
  };

  const handleRemoveUploading = (id: string) => {
    const file = step3.uploadingFiles.find((f) => f.id === id);
    if (file?.previewUrl) URL.revokeObjectURL(file.previewUrl);
    removeUploadingFile(id);
  };

  const totalImages =
    step3.imageUrls.length +
    step3.uploadingFiles.filter((f) => f.status !== "error").length;
  const canUploadMore = totalImages < MAX_IMAGES;

  const handleNext = () => {
    onNext();
  };

  return (
    <div className="space-y-5">
      {/* 업로드 영역 */}
      {canUploadMore && (
        <div
          data-testid="drop-zone"
          onDragOver={(e) => e.preventDefault()}
          onDrop={handleDrop}
          onClick={() => inputRef.current?.click()}
          className="flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-border bg-muted/20 p-10 text-center transition-colors hover:bg-muted/40"
        >
          <svg
            className="mb-3 h-10 w-10 text-muted-foreground"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
            />
          </svg>
          <p className="text-sm font-medium text-foreground">
            이미지를 드래그하거나 클릭하여 업로드
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            JPG, PNG, WEBP, GIF · 최대 10MB · 최대 {MAX_IMAGES}장
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            ({totalImages}/{MAX_IMAGES}장 선택됨)
          </p>
          <input
            ref={inputRef}
            type="file"
            accept={ALLOWED_TYPES.join(",")}
            multiple
            className="hidden"
            onChange={(e) => handleFileSelect(e.target.files)}
            data-testid="file-input"
          />
        </div>
      )}

      {/* 이미지 프리뷰 그리드 */}
      {(step3.imageUrls.length > 0 || step3.uploadingFiles.length > 0) && (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4">
          {/* 업로드 완료 이미지 */}
          {step3.imageUrls.map((url, idx) => (
            <div key={url} className="group relative aspect-square overflow-hidden rounded-lg border">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={url}
                alt={`상품 이미지 ${idx + 1}`}
                className="h-full w-full object-cover"
              />
              {idx === 0 && (
                <span className="absolute left-1 top-1 rounded bg-primary px-1.5 py-0.5 text-[10px] font-medium text-primary-foreground">
                  대표
                </span>
              )}
              <button
                type="button"
                onClick={() => handleRemoveUploaded(url)}
                className="absolute right-1 top-1 hidden h-6 w-6 items-center justify-center rounded-full bg-background/80 text-foreground shadow group-hover:flex"
                aria-label="이미지 삭제"
              >
                <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          ))}

          {/* 업로드 중 이미지 */}
          {step3.uploadingFiles.map((f) => (
            <div
              key={f.id}
              className={cn(
                "relative aspect-square overflow-hidden rounded-lg border",
                f.status === "error" && "border-destructive"
              )}
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={f.previewUrl}
                alt="업로드 중"
                className="h-full w-full object-cover opacity-60"
              />
              {f.status === "uploading" && (
                <div className="absolute inset-0 flex flex-col items-center justify-center bg-background/60">
                  <div className="h-1.5 w-3/4 overflow-hidden rounded-full bg-muted">
                    <div
                      className="h-full rounded-full bg-primary transition-all"
                      style={{ width: `${f.progress}%` }}
                    />
                  </div>
                  <span className="mt-1.5 text-xs text-foreground">
                    업로드 중...
                  </span>
                </div>
              )}
              {f.status === "error" && (
                <div className="absolute inset-0 flex flex-col items-center justify-center bg-background/60">
                  <span className="text-xs text-destructive">실패</span>
                </div>
              )}
              <button
                type="button"
                onClick={() => handleRemoveUploading(f.id)}
                className="absolute right-1 top-1 flex h-6 w-6 items-center justify-center rounded-full bg-background/80 text-foreground shadow"
                aria-label="이미지 삭제"
              >
                <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          ))}
        </div>
      )}

      <p className="text-xs text-muted-foreground">
        첫 번째 이미지가 대표 이미지로 사용됩니다. 최소 1장 이상 업로드를 권장합니다.
      </p>

      <div className="flex justify-between pt-2">
        <Button type="button" variant="outline" onClick={onPrev} size="lg">
          이전
        </Button>
        <Button
          type="button"
          onClick={handleNext}
          disabled={isSubmitting}
          size="lg"
        >
          {isSubmitting ? "저장 중..." : "다음 단계"}
        </Button>
      </div>
    </div>
  );
}
