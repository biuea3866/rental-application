package com.rental.commerce.domain.common

enum class ErrorCode(
    val httpStatus: Int,
    val code: String,
    val message: String,
) {
    // 400
    INVALID_INPUT(400, "INVALID_INPUT", "잘못된 입력입니다"),
    INVALID_STATE_TRANSITION(400, "INVALID_STATE_TRANSITION", "허용되지 않는 상태 전이입니다"),
    INVALID_FILE_TYPE(400, "INVALID_FILE_TYPE", "지원하지 않는 파일 형식입니다"),
    INVALID_VERIFICATION_CODE(400, "INVALID_VERIFICATION_CODE", "인증코드가 일치하지 않습니다"),
    VERIFICATION_CODE_EXPIRED(400, "VERIFICATION_CODE_EXPIRED", "인증코드가 만료되었습니다"),
    PHONE_NOT_VERIFIED(400, "PHONE_NOT_VERIFIED", "휴대폰 인증이 완료되지 않았습니다"),
    PRODUCT_NOT_DRAFT(400, "PRODUCT_NOT_DRAFT", "임시저장 상태의 상품만 수정할 수 있습니다"),

    // 401
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다"),
    EXPIRED_TOKEN(401, "EXPIRED_TOKEN", "토큰이 만료되었습니다"),
    INVALID_TOKEN(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다"),
    TOKEN_FAMILY_COMPROMISED(401, "TOKEN_FAMILY_COMPROMISED", "토큰이 탈취된 것으로 감지되었습니다"),

    // 403
    FORBIDDEN(403, "FORBIDDEN", "접근 권한이 없습니다"),
    PRODUCT_OWNERSHIP_DENIED(403, "PRODUCT_OWNERSHIP_DENIED", "해당 상품의 소유자가 아닙니다"),

    // 404
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "리소스를 찾을 수 없습니다"),
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
    PRODUCT_NOT_FOUND(404, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다"),
    BUCKET_NOT_FOUND(404, "BUCKET_NOT_FOUND", "존재하지 않는 버킷입니다"),
    LENDER_PROFILE_NOT_FOUND(404, "LENDER_PROFILE_NOT_FOUND", "등록자 프로필을 찾을 수 없습니다"),
    RENTER_PROFILE_NOT_FOUND(404, "RENTER_PROFILE_NOT_FOUND", "대여자 프로필을 찾을 수 없습니다"),
    NOTIFICATION_NOT_FOUND(404, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다"),

    // 409
    CONFLICT(409, "CONFLICT", "이미 존재하는 리소스입니다"),
    DUPLICATE_EMAIL(409, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다"),
    DUPLICATE_PHONE(409, "DUPLICATE_PHONE", "이미 등록된 전화번호입니다"),
    DUPLICATE_PROFILE(409, "DUPLICATE_PROFILE", "이미 등록된 프로필입니다"),
    LENDER_PROFILE_ALREADY_EXISTS(409, "LENDER_PROFILE_ALREADY_EXISTS", "이미 등록자 프로필이 존재합니다"),
    RENTER_PROFILE_ALREADY_EXISTS(409, "RENTER_PROFILE_ALREADY_EXISTS", "이미 대여자 프로필이 존재합니다"),

    // 500
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다"),
    EXTERNAL_API_ERROR(500, "EXTERNAL_API_ERROR", "외부 서비스 호출에 실패했습니다"),
}
