package com.rental.commerce.application.rental

import com.rental.commerce.domain.rental.DeliveryInfo
import java.time.ZonedDateTime

data class RequestRentalCommand(
    val renterId: Long,
    val productId: Long,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val dailyPrice: Long,
    val deliveryInfo: DeliveryInfo,
)
