package org.pspace.common.web.services;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface ConvertingPagingAndSortingRepository<T> extends
        PagingAndSortingRepository<T, Long>,
        Converter<String, T> {

    @Override
    default T convert(String source) {
        return findOne(Long.parseLong(source));
    }
}
