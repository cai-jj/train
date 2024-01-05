package com.cjj.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.cjj.train.common.context.LoginMemberContext;
import com.cjj.train.common.resp.PageResp;
import com.cjj.train.common.util.SnowUtil;
import com.cjj.train.member.domain.Passenger;
import com.cjj.train.member.domain.PassengerExample;
import com.cjj.train.member.mapper.PassengerMapper;
import com.cjj.train.member.req.PassengerQueryReq;
import com.cjj.train.member.req.PassengerSaveReq;
import com.cjj.train.member.resp.PassengerQueryResp;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PassengerService {

    private static final Logger LOG = LoggerFactory.getLogger(PassengerService.class);

    @Autowired
    private PassengerMapper passengerMapper;

    public void save(PassengerSaveReq passengerSaveReq) {

        Passenger passenger = BeanUtil.copyProperties(passengerSaveReq, Passenger.class);
        DateTime now = DateTime.now();
        //id为空，新增保存
        if(ObjectUtil.isNull(passenger.getId())) {
            passenger.setMemberId(LoginMemberContext.getId());
            //雪花算法生成id
            passenger.setId(SnowUtil.getSnowflakeNextId());
            passenger.setCreateTime(now);
            passenger.setUpdateTime(now);
            passengerMapper.insert(passenger);
        } else {
            //id不为空，编辑保存
            passenger.setUpdateTime(now);
            passengerMapper.updateByPrimaryKey(passenger);
        }

    }

    public PageResp<PassengerQueryResp> queryList(PassengerQueryReq passengerQueryReq) {
        Long memberId = passengerQueryReq.getMemberId();
        PassengerExample passengerExample = new PassengerExample();
        PassengerExample.Criteria criteria = passengerExample.createCriteria();
        if(ObjectUtil.isNotNull(memberId)) {
            criteria.andMemberIdEqualTo(memberId);
        }
        LOG.info("查询页码：{}", passengerQueryReq.getPage());
        LOG.info("每页条数：{}", passengerQueryReq.getSize());
        PageHelper.startPage(passengerQueryReq.getPage(), passengerQueryReq.getSize());
//        PageHelper.startPage(1, 2);
        List<Passenger> passengerList = passengerMapper.selectByExample(passengerExample);

        PageInfo<Passenger> pageInfo = new PageInfo<>(passengerList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<PassengerQueryResp> list = BeanUtil.copyToList(passengerList, PassengerQueryResp.class);
        PageResp<PassengerQueryResp> pageResp = new PageResp<>();
        pageResp.setList(list);
        pageResp.setTotal(pageInfo.getTotal());
        return pageResp;
    }

    public void deleteById(Long id) {
        passengerMapper.deleteByPrimaryKey(id);
    }
}
