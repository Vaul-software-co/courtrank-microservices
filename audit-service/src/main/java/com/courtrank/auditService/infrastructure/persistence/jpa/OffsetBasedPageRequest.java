package com.courtrank.auditService.infrastructure.persistence.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetBasedPageRequest implements Pageable {
    private final int limit;
    private final long offset;
    private final Sort sort;

    public OffsetBasedPageRequest(int limit, long offset) {
        this(limit, offset, Sort.unsorted());
    }

    public OffsetBasedPageRequest(int limit, long offset, Sort sort) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be negative");
        }
        this.limit = limit;
        this.offset = offset;
        this.sort = sort == null ? Sort.unsorted() : sort;
    }

    @Override
    public int getPageNumber() {
        return Math.toIntExact(this.offset / this.limit);
    }

    @Override
    public int getPageSize() {
        return this.limit;
    }

    @Override
    public long getOffset() {
        return this.offset;
    }

    @Override
    public Sort getSort() {
        return this.sort;
    }

    @Override
    public Pageable next() {
        return new OffsetBasedPageRequest(this.limit, this.offset + this.limit, this.sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetBasedPageRequest(this.limit, this.offset - this.limit, this.sort) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetBasedPageRequest(this.limit, 0, this.sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetBasedPageRequest(this.limit, (long) pageNumber * this.limit, this.sort);
    }

    @Override
    public boolean hasPrevious() {
        return this.offset > 0;
    }
}
