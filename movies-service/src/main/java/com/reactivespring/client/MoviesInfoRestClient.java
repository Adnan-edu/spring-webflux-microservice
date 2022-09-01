package com.reactivespring.client;

import com.reactivespring.domain.MovieInfo;
import com.reactivespring.exception.MoviesInfoClientException;
import com.reactivespring.exception.MoviesInfoServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MoviesInfoRestClient {
    private WebClient webClient;

    @Value("${restClient.moviesInfoUrl}")
    private String moviesInfoUrl;

    public MoviesInfoRestClient(WebClient webClient) {
        this.webClient = webClient;
    }
    public Mono<MovieInfo> retrieveMovieInfo(String movieId){
        var url = moviesInfoUrl.concat("/{id}");
        //If you want to access error status onStatus()
        //And then you can implement your custom error handling logic here use onStatus

        return webClient
                .get()
                .uri(url, movieId)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> { //Any kind of 4xx error is going to catch by this particular predicate
                    log.info("Status code is : {}",clientResponse.statusCode().value());
                    if(clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)){
                        return Mono.error(new MoviesInfoClientException(
                                "There is no MovieInfo Available for the passed in Id : "+movieId, clientResponse.statusCode().value()
                        ));
                    }
                    //Here it is accessing the actual error message from the body of the http response
                    //And throw Mono related exception
                    return clientResponse.bodyToMono(String.class).flatMap(
                      responseMessage -> Mono.error(new MoviesInfoClientException(responseMessage, clientResponse.statusCode().value()))
                    );
                })
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> { //Any kind of 4xx error is going to catch by this particular predicate
                    log.info("Status code is : {}",clientResponse.statusCode().value());
                    return clientResponse.bodyToMono(String.class).flatMap(
                            responseMessage -> Mono.error(new MoviesInfoServerException(
                                    "Server Exception in MoviesInfoService " +responseMessage))
                    );
                })
                .bodyToMono(MovieInfo.class)
                .log();
    }
}
