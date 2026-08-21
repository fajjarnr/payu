package id.payu.support.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "faqs")
public class FaqEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "category", nullable = false)
    private String category = "GENERAL";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { if (id==null) id=UUID.randomUUID(); createdAt=Instant.now(); }

    public FaqEntity() {}
    public UUID getId(){return id;} public void setId(UUID i){this.id=i;}
    public String getQuestion(){return question;} public void setQuestion(String q){this.question=q;}
    public String getAnswer(){return answer;} public void setAnswer(String a){this.answer=a;}
    public String getCategory(){return category;} public void setCategory(String c){this.category=c;}
    public Instant getCreatedAt(){return createdAt;}
}
