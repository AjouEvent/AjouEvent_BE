package com.example.ajouevent.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
public class SliceResponse<T> {

	protected List<T> result;
	protected boolean hasPrevious;
	protected boolean hasNext;
	protected Integer currentPage;
	protected SortResponse sort;

	public SliceResponse(List<T> result, boolean hasPrevious, boolean hasNext, Integer currentPage, SortResponse sort) {
		this.hasPrevious = hasPrevious;
		this.hasNext = hasNext;
		this.result = result;
		this.currentPage = currentPage;
		this.sort = sort;
	}

	@Getter
	@Builder
	public static class SortResponse {
		protected boolean sorted;
		protected String direction;
		protected String orderProperty;
	}
}
