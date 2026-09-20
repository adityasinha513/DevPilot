package devPilot.backend.entity;
import java.time.Instant; import java.util.UUID; import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="chat_messages", indexes=@Index(name="idx_messages_conversation_created", columnList="conversation_id,created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {
 public enum Role { USER, ASSISTANT }
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="conversation_id") private Conversation conversation;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private Role role;
 @Column(columnDefinition="TEXT",nullable=false) private String content;
 @Column(name="citations_json",columnDefinition="TEXT") private String citationsJson;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @PrePersist void create(){if(createdAt==null)createdAt=Instant.now();}
}
