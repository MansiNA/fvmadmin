package com.example.application.views;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeController {

 //   @Value("${export.tables}")
 //   private String Tt;

    @GetMapping("/application")
    private String getApplicationName(){
        return "Huhu";
    }

}
