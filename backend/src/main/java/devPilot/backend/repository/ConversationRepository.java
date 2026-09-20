package devPilot.backend.repository;
import java.util.*; import devPilot.backend.entity.Conversation; import org.springframework.data.jpa.repository.JpaRepository;
public interface ConversationRepository extends JpaRepository<Conversation, UUID> { List<Conversation> findByRepositoryIdAndUserIdOrderByUpdatedAtDesc(UUID repositoryId, UUID userId); Optional<Conversation> findByIdAndUserId(UUID id, UUID userId); }
