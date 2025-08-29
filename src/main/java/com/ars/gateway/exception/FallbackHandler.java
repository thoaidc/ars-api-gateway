package com.ars.gateway.exception;

import com.dct.model.dto.response.BaseResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackHandler {

    private final Logger log = LoggerFactory.getLogger(FallbackHandler.class);

    @GetMapping(value = "/services/unavailable", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<BaseResponseDTO>> serviceUnavailableJson() {
        log.error("[SERVICE_UNAVAILABLE] - Service unavailable");
        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message("Service temporarily unavailable. Please try again later")
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(responseDTO));
    }
}
