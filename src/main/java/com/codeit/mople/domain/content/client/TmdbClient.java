package com.codeit.mople.domain.content.client;

import com.codeit.mople.domain.content.client.dto.TmdbGenreListResponse;
import com.codeit.mople.domain.content.client.dto.TmdbMovieResponse;
import com.codeit.mople.domain.content.client.dto.TmdbPageResponse;
import com.codeit.mople.domain.content.client.dto.TmdbTvResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "tmdb",
    url = "${external-api.tmdb.base-url}",
    configuration = TmdbFeignConfig.class)
public interface TmdbClient {

  @GetMapping("/movie/popular")
  TmdbPageResponse<TmdbMovieResponse> getPopularMovies(@RequestParam("page") int page);

  @GetMapping("/tv/popular")
  TmdbPageResponse<TmdbTvResponse> getPopularTvSeries(@RequestParam("page") int page);

  @GetMapping("/genre/movie/list")
  TmdbGenreListResponse getMovieGenres();

  @GetMapping("/genre/tv/list")
  TmdbGenreListResponse getTvGenres();

}
