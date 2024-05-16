package com.example.ajouevent.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Date;

import com.example.ajouevent.dto.ExceptionResponse;

@RestController
@ControllerAdvice
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

	//Default exception
	@ExceptionHandler(Exception.class)
	public final ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
		ExceptionResponse exceptionResponse = new ExceptionResponse().builder()
			.timestamp(new Date())
			.message(ex.getMessage())
			.details(request.getDescription(false))
			.build();
		return new ResponseEntity(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);

	}

	@ExceptionHandler(UserNotFoundException.class)
	public final ResponseEntity<Object> handleUserNotFoundExceptions(Exception ex, WebRequest request) {

		ExceptionResponse exceptionResponse = new ExceptionResponse().builder()
			.timestamp(new Date())
			.message(ex.getMessage())
			.details(request.getDescription(false))
			.build();
		return new ResponseEntity(exceptionResponse, HttpStatus.NOT_FOUND);

	}

	public final ResponseEntity<Object> handleMethodArgumentNotValid(
		MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
		ExceptionResponse exceptionResponse = new ExceptionResponse().builder()
			.timestamp(new Date())
			.message(ex.getMessage())
			.details(ex.getBindingResult().toString())
			.build();
		return new ResponseEntity(exceptionResponse, HttpStatus.BAD_REQUEST);
	}

}