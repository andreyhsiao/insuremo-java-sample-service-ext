package com.ebao.cloud.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @USER: jan.zhang
 * @DATE: 2021/11/10 12:48
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/ok")
    public String test() {
        return "ok";
    }
}
