package id.payu.cms.domain.port.out;

import id.payu.cms.domain.entity.Content;

/**
 * Outbound port for Content event publishing.
 */
public interface ContentEventPort {

    void publishContentCreated(Content content);

    void publishContentUpdated(Content content);

    void publishContentStatusChanged(Content content, String oldStatus, String newStatus);

    void publishContentDeleted(Content content);
}
