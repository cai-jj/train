package com.cjj.train.member.controller;

import com.cjj.train.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberController {

    @Autowired
    public MemberService memberService;
    @GetMapping("/count")
    public Integer count() {
        return memberService.count();
    }
}
