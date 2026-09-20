package devPilot.backend.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import devPilot.backend.entity.GitRepository;
import devPilot.backend.entity.User;
import devPilot.backend.exceptions.NotFoundException;
import devPilot.backend.repository.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
class RepositoryChatServiceTest {
    @Mock private GitRepositoryRepository repositories;
    @Mock private ConversationRepository conversations;
    @Mock private ChatMessageRepository messages;
    @Mock private RepositoryRetrievalService retrieval;
    @Mock private ChatClient.Builder chatClientBuilder;

    @Test
    void userCannotListAnotherUsersConversations() {
        UUID caller = UUID.randomUUID();
        User owner = new User(); owner.setId(UUID.randomUUID());
        GitRepository repository = new GitRepository(); repository.setId(UUID.randomUUID()); repository.setUser(owner);
        when(repositories.findById(repository.getId())).thenReturn(Optional.of(repository));
        RepositoryChatService service = new RepositoryChatService(repositories, conversations, messages, retrieval, chatClientBuilder, new ObjectMapper());
        assertThatThrownBy(() -> service.list(caller, repository.getId())).isInstanceOf(NotFoundException.class);
    }
}
