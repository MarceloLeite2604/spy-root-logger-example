package com.figtreelake.spyrootloggerexamples;


import ch.qos.logback.classic.Level;
import com.figtreelake.spyrootlogger.SpyRootLogger;
import com.figtreelake.spyrootlogger.SpyRootLoggerExtension;
import com.figtreelake.spyrootlogger.SpyRootLoggerInject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

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

    final var events = spyRootLogger.findEventsByInstanceOfThrowableAttached(DangerousContentException.class);
    assertThat(events).isNotEmpty()
        .allMatch(event -> Level.ERROR.equals(event.getLevel()));
  }

  @Test
  void shouldReturnHttpStatusInternalServerErrorAndLogErrorEventWithCapturedExceptionWhenAnUnknownExceptionOccurs() {
    final var name = "MarceloLeite2604";

    final var exceptionClass = IllegalStateException.class;

    doThrow(exceptionClass).when(helloService)
        .elaborateGreeting(any());

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("hello")
            .queryParam("name", name)
            .build())
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

    final var events = spyRootLogger.findEventsByInstanceOfThrowableAttached(exceptionClass);
    assertThat(events).isNotEmpty()
        .allMatch(event -> Level.ERROR.equals(event.getLevel()));
  }

}