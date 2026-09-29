package se.sundsvall.minaarenden.integration.db;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Pageable med fri offset (inte sidnummer), så att fraga.behandling.paginering (offset, limit) kan skickas rakt in i
 * databasfrågan. Sorteringen sätts i specifikationen, inte här.
 *
 * @param offset antal poster att hoppa över
 * @param limit  högsta antal poster att hämta
 */
public record OffsetPageable(long offset, int limit)
	implements
	Pageable {

	public OffsetPageable {
		if (offset < 0) {
			throw new IllegalArgumentException("offset får inte vara negativ");
		}
		if (limit < 1) {
			throw new IllegalArgumentException("limit måste vara minst 1");
		}
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
	public Pageable withPage(final int pageNumber) {
		return new OffsetPageable((long) pageNumber * limit, limit);
	}

	@Override
	public boolean hasPrevious() {
		return offset > 0;
	}
}
