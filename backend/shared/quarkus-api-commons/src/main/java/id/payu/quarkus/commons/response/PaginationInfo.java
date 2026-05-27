package id.payu.quarkus.commons.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationInfo {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private PaginationLinks links;

    public PaginationInfo() {
    }

    public PaginationInfo(int page, int size, long totalElements, int totalPages,
                          boolean hasNext, boolean hasPrevious, PaginationLinks links) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.links = links;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PaginationInfo of(int page, int size, long totalElements, int totalPages) {
        return PaginationInfo.builder()
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
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

    public static class Builder {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
        private PaginationLinks links;

        public Builder page(int page) { this.page = page; return this; }
        public Builder size(int size) { this.size = size; return this; }
        public Builder totalElements(long totalElements) { this.totalElements = totalElements; return this; }
        public Builder totalPages(int totalPages) { this.totalPages = totalPages; return this; }
        public Builder hasNext(boolean hasNext) { this.hasNext = hasNext; return this; }
        public Builder hasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; return this; }
        public Builder links(PaginationLinks links) { this.links = links; return this; }
        public PaginationInfo build() {
            return new PaginationInfo(page, size, totalElements, totalPages, hasNext, hasPrevious, links);
        }
    }
}
