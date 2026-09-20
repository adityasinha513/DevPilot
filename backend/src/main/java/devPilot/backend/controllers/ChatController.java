package devPilot.backend.controllers;
import java.util.*; import devPilot.backend.dto.*; import devPilot.backend.security.*; import devPilot.backend.services.RepositoryChatService; import jakarta.validation.Valid; import lombok.RequiredArgsConstructor; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ChatController {
 private final CurrentUser currentUser; private final RepositoryChatService chat;
 @GetMapping("/repositories/{repositoryId}/conversations") List<ConversationResponse> list(@PathVariable UUID repositoryId){return chat.list(currentUser.require().getId(),repositoryId);}
 @PostMapping("/repositories/{repositoryId}/conversations") @ResponseStatus(HttpStatus.CREATED) ConversationResponse create(@PathVariable UUID repositoryId,@Valid @RequestBody CreateConversationRequest request){return chat.create(currentUser.require().getId(),repositoryId,request.title());}
 @GetMapping("/conversations/{conversationId}/messages") List<MessageResponse> messages(@PathVariable UUID conversationId){return chat.messages(currentUser.require().getId(),conversationId);}
 @PostMapping("/conversations/{conversationId}/messages") ChatTurnResponse send(@PathVariable UUID conversationId,@Valid @RequestBody SendMessageRequest request){return chat.send(currentUser.require().getId(),conversationId,request.content());}
}
