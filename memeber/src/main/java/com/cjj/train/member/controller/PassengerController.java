package com.cjj.train.member.controller;

import com.cjj.train.common.context.LoginMemberContext;
import com.cjj.train.common.resp.CommonResp;
import com.cjj.train.common.resp.PageResp;
import com.cjj.train.member.req.PassengerQueryReq;
import com.cjj.train.member.req.PassengerSaveReq;
import com.cjj.train.member.resp.PassengerQueryResp;
import com.cjj.train.member.service.PassengerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/passenger")
public class PassengerController {

    @Autowired
    private PassengerService passengerService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody PassengerSaveReq passengerSaveReq) {
        passengerService.save(passengerSaveReq);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp> queryList(@Valid PassengerQueryReq passengerQueryReq) {
        passengerQueryReq.setMemberId(LoginMemberContext.getId());
        PageResp<PassengerQueryResp> list = passengerService.queryList(passengerQueryReq);
        return new CommonResp<>(list);
    }
}
