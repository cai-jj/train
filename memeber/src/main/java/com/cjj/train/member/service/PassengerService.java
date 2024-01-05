package com.cjj.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.cjj.train.common.context.LoginMemberContext;
import com.cjj.train.common.util.SnowUtil;
import com.cjj.train.member.domain.Passenger;
import com.cjj.train.member.domain.PassengerExample;
import com.cjj.train.member.mapper.PassengerMapper;
import com.cjj.train.member.req.PassengerQueryReq;
import com.cjj.train.member.req.PassengerSaveReq;
import com.cjj.train.member.resp.PassengerQueryResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PassengerService {

    @Autowired
    private PassengerMapper passengerMapper;

    public void save(PassengerSaveReq passengerSaveReq) {
        Passenger passenger = BeanUtil.copyProperties(passengerSaveReq, Passenger.class);
        DateTime now = DateTime.now();
        passenger.setMemberId(LoginMemberContext.getId());
        //雪花算法生成id
        passenger.setId(SnowUtil.getSnowflakeNextId());
        passenger.setCreateTime(now);
        passenger.setUpdateTime(now);
        passengerMapper.insert(passenger);
    }

    public List<PassengerQueryResp> queryList(PassengerQueryReq passengerQueryReq) {
        Long memberId = passengerQueryReq.getMemberId();
        PassengerExample passengerExample = new PassengerExample();
        PassengerExample.Criteria criteria = passengerExample.createCriteria();
        if(ObjectUtil.isNotNull(memberId)) {
            criteria.andMemberIdEqualTo(memberId);
        }
        List<Passenger> passengerList = passengerMapper.selectByExample(passengerExample);
        List<PassengerQueryResp> list = BeanUtil.copyToList(passengerList, PassengerQueryResp.class);
        return list;
    }
}
