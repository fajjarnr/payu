package id.payu.backoffice.adapter.persistence;
import static org.junit.jupiter.api.Assertions.*; import id.payu.backoffice.domain.*; import org.junit.jupiter.api.Test;
class KycReviewMapperTest { @Test void roundTripPreservesStatus(){KycReviewMapper m=new KycReviewMapper(); KycReview r=KycReview.create("user","ACC","KTP","nik","url","Name","Address","0812",null); assertEquals(KycStatus.PENDING,m.toDomain(m.toEntity(r)).getStatus());} }
