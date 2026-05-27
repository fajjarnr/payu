package id.payu.quarkus.commons.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationLinks {

    private String self;
    private String first;
    private String last;
    private String next;
    private String prev;

    public PaginationLinks() {
    }

    public PaginationLinks(String self, String first, String last, String next, String prev) {
        this.self = self;
        this.first = first;
        this.last = last;
        this.next = next;
        this.prev = prev;
    }

    public static Builder builder() {
        return new Builder();
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

    public static class Builder {
        private String self;
        private String first;
        private String last;
        private String next;
        private String prev;

        public Builder self(String self) { this.self = self; return this; }
        public Builder first(String first) { this.first = first; return this; }
        public Builder last(String last) { this.last = last; return this; }
        public Builder next(String next) { this.next = next; return this; }
        public Builder prev(String prev) { this.prev = prev; return this; }
        public PaginationLinks build() { return new PaginationLinks(self, first, last, next, prev); }
    }
}
