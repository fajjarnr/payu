package id.payu.support.domain.model;

import java.time.Instant;
import java.util.UUID;

public class SupportTicket {
    private UUID id;
    private String tenantId;
    private String userId;
    private String subject;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String assignedTo;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;

    public SupportTicket() {}
    public UUID getId(){return id;} public void setId(UUID id){this.id=id;}
    public String getTenantId(){return tenantId;} public void setTenantId(String t){this.tenantId=t;}
    public String getUserId(){return userId;} public void setUserId(String u){this.userId=u;}
    public String getSubject(){return subject;} public void setSubject(String s){this.subject=s;}
    public String getDescription(){return description;} public void setDescription(String d){this.description=d;}
    public String getCategory(){return category;} public void setCategory(String c){this.category=c;}
    public String getPriority(){return priority;} public void setPriority(String p){this.priority=p;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public String getAssignedTo(){return assignedTo;} public void setAssignedTo(String a){this.assignedTo=a;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant c){this.createdAt=c;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant u){this.updatedAt=u;}
    public Instant getResolvedAt(){return resolvedAt;} public void setResolvedAt(Instant r){this.resolvedAt=r;}
}
