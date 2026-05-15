package id.payu.cms.domain.port.out;

import id.payu.cms.adapter.persistence.entity.ContentEntity;

/**
 * Outbound port for Content event publishing.
 */
public interface ContentEventPort {

    void publishContentCreated(ContentEntity content);

    void publishContentUpdated(ContentEntity content);

    void publishContentStatusChanged(ContentEntity content, String oldStatus, String newStatus);

    void publishContentDeleted(ContentEntity content);
}
