package me.dannly.data.repository

import me.dannly.domain.model.Conversation
import me.dannly.domain.model.Message
import me.dannly.domain.model.UserMessageStatusUpdate
import me.dannly.domain.repository.ConversationDataSource
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineDatabase

class ConversationDataSourceImpl(
    database: CoroutineDatabase
) : ConversationDataSource {

    private val conversations = database.getCollection<Conversation>()

    override suspend fun getUserConversations(userId: Int): List<Conversation> {
        return conversations.find(Conversation::participants contains userId).toList()
    }

    override suspend fun getConversationByMessageId(id: String): Conversation? {
        return conversations.findOne(Conversation::messages.elemMatch(Message::id eq id))
    }

    override suspend fun getConversationById(id: String): Conversation? {
        return conversations.findOneById(id)
    }

    override suspend fun insertConversation(conversation: Conversation) {
        conversations.updateOneById(conversation.id, conversation, upsert())
    }

    override suspend fun insertUserReadStatus(userMessageStatusUpdate: UserMessageStatusUpdate): List<Int> {
        val conversation = conversations.findOneById(userMessageStatusUpdate.conversationId) ?: return emptyList()
        conversations.updateMany(
            Conversation::messages / Message::id eq userMessageStatusUpdate.messageId, setValue(
                Conversation::messages.allPosOp / Message::messageStatus.keyProjection(userMessageStatusUpdate.userId),
                userMessageStatusUpdate.read
            )
        )
        return conversation.participants
    }

    override suspend fun addMessageAndGetReceivers(newMessage: Message) {
        conversations.updateOneById(
            newMessage.conversationId, push(Conversation::messages, newMessage)
        )
    }
}