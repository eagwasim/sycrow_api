package com.sycrow.api.model.ext;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.domain.Sort;

import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.io.Serializable;

public class OffsetPageRequest implements Pageable, Serializable {
    private static final long serialVersionUID = -25822477129613575L;

    private int limit;
    private int offset;
    private final Sort sort;

    protected OffsetPageRequest(int offset, int limit, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset index must not be less than zero!");
        }

        if (limit < 1) {
            throw new IllegalArgumentException("Limit must not be less than one!");
        }
        this.limit = limit;
        this.offset = offset;
        this.sort = sort;
    }

    protected OffsetPageRequest(int offset, int limit, Sort.Direction direction, String... properties) {
        this(offset, limit, Sort.by(direction, properties));
    }
    protected OffsetPageRequest(int offset, int limit) {
        this(offset, limit, Sort.unsorted());
    }

    public static OffsetPageRequest of(int offset, int limit, Sort sort){
        return new OffsetPageRequest(offset, limit, sort);
    }

    public static OffsetPageRequest of(int offset, int limit, Sort.Direction direction, String... properties){
        return new OffsetPageRequest(offset, limit, direction, properties);
    }

    public static OffsetPageRequest of(int offset, int limit) {
        return new OffsetPageRequest (offset, limit, Sort.unsorted());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof OffsetPageRequest)) return false;

        OffsetPageRequest that = (OffsetPageRequest) o;

        return new EqualsBuilder()
                .append(limit, that.limit)
                .append(offset, that.offset)
                .append(sort, that.sort)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(limit)
                .append(offset)
                .append(sort)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("limit", limit)
                .append("offset", offset)
                .append("sort", sort)
                .toString();
    }

    @Override
    public int getNumberOfPages() {
        return 0;
    }

    @Override
    public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException {
        return null;
    }

    @Override
    public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException {
        return null;
    }
}
