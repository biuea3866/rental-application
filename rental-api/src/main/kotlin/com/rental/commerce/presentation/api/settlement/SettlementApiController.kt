package com.rental.commerce.presentation.api.settlement

import com.rental.commerce.application.settlement.GetMySettlementsCommand
import com.rental.commerce.application.settlement.GetMySettlementsUseCase
import com.rental.commerce.application.settlement.SettlementResult
import com.rental.commerce.domain.common.PageResult
import com.rental.commerce.presentation.api.common.AuthenticatedMember
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class SettlementApiController(
    private val getMySettlementsUseCase: GetMySettlementsUseCase,
) {

    @GetMapping("/my-settlements")
    fun getMySettlements(
        @AuthenticatedMember lenderId: Long,
    ): ResponseEntity<PageResult<SettlementResult>> {
        val result = getMySettlementsUseCase.execute(GetMySettlementsCommand(lenderId = lenderId))
        return ResponseEntity.ok(result)
    }
}
