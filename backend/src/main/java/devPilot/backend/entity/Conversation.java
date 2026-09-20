package devPilot.backend.entity;
import java.time.Instant; import java.util.UUID; import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="conversations", indexes=@Index(name="idx_conversations_repository_user", columnList="repository_id,user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="repository_id") private GitRepository repository;
 @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id") private User user;
 @Column(nullable=false,length=200) private String title;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
 @Column(name="updated_at",nullable=false) private Instant updatedAt;
 @PrePersist void create(){ Instant now=Instant.now(); if(createdAt==null)createdAt=now; updatedAt=now; }
 @PreUpdate void update(){updatedAt=Instant.now();}
}
