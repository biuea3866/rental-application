package com.rental.commerce.domain.wishlist

import com.rental.commerce.domain.common.BusinessException
import com.rental.commerce.domain.common.ErrorCode

class WishlistAlreadyExistsException(
    message: String = ErrorCode.WISHLIST_ALREADY_EXISTS.message,
) : BusinessException(ErrorCode.WISHLIST_ALREADY_EXISTS, message)

class WishlistNotFoundException(
    message: String = ErrorCode.WISHLIST_NOT_FOUND.message,
) : BusinessException(ErrorCode.WISHLIST_NOT_FOUND, message)
