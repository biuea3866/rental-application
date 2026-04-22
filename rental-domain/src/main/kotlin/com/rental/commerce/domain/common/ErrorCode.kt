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
    PRODUCT_NOT_DELETABLE(400, "PRODUCT_NOT_DELETABLE", "삭제할 수 없는 상태의 상품입니다"),
    PRODUCT_NOT_UNDER_REVIEW(400, "PRODUCT_NOT_UNDER_REVIEW", "검수 중인 상품만 승인/반려할 수 있습니다"),

    // 401
    UNAUTHORIZED(401, "UNAUTHORIZED", "인증이 필요합니다"),
    INVALID_PASSWORD(401, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다"),
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
    RENTAL_NOT_FOUND(404, "RENTAL_NOT_FOUND", "대여를 찾을 수 없습니다"),

    // 402
    PAYMENT_FAILED(402, "PAYMENT_FAILED", "결제에 실패했습니다"),

    // 409
    SOCIAL_ACCOUNT_ALREADY_LINKED(409, "SOCIAL_ACCOUNT_ALREADY_LINKED", "이미 소셜 계정이 연결되어 있습니다"),
    CONFLICT(409, "CONFLICT", "이미 존재하는 리소스입니다"),
    DUPLICATE_EMAIL(409, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다"),
    DUPLICATE_PHONE(409, "DUPLICATE_PHONE", "이미 등록된 전화번호입니다"),
    DUPLICATE_PROFILE(409, "DUPLICATE_PROFILE", "이미 등록된 프로필입니다"),
    LENDER_PROFILE_ALREADY_EXISTS(409, "LENDER_PROFILE_ALREADY_EXISTS", "이미 등록자 프로필이 존재합니다"),
    RENTER_PROFILE_ALREADY_EXISTS(409, "RENTER_PROFILE_ALREADY_EXISTS", "이미 대여자 프로필이 존재합니다"),
    RENTAL_PERIOD_CONFLICT(409, "RENTAL_PERIOD_CONFLICT", "해당 기간에 이미 대여 신청이 존재합니다"),
    ALREADY_PAID(409, "ALREADY_PAID", "이미 결제가 완료된 대여입니다"),
    REVIEW_ALREADY_EXISTS(409, "REVIEW_ALREADY_EXISTS", "이미 리뷰가 작성된 대여입니다"),

    // 400 — Review
    REVIEW_INVALID_RATING(400, "REVIEW_INVALID_RATING", "rating은 1~5 사이어야 합니다"),
    REVIEW_CONTENT_TOO_SHORT(400, "REVIEW_CONTENT_TOO_SHORT", "리뷰 내용은 10~500자 사이어야 합니다"),

    // 404 — Review
    REVIEW_NOT_FOUND(404, "REVIEW_NOT_FOUND", "리뷰를 찾을 수 없습니다"),

    // 422 — Review
    REVIEW_RENTAL_NOT_RETURNED(422, "REVIEW_RENTAL_NOT_RETURNED", "반납 완료된 대여에만 리뷰를 작성할 수 있습니다"),

    // 403 — Review
    REVIEW_FORBIDDEN(403, "REVIEW_FORBIDDEN", "본인의 대여에만 리뷰를 작성할 수 있습니다"),

    // 403 — Chat
    CHAT_ACCESS_DENIED(403, "CHAT_ACCESS_DENIED", "채팅방 접근 권한이 없습니다"),

    // 404 — Chat
    CHAT_ROOM_NOT_FOUND(404, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다"),

    // 409 — Settlement
    SETTLEMENT_ALREADY_EXISTS(409, "SETTLEMENT_ALREADY_EXISTS", "이미 정산이 생성된 대여입니다"),

    // 422 — Settlement
    SETTLEMENT_ALREADY_PROCESSED(422, "SETTLEMENT_ALREADY_PROCESSED", "이미 처리된 정산입니다"),

    // Dispute — 400/403/404/409
    DISPUTE_INVALID_DESCRIPTION(400, "DISPUTE_INVALID_DESCRIPTION", "분쟁 설명은 1~1000자여야 합니다"),
    DISPUTE_INVALID_AMOUNT(400, "DISPUTE_INVALID_AMOUNT", "환불 금액은 0보다 커야 합니다"),
    DISPUTE_INVALID_STATE_TRANSITION(422, "DISPUTE_INVALID_STATE_TRANSITION", "허용되지 않는 분쟁 상태 전이입니다"),
    DISPUTE_FORBIDDEN(403, "DISPUTE_FORBIDDEN", "해당 분쟁에 대한 접근 권한이 없습니다"),
    DISPUTE_NOT_FOUND(404, "DISPUTE_NOT_FOUND", "분쟁을 찾을 수 없습니다"),
    DISPUTE_ALREADY_ACTIVE(409, "DISPUTE_ALREADY_ACTIVE", "이미 진행 중인 분쟁이 있습니다"),

    // Wishlist
    WISHLIST_ALREADY_EXISTS(409, "WISHLIST_ALREADY_EXISTS", "이미 위시리스트에 담긴 상품입니다"),
    WISHLIST_NOT_FOUND(404, "WISHLIST_NOT_FOUND", "위시리스트 항목을 찾을 수 없습니다"),

    // Notification Preference
    NOTIFICATION_PREFERENCE_NOT_FOUND(404, "NOTIFICATION_PREFERENCE_NOT_FOUND", "알림 설정이 존재하지 않습니다"),

    // 500
    INTERNAL_SERVER_ERROR(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다"),
    EXTERNAL_API_ERROR(500, "EXTERNAL_API_ERROR", "외부 서비스 호출에 실패했습니다"),
}
