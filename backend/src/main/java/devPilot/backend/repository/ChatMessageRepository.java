package devPilot.backend.repository;
import java.util.*; import devPilot.backend.entity.ChatMessage; import org.springframework.data.jpa.repository.JpaRepository;
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> { List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId); }
