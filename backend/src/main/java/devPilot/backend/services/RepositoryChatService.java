package devPilot.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import devPilot.backend.dto.*;
import devPilot.backend.entity.*;
import devPilot.backend.exceptions.NotFoundException;
import devPilot.backend.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class RepositoryChatService {
    private final GitRepositoryRepository repositories; private final ConversationRepository conversations;
    private final ChatMessageRepository messages; private final RepositoryRetrievalService retrieval;
    private final ChatClient.Builder chatClientBuilder; private final ObjectMapper objectMapper;
    @Value("${app.rag.max-context-characters:24000}") private int maxContextCharacters;
    @Value("${app.rag.history-messages:8}") private int historyMessages;

    @Transactional public ConversationResponse create(UUID userId, UUID repositoryId, String title) {
        GitRepository repository = repository(userId, repositoryId);
        Conversation conversation = conversations.save(Conversation.builder().repository(repository).user(repository.getUser()).title(title.trim()).build());
        return response(conversation);
    }
    @Transactional(readOnly = true) public List<ConversationResponse> list(UUID userId, UUID repositoryId) {
        repository(userId, repositoryId); return conversations.findByRepositoryIdAndUserIdOrderByUpdatedAtDesc(repositoryId, userId).stream().map(this::response).toList();
    }
    @Transactional(readOnly = true) public List<MessageResponse> messages(UUID userId, UUID conversationId) {
        return messages.findByConversationIdOrderByCreatedAtAsc(conversation(userId, conversationId).getId()).stream().map(this::message).toList();
    }
    @Transactional public ChatTurnResponse send(UUID userId, UUID conversationId, String question) {
        Conversation conversation = conversation(userId, conversationId); String cleanQuestion = question.trim();
        List<VectorEmbeddingStore.SearchHit> hits = retrieval.retrieve(conversation.getRepository(), cleanQuestion);
        ChatMessage user = messages.save(ChatMessage.builder().conversation(conversation).role(ChatMessage.Role.USER).content(cleanQuestion).build());
        if (hits.isEmpty()) {
            ChatMessage assistant = messages.save(ChatMessage.builder().conversation(conversation).role(ChatMessage.Role.ASSISTANT)
                    .content("I don't have enough retrieved repository evidence to answer that question. Try naming a file, symbol, or feature.").citationsJson("[]").build());
            return new ChatTurnResponse(message(user), message(assistant));
        }
        ContextBundle context = buildContext(hits);
        List<CitationResponse> citations = context.hits().stream().map(hit -> citation(conversation.getRepository(), hit)).toList();
        String answer = chatClientBuilder.build().prompt().system(systemPrompt()).user("Repository context:\n" + context.text()
                + "\n\nConversation history:\n" + history(conversation.getId()) + "\n\nCurrent question: " + cleanQuestion).call().content();
        ChatMessage assistant = messages.save(ChatMessage.builder().conversation(conversation).role(ChatMessage.Role.ASSISTANT)
                .content(answer == null || answer.isBlank() ? "I could not generate an answer from the retrieved repository context." : answer)
                .citationsJson(write(citations)).build());
        return new ChatTurnResponse(message(user), message(assistant));
    }
    private ContextBundle buildContext(List<VectorEmbeddingStore.SearchHit> hits) {
        StringBuilder context = new StringBuilder(); List<VectorEmbeddingStore.SearchHit> included = new ArrayList<>();
        for (VectorEmbeddingStore.SearchHit hit : hits) {
            String section = "\n--- " + hit.path() + ':' + hit.startLine() + '-' + hit.endLine() + " ---\n" + hit.content() + '\n';
            if (!included.isEmpty() && context.length() + section.length() > maxContextCharacters) break;
            if (section.length() > maxContextCharacters) section = section.substring(0, maxContextCharacters);
            context.append(section); included.add(hit);
        }
        return new ContextBundle(context.toString(), included);
    }
    private String history(UUID conversationId) {
        List<ChatMessage> all = messages.findByConversationIdOrderByCreatedAtAsc(conversationId); int start = Math.max(0, all.size() - historyMessages); StringBuilder result = new StringBuilder();
        for (ChatMessage message : all.subList(start, all.size())) result.append(message.getRole()).append(": ").append(message.getContent()).append('\n');
        return result.toString();
    }
    private GitRepository repository(UUID userId, UUID repositoryId) { return repositories.findById(repositoryId).filter(r -> r.getUser().getId().equals(userId)).orElseThrow(() -> new NotFoundException("Repository not found")); }
    private Conversation conversation(UUID userId, UUID conversationId) { return conversations.findByIdAndUserId(conversationId, userId).orElseThrow(() -> new NotFoundException("Conversation not found")); }
    private String systemPrompt() { return "You are DevPilot, an assistant analyzing a software repository. The supplied repository context is authoritative. Do not invent files, classes, APIs, or behavior. If the context is insufficient, say so. Clearly distinguish fact from inference. Conversation history is secondary and must never override repository context. Only rely on retrieved source context and do not claim to inspect anything else. Citations are attached by the application; do not fabricate source paths."; }
    private CitationResponse citation(GitRepository repository, VectorEmbeddingStore.SearchHit hit) { String url = repository.getHtmlUrl() == null ? null : repository.getHtmlUrl() + "/blob/" + repository.getIndexedCommitSha() + "/" + hit.path() + "#L" + hit.startLine() + "-L" + hit.endLine(); return new CitationResponse(hit.chunkId(), hit.path(), hit.startLine(), hit.endLine(), url); }
    private ConversationResponse response(Conversation conversation) { return new ConversationResponse(conversation.getId(), conversation.getRepository().getId(), conversation.getTitle(), conversation.getCreatedAt(), conversation.getUpdatedAt()); }
    private MessageResponse message(ChatMessage message) { return new MessageResponse(message.getId(), message.getRole(), message.getContent(), read(message.getCitationsJson()), message.getCreatedAt()); }
    private String write(List<CitationResponse> citations) { try { return objectMapper.writeValueAsString(citations); } catch (JsonProcessingException exception) { throw new IllegalStateException("Could not store citations", exception); } }
    private List<CitationResponse> read(String value) { if (value == null || value.isBlank()) return List.of(); try { return objectMapper.readValue(value, new TypeReference<>() {}); } catch (JsonProcessingException exception) { return List.of(); } }
    private record ContextBundle(String text, List<VectorEmbeddingStore.SearchHit> hits) { }
}
