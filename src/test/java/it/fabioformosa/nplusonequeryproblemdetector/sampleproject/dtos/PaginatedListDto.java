package it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@ToString
public class PaginatedListDto<T> {

    @Builder.Default
    public List<T> items = new ArrayList<>();

    public int totalPages;
    public long totalItems;
}
