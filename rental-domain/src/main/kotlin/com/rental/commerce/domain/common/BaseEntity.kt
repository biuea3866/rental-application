package com.rental.commerce.domain.common

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now()
        protected set

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
        protected set
}
