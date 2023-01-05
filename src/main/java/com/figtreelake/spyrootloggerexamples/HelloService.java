package com.figtreelake.spyrootloggerexamples;

import org.springframework.stereotype.Service;

@Service
public class HelloService {

  public String elaborateGreeting(String name) {
    return "Hello, " + name;
  }
}
