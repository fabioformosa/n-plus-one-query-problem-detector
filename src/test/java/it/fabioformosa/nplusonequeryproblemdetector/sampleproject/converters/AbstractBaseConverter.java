package it.fabioformosa.nplusonequeryproblemdetector.sampleproject.converters;

import it.fabioformosa.nplusonequeryproblemdetector.sampleproject.dtos.PaginatedListDto;
import org.springframework.data.domain.Page;

import java.util.function.Function;

@SuppressWarnings("java:S119")
public class AbstractBaseConverter {
    public static <ENTITY, DTO> PaginatedListDto<DTO> fromPageToPaginatedListDto(Page<ENTITY> page, Function<ENTITY, DTO> conversion){
        return PaginatedListDto.<DTO>builder()
                .items(page.getContent().stream().map(conversion).toList())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements())
                .build();
    }
}
