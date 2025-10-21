package com.originb.whatstap2.domain.model

data class SyncResult(
    val inserted: Int,
    val updated: Int,
    val hasChanges: Boolean
)

