package devPilot.backend.exceptions;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException exception) {
		return errorResponse(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException exception) {
		return errorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException exception) {
		return errorResponse(HttpStatus.UNAUTHORIZED, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
		Map<String, String> validationErrors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}

		Map<String, Object> response = errorBody(HttpStatus.BAD_REQUEST, "Validation failed");
		response.put("validationErrors", validationErrors);
		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException exception) {
		return errorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
		return errorResponse(HttpStatus.BAD_REQUEST, "Malformed request body");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGeneric(Exception exception) {
		return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(errorBody(status, message));
	}

	private Map<String, Object> errorBody(HttpStatus status, String message) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("timestamp", LocalDateTime.now());
		response.put("status", status.value());
		response.put("error", status.getReasonPhrase());
		response.put("message", message == null ? status.getReasonPhrase() : message);
		return response;
	}
}
