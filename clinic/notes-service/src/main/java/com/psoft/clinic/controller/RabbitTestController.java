package com.psoft.clinic.controller;

import com.psoft.clinic.config.RabbitMQConfig;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.web.bind.annotation.*;

////@RestController
//@RequestMapping("/rabbit")
//public class RabbitTestController {

//  private final RabbitTemplate rabbitTemplate;

//  public RabbitTestController(RabbitTemplate rabbitTemplate) {
//      this.rabbitTemplate = rabbitTemplate;
//  }

//  @GetMapping("/test")
//  public String sendTestMessage() {
//    rabbitTemplate.convertAndSend(
//            RabbitMQConfig.EXCHANGE_NOTES,
//            "notes.test",
//             "Olá notes-service, isto é um teste!"
//     );
//      return "Mensagem enviada!";
                // }
//}
