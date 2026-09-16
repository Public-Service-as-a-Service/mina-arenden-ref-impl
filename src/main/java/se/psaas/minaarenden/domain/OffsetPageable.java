package se.psaas.minaarenden.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Pageable med fri offset (inte sidnummer), så att fraga.behandling.paginering (offset, limit) kan
 * skickas rakt in i databasfrågan. Sorteringen sätts i specifikationen, inte här.
 */
public final class OffsetPageable implements Pageable {

    private final long offset;
    private final int limit;

    public OffsetPageable(long offset, int limit) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset får inte vara negativ");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit måste vara minst 1");
        }
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }

    @Override
    public Pageable next() {
        return new OffsetPageable(offset + limit, limit);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetPageable(Math.max(0, offset - limit), limit) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageable(0, limit);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageable((long) pageNumber * limit, limit);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
