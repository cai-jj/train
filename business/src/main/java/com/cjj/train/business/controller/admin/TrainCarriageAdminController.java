package com.cjj.train.business.controller.admin;

import com.cjj.train.business.req.TrainCarriageQueryReq;
import com.cjj.train.business.req.TrainCarriageSaveReq;
import com.cjj.train.business.resp.TrainCarriageQueryResp;
import com.cjj.train.business.service.TrainCarriageService;
import com.cjj.train.common.resp.CommonResp;
import com.cjj.train.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/train-carriage")
public class TrainCarriageAdminController {

    @Resource
    private TrainCarriageService trainCarriageService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody TrainCarriageSaveReq req) {
        trainCarriageService.save(req);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<TrainCarriageQueryResp>> queryList(@Valid TrainCarriageQueryReq req) {
        PageResp<TrainCarriageQueryResp> list = trainCarriageService.queryList(req);
        return new CommonResp<>(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id) {
        trainCarriageService.delete(id);
        return new CommonResp<>();
    }

}
