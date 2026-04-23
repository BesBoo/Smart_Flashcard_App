package com.example.myapplication.presentation.navigation

import com.example.myapplication.presentation.utilities.ChatBubbleState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint to access ChatBubbleState singleton
 * from composable functions outside of ViewModel scope.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatBubbleStateEntryPoint {
    fun chatBubbleState(): ChatBubbleState
}
