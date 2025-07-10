package com.niclauscott.jetdrive.common.exception;

import com.niclauscott.jetdrive.auth_feature.exception.BadCredentialsException;
import com.niclauscott.jetdrive.auth_feature.exception.InvalidOrExpiredTokenException;
import com.niclauscott.jetdrive.file_feature.common.exception.AudioMetaDataExtractionException;
import com.niclauscott.jetdrive.file_feature.common.exception.CantUploadFileException;
import com.niclauscott.jetdrive.file_feature.common.exception.FileNotFoundException;
import com.niclauscott.jetdrive.file_feature.upload.exception.UncompletedUploadException;
import com.niclauscott.jetdrive.file_feature.upload.exception.UploadNotSupportedException;
import com.niclauscott.jetdrive.file_feature.upload.exception.UploadSessionNotFoundException;
import com.niclauscott.jetdrive.user_feature.exception.UserAlreadyExistException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CantUploadFileException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleCantUploadFileException(CantUploadFileException e) {
        Map<String, String> error = Map.of("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AudioMetaDataExtractionException.class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Map<String, String>> handleAudioMetaDataExtractionException(
            AudioMetaDataExtractionException e)
    {
        Map<String, String> error = Map.of("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NO_CONTENT);
    }


    @ExceptionHandler(FileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, String>> handleFileNotFoundException(FileNotFoundException e) {
        Map<String, String> error = Map.of("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UncompletedUploadException.class)
    @ResponseStatus(HttpStatus.PARTIAL_CONTENT)
    public ResponseEntity<Map<String, String>> handleUncompletedUploadException(UncompletedUploadException e) {
        Map<String, String> error = Map.of("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.PARTIAL_CONTENT);
    }

    @ExceptionHandler(UploadNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleUploadNotSupportedException(UploadNotSupportedException e) {
        Map<String, String> error = Map.of("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UploadSessionNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleUploadSessionNotFoundException(UploadSessionNotFoundException e) {
        Map<String, String> error = Map.of("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleBadCredentialsException(BadCredentialsException e) {
        Map<String, String> error = Map.of("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAlreadyExistException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExistException(UserAlreadyExistException e) {
        Map<String, String> error = Map.of("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidOrExpiredTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<Map<String, String>> handleInvalidOrExpiredJwtTokenException(InvalidOrExpiredTokenException e) {
        Map<String, String> error = Map.of("message", e.getMessage());
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach((k) -> errors.put(k.getField(), k.getDefaultMessage()));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }


}
