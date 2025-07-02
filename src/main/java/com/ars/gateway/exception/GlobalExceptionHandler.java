package com.ars.gateway.exception;

import com.dct.model.common.MessageTranslationUtils;
import com.dct.model.constants.BaseExceptionConstants;
import com.dct.model.constants.BaseHttpStatusConstants;
import com.dct.model.dto.response.BaseResponseDTO;
import com.dct.model.exception.BaseAuthenticationException;
import com.dct.model.exception.BaseBadRequestException;
import com.dct.model.exception.BaseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Objects;

/**
 * Used to handle exceptions in the application centrally and return consistent responses <p>
 * Provides a standardized and centralized approach to handling common errors in Spring applications <p>
 * Helps log detailed errors, return structured responses, and easily internationalize error messages
 *
 * @author thoaidc
 */
@ControllerAdvice
public abstract class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageTranslationUtils messageUtils;

    protected GlobalExceptionHandler(MessageTranslationUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @ExceptionHandler({ BaseAuthenticationException.class })
    public ResponseEntity<BaseResponseDTO> handleBaseAuthenticationException(BaseAuthenticationException exception) {
        String reason = Objects.nonNull(messageUtils)
                ? messageUtils.getMessageI18n(exception.getErrorKey(), exception.getArgs())
                : exception.getErrorKey();
        log.error("[{}] Handle authentication exception: {}", exception.getEntityName(), reason);

        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.UNAUTHORIZED)
                .success(BaseHttpStatusConstants.STATUS.FAILED)
                .message(exception.getErrorKey())
                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({ BaseBadRequestException.class })
    public ResponseEntity<BaseResponseDTO> handleBaseBadRequestException(BaseBadRequestException exception) {
        String reason = Objects.nonNull(messageUtils)
                ? messageUtils.getMessageI18n(exception.getErrorKey(), exception.getArgs())
                : exception.getErrorKey();
        log.error("[{}] Handle bad request alert exception: {}", exception.getEntityName(), reason);

        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.BAD_REQUEST)
                .success(BaseHttpStatusConstants.STATUS.FAILED)
                .message(exception.getErrorKey())
                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ BaseException.class })
    public ResponseEntity<BaseResponseDTO> handleBaseException(BaseException exception) {
        String reason = Objects.nonNull(messageUtils)
                ? messageUtils.getMessageI18n(exception.getErrorKey(), exception.getArgs())
                : exception.getErrorKey();
        log.error("[{}] Handle exception: {}", exception.getEntityName(), reason, exception.getError());

        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.BAD_REQUEST)
                .success(BaseHttpStatusConstants.STATUS.FAILED)
                .message(exception.getErrorKey())
                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ MaxUploadSizeExceededException.class })
    public ResponseEntity<Object> handleNullPointerException(MaxUploadSizeExceededException e, WebRequest request) {
        log.error("[{}] Maximum upload size exceeded: {}", request.getClass().getName(), e.getMessage());

        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.BAD_REQUEST)
                .success(BaseHttpStatusConstants.STATUS.FAILED)
                .message(BaseExceptionConstants.MAXIMUM_UPLOAD_SIZE_EXCEEDED)
                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ NullPointerException.class })
    public ResponseEntity<Object> handleNullPointerException(NullPointerException exception, WebRequest request) {
        // Handle NullPointerException (include of Objects.requireNonNull())
        log.error("[{}] Null pointer exception occurred: {}", request.getClass().getName(), exception.getMessage());

        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.INTERNAL_SERVER_ERROR)
                .success(BaseHttpStatusConstants.STATUS.FAILED)
                .message(BaseExceptionConstants.NULL_EXCEPTION)
                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ RuntimeException.class })
    public ResponseEntity<BaseResponseDTO> handleRuntimeException(RuntimeException exception) {
        log.error("Handle runtime exception", exception);

        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.INTERNAL_SERVER_ERROR)
                .success(BaseHttpStatusConstants.STATUS.FAILED)
                .message(BaseExceptionConstants.UNCERTAIN_ERROR)
                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ Exception.class })
    public ResponseEntity<BaseResponseDTO> handleException(Exception exception) {
        log.error("Handle unexpected exception", exception);

        BaseResponseDTO responseDTO = BaseResponseDTO.builder()
                .code(BaseHttpStatusConstants.INTERNAL_SERVER_ERROR)
                .success(BaseHttpStatusConstants.STATUS.FAILED)
                .message(BaseExceptionConstants.UNCERTAIN_ERROR)
                .build();

        return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
