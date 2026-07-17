package id.payu.backoffice.domain;
import static org.junit.jupiter.api.Assertions.*; import org.junit.jupiter.api.Test;
class KycReviewTest {
 @Test void startsPendingAndRecordsReviewWithoutLeakingPiiFromToString(){KycReview r=KycReview.create("user","ACC","KTP","secret-nik","url","Name","Address","0812",null); assertEquals(KycStatus.PENDING,r.getStatus()); assertFalse(r.toString().contains("secret-nik")); r.review(KycStatus.APPROVED,"ok","agent"); assertEquals("agent",r.getReviewedBy()); assertNotNull(r.getReviewedAt());}
}
