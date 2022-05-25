package me.dannly.domain.repository

import me.dannly.domain.model.Conversation
import me.dannly.domain.model.Message
import me.dannly.domain.model.UserMessageStatusUpdate

interface ConversationDataSource {

    suspend fun getUserConversations(userId: Int): List<Conversation>
    suspend fun getConversationById(id: String): Conversation?
    suspend fun insertConversation(conversation: Conversation)
    suspend fun addMessageAndGetReceivers(newMessage: Message)
    suspend fun getConversationByMessageId(id: String): Conversation?
    suspend fun insertUserReadStatus(userMessageStatusUpdate: UserMessageStatusUpdate): List<Int>
}