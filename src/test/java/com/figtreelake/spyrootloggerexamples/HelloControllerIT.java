package com.figtreelake.spyrootloggerexamples;


import ch.qos.logback.classic.Level;
import com.figtreelake.spyrootlogger.SpyRootLogger;
import com.figtreelake.spyrootlogger.SpyRootLoggerExtension;
import com.figtreelake.spyrootlogger.SpyRootLoggerInject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebClient
@ExtendWith(SpyRootLoggerExtension.class)
class HelloControllerIT {

  @Autowired
  private WebTestClient webTestClient;

  @MockBean
  private HelloService helloService;

  @SpyRootLoggerInject
  private SpyRootLogger spyRootLogger;

  @Test
  void shouldReturnHttpStatusOkAndMessageOnBody() {

    final var name = "MarceloLeite2604";
    final var expected = "Hello, " + name;

    when(helloService.elaborateGreeting(any())).thenCallRealMethod();

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("hello")
            .queryParam("name", name)
            .build())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo(expected);
  }

  @Test
  void shouldReturnHttpStatusBadRequestAndLogWarningEventWhenRequestDoesNotHaveMandatoryParameter() {

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("hello")
            .build())
        .exchange()
        .expectStatus()
        .isBadRequest();

    /* Spy Root Logger can be used to count events of different types. */
    assertThat(spyRootLogger.countWarningEvents()).isPositive();
  }

  @Test
  void shouldReturnHttpStatusBadRequestAndLogErrorEventWithDangerousContentExceptionWhenReceivingContentWithNonAlphanumericCharacters() {
    final var name = "<div>Malicious content</div>";

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("hello")
            .queryParam("name", name)
            .build())
        .exchange()
        .expectStatus()
        .isBadRequest();

    /* Spy Root Logger can be used to retrieve all events which have a specific
    type of throwable attached. */
    final var events = spyRootLogger.findEventsByInstanceOfThrowableAttached(DangerousContentException.class);

    /* A log event contains the level, message and the throwable attached to it
    (if any). These fields can be used to analyse and create assertions. */
    assertThat(events).isNotEmpty()
        .allMatch(event -> Level.ERROR.equals(event.getLevel()));
  }

  @Test
  void shouldReturnHttpStatusInternalServerErrorAndLogErrorEventWithCapturedExceptionWhenAnUnknownExceptionOccurs() {
    final var name = "MarceloLeite2604";

    final var mockedIllegalStateException = mock(IllegalStateException.class);

    doThrow(mockedIllegalStateException).when(helloService)
        .elaborateGreeting(any());

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("hello")
            .queryParam("name", name)
            .build())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

    /* Spy Root Logger can also find events with a specific exception attached. */
    final var events = spyRootLogger.findEventsByThrowableAttached(mockedIllegalStateException);
    assertThat(events).isNotEmpty()
        .allMatch(event -> Level.ERROR.equals(event.getLevel()));
  }

  @SpringBootApplication
  @EnableAutoConfiguration
  @ComponentScan
  public static class ITConfig {
  }
}