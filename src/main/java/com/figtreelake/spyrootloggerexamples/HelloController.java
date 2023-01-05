package com.figtreelake.spyrootloggerexamples;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.MissingRequestValueException;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@RestController
@RequestMapping("hello")
@Slf4j
@RequiredArgsConstructor
public class HelloController {

  private static final Predicate<String> NON_ALPHANUMERIC_MATCH_PREDICATE = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE).asPredicate();

  private final HelloService helloService;

  @GetMapping
  public String get(@RequestParam String name) {
    if (NON_ALPHANUMERIC_MATCH_PREDICATE.test(name)) {
      throw new DangerousContentException("Received non-alphanumeric characters while handling request.");
    }

    return helloService.elaborateGreeting(name);
  }

  @ExceptionHandler(MissingRequestValueException.class)
  private ResponseEntity<ProblemDetail> handleMissingRequestValueException(MissingRequestValueException exception) {
    log.warn("Received a request with missing value.", exception);
    return ResponseEntity.badRequest()
        .body(exception.getBody());
  }

  @ExceptionHandler(DangerousContentException.class)
  private ResponseEntity<String> handleDangerousContentException(DangerousContentException exception) {
    log.error("Received a request with with dangerous content.", exception);
    return ResponseEntity.badRequest()
        .body("The content received is considered dangerous and will not be accepted.");
  }

  @ExceptionHandler(Exception.class)
  private ResponseEntity<String> handleException(Exception exception) {
    log.error("The following unmapped exception was thrown while handling a request.", exception);

    return ResponseEntity.internalServerError()
        .body("Something went wrong while processing your request.");
  }
}
