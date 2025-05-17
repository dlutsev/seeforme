package com.example.seeforme

object WebRTCState {
    private var isCallInProgress = false
    private var currentUser: String? = null
    private var targetUser: String? = null

    @Synchronized
    fun startCall(currentUser: String, targetUser: String): Boolean {
        return if (!isCallInProgress) {
            this.currentUser = currentUser
            this.targetUser = targetUser
            isCallInProgress = true
            true
        } else {
            false
        }
    }

    @Synchronized
    fun endCall() {
        isCallInProgress = false
        currentUser = null
        targetUser = null
    }

    @Synchronized
    fun isCallActive(): Boolean {
        return isCallInProgress
    }

    @Synchronized
    fun getCurrentUser(): String? {
        return currentUser
    }

    @Synchronized
    fun getTargetUser(): String? {
        return targetUser
    }
} 