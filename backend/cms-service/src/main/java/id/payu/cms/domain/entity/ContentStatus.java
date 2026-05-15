package id.payu.cms.domain.entity;

/**
 * Content status enum representing the lifecycle states of CMS content.
 * <p>
 * State transitions: DRAFT -> SCHEDULED -> ACTIVE -> ARCHIVED
 *                    DRAFT -> ACTIVE (immediate activation)
 *                    ACTIVE -> PAUSED -> ACTIVE (temporary pause)
 *                    ACTIVE -> ARCHIVED (end of life)
 */
public enum ContentStatus {
    /** Not yet active - initial state for new content */
    DRAFT,
    /** Scheduled for future activation */
    SCHEDULED,
    /** Currently visible and within date range */
    ACTIVE,
    /** Temporarily disabled but may be reactivated */
    PAUSED,
    /** No longer in use - end of life */
    ARCHIVED
}
