package com.cjj.train.member.controller;

import com.cjj.train.common.resp.CommonResp;
import com.cjj.train.member.req.MemberLoginReq;
import com.cjj.train.member.req.MemberRegisterReq;
import com.cjj.train.member.req.MemberSendCodeReq;
import com.cjj.train.member.resp.MemberLoginResp;
import com.cjj.train.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
    public CommonResp<Long> register(@Valid @RequestBody MemberRegisterReq memberRegisterReq) {
//        long mobile = memberService.register(memberRegisterReq);
//        CommonResp<Long> commonResp = new CommonResp<>();
//        commonResp.setContent(mobile);
//        return commonResp;
        return new CommonResp<Long>(memberService.register(memberRegisterReq));
    }

    //发送验证码
    @PostMapping("/sendCode")
    public CommonResp<Long> register(@Valid @RequestBody MemberSendCodeReq memberSendCodeReq) {
        memberService.sendCode(memberSendCodeReq);
        return new CommonResp<Long>();
    }

    //登录
    @PostMapping("/login")
    public CommonResp<MemberLoginResp> login(@Valid @RequestBody MemberLoginReq memberLoginReq) {
        MemberLoginResp resp = memberService.login(memberLoginReq);
        return new CommonResp<MemberLoginResp>(resp);
    }
}
