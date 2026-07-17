package id.payu.backoffice.domain;
import java.time.LocalDateTime; import java.util.UUID;
public final class KycReview {
 private final UUID id; private final String userId,accountNumber,documentType,documentNumber,documentUrl,fullName,address,phoneNumber; private KycStatus status; private String notes,reviewedBy; private LocalDateTime reviewedAt; private final LocalDateTime createdAt; private final Long version;
 private KycReview(UUID id,String u,String a,String dt,String dn,String du,String fn,String ad,String p,KycStatus s,String n,String rb,LocalDateTime ra,LocalDateTime ca,Long v){if(u==null||u.isBlank())throw new IllegalArgumentException("userId is required"); id=id;this.id=id;userId=u;accountNumber=a;documentType=dt;documentNumber=dn;documentUrl=du;fullName=fn;address=ad;phoneNumber=p;status=s;notes=n;reviewedBy=rb;reviewedAt=ra;createdAt=ca;version=v;}
 public static KycReview create(String u,String a,String dt,String dn,String du,String fn,String ad,String p,String n){return new KycReview(null,u,a,dt,dn,du,fn,ad,p,KycStatus.PENDING,n,null,null,LocalDateTime.now(),null);}
 public static KycReview reconstitute(UUID id,String u,String a,String dt,String dn,String du,String fn,String ad,String p,KycStatus s,String n,String rb,LocalDateTime ra,LocalDateTime ca,Long v){return new KycReview(id,u,a,dt,dn,du,fn,ad,p,s,n,rb,ra,ca,v);}
 public void review(KycStatus s,String n,String actor){
  if(actor==null||actor.isBlank())throw new IllegalArgumentException("reviewedBy is required");
  if(status==KycStatus.APPROVED||status==KycStatus.REJECTED)throw new IllegalStateException("Terminal KYC review cannot transition");
  status=s;notes=n;reviewedBy=actor;reviewedAt=LocalDateTime.now();
 }
 public UUID getId(){return id;} public String getUserId(){return userId;} public String getAccountNumber(){return accountNumber;} public String getDocumentType(){return documentType;} public String getDocumentNumber(){return documentNumber;} public String getDocumentUrl(){return documentUrl;} public String getFullName(){return fullName;} public String getAddress(){return address;} public String getPhoneNumber(){return phoneNumber;} public KycStatus getStatus(){return status;} public String getNotes(){return notes;} public String getReviewedBy(){return reviewedBy;} public LocalDateTime getReviewedAt(){return reviewedAt;} public LocalDateTime getCreatedAt(){return createdAt;} public Long getVersion(){return version;}
 @Override public String toString(){return "KycReview[id="+id+", status="+status+"]";}
}
