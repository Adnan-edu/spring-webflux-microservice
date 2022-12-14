package com.reactivespring.controller;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.service.MoviesInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1")
@Slf4j
public class MoviesInfoController {

    private MoviesInfoService moviesInfoService;

    Sinks.Many<MovieInfo> moviesInfoSink = Sinks.many().replay().all();

    public MoviesInfoController(MoviesInfoService moviesInfoService) {
        this.moviesInfoService = moviesInfoService;
    }

    @GetMapping("/movieinfos")
    public Flux<MovieInfo> getAllMovieInfos(@RequestParam(value = "year", required = false) Integer year, @RequestParam(value = "name", required = false) String name){
        log.info("Year is : {} ", year);
        if(year!=null){
            return moviesInfoService.getMovieInfoByYear(year);
        }else if(name!=null){
            return moviesInfoService.getMovieInfoByName(name);
        }
        return moviesInfoService.getAllMovieInfos().log();
    }

    @GetMapping("/movieinfos/{id}")
    public Mono<ResponseEntity<MovieInfo>> getMovieInfoById(@PathVariable String id){

        return moviesInfoService.getMovieInfoById(id)
                    .map(ResponseEntity.ok()::body)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PutMapping("/movieinfos/{id}")
    public Mono<ResponseEntity<MovieInfo>> updateMovieInfo(@RequestBody MovieInfo updatedMovieInfo, @PathVariable String id){
        return moviesInfoService.updateMovieInfo(updatedMovieInfo, id)
                .map(ResponseEntity.ok()::body)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build())).log();//This should give you the response as not found if the passed in movie id is an invalid one.
    }

    @DeleteMapping("/movieinfos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteMovieInfo(@PathVariable String id){
        return moviesInfoService.deleteMovieInfo(id);
    }

    @GetMapping(value = "/movieinfos/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<MovieInfo> getMovieInfoById(){
        //Here we are attaching the subscriber
        return moviesInfoSink.asFlux();
    }

    @PostMapping("/movieinfos")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MovieInfo> addMovieInfo(@RequestBody @Valid MovieInfo movieInfo){
        return moviesInfoService.addMovieInfo(movieInfo)
                .doOnNext(savedInfo->moviesInfoSink.tryEmitNext(savedInfo)); //Here we are publishing to the sync
    }
}
