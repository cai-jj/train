package com.cjj.train.member.controller;

import com.cjj.train.common.resp.CommonResp;
import com.cjj.train.member.req.MemberRegisterReq;
import com.cjj.train.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberController {

    @Autowired
    public MemberService memberService;
    @GetMapping("/count")
    public CommonResp<Integer> count() {
        CommonResp<Integer> commonResp = new CommonResp<>();
        int count = memberService.count();
        commonResp.setContent(count);
        return commonResp;
    }

    //注册用户
    @PostMapping("/register")
    public CommonResp<Long> register(@Valid MemberRegisterReq memberRegisterReq) {
//        long mobile = memberService.register(memberRegisterReq);
//        CommonResp<Long> commonResp = new CommonResp<>();
//        commonResp.setContent(mobile);
//        return commonResp;
        return new CommonResp<Long>(memberService.register(memberRegisterReq));
    }
}
