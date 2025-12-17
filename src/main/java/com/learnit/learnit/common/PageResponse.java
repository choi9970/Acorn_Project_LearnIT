package com.learnit.learnit.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
//PageResponse.java (Spring Data 없이 Page 형태 내려주는 DTO)
@Getter
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}