package edu.neu.csye7255.healthcare.controller;

import edu.neu.csye7255.healthcare.model.LoginResponseDTO;
import edu.neu.csye7255.healthcare.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    @Autowired
    LoginService loginService;
//    @PostMapping("/login")
//    public LoginResponseDTO login(@RequestParam String username, @RequestParam String password) {
//        return loginHelper.login(username, password);
//    }

    @GetMapping("/grantcode")
    public LoginResponseDTO grantCode(@RequestParam("code") String code, @RequestParam("scope") String scope, @RequestParam("authuser") String authUser, @RequestParam("prompt") String prompt) {
        return loginService.processGrantCode(code);
    }

//    http://localhost:8080/api/v1/auth/grantcode
}
