package com.github.grayalert.http;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface GraylogClient {
    @RequestLine("GET /api/search/universal/relative/export?fields={fields}&query={query}&range={range}")
    @Headers("Accept: text/csv")
    String getLogs(@Param("fields") String fields, @Param("query") String query, @Param("range") int range);
}