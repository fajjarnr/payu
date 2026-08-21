package id.payu.support.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Faq {
    private UUID id;
    private String question;
    private String answer;
    private String category;
    private Instant createdAt;
    public UUID getId(){return id;} public void setId(UUID id){this.id=id;}
    public String getQuestion(){return question;} public void setQuestion(String q){this.question=q;}
    public String getAnswer(){return answer;} public void setAnswer(String a){this.answer=a;}
    public String getCategory(){return category;} public void setCategory(String c){this.category=c;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant c){this.createdAt=c;}
}
