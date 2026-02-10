package id.payu.partner.adapter.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Pagination information for list responses.
 * Provides metadata about paginated results and navigation links.
 */
@Schema(description = "Pagination information for list responses")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationInfo {

    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;

    @Schema(description = "Number of items per page", example = "20")
    private int size;

    @Schema(description = "Total number of elements", example = "150")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "8")
    private int totalPages;

    @Schema(description = "Whether there is a next page", example = "true")
    private boolean hasNext;

    @Schema(description = "Whether there is a previous page", example = "false")
    private boolean hasPrevious;

    @Schema(description = "Navigation links for pagination")
    private PaginationLinks links;

    public PaginationInfo() {}

    public PaginationInfo(int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious, PaginationLinks links) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.links = links;
    }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
    public boolean isHasPrevious() { return hasPrevious; }
    public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    public PaginationLinks getLinks() { return links; }
    public void setLinks(PaginationLinks links) { this.links = links; }

    // Static factory methods for convenience
    public static PaginationInfo of(int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PaginationInfo(page, size, totalElements, totalPages,
            page < totalPages - 1, page > 0, null);
    }

    /**
     * Pagination links for navigation.
     */
    @Schema(description = "Navigation links for pagination")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationLinks {

        @Schema(description = "URL for the current page", example = "/partners?page=0&size=20")
        private String self;

        @Schema(description = "URL for the first page", example = "/partners?page=0&size=20")
        private String first;

        @Schema(description = "URL for the last page", example = "/partners?page=7&size=20")
        private String last;

        @Schema(description = "URL for the next page (if available)", example = "/partners?page=1&size=20")
        private String next;

        @Schema(description = "URL for the previous page (if available)", example = "/partners?page=0&size=20")
        private String prev;

        public PaginationLinks() {}

        public PaginationLinks(String self, String first, String last, String next, String prev) {
            this.self = self;
            this.first = first;
            this.last = last;
            this.next = next;
            this.prev = prev;
        }

        public String getSelf() { return self; }
        public void setSelf(String self) { this.self = self; }
        public String getFirst() { return first; }
        public void setFirst(String first) { this.first = first; }
        public String getLast() { return last; }
        public void setLast(String last) { this.last = last; }
        public String getNext() { return next; }
        public void setNext(String next) { this.next = next; }
        public String getPrev() { return prev; }
        public void setPrev(String prev) { this.prev = prev; }
    }
}
