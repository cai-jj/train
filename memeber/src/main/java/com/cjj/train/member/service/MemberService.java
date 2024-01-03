package com.cjj.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import com.cjj.train.common.exception.BusinessException;
import com.cjj.train.common.exception.BusinessExceptionEnum;
import com.cjj.train.common.util.SnowUtil;
import com.cjj.train.member.domain.Member;
import com.cjj.train.member.domain.MemberExample;
import com.cjj.train.member.mapper.MemberMapper;
import com.cjj.train.member.req.MemberLoginReq;
import com.cjj.train.member.req.MemberRegisterReq;
import com.cjj.train.member.req.MemberSendCodeReq;
import com.cjj.train.member.resp.MemberLoginResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {
    @Autowired
    public MemberMapper memberMapper;

    private static final Logger LOG = LoggerFactory.getLogger(MemberService.class);
    public int count() {
        return Math.toIntExact(memberMapper.countByExample(null));
    }

    public long register(MemberRegisterReq memberRegisterReq) {
        String mobile = memberRegisterReq.getMobile();
        //select * from member where mobile = xxx;
        //构造查询条件，号码不能重复
        Member memberDB = selectByMobile(mobile);

        if(ObjectUtil.isNotNull(memberDB)) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_EXIST);
        }
        Member member = new Member();
        member.setId(SnowUtil.getSnowflakeNextId());
        member.setMobile(mobile);
        memberMapper.insert(member);
        return member.getId();
    }

    public void sendCode(MemberSendCodeReq memberSendCodeReq) {

        String mobile = memberSendCodeReq.getMobile();

        Member memberDB = selectByMobile(mobile);
        if(ObjectUtil.isNull(memberDB)) {
            LOG.info("手机号不存在，插入数据库");
            Member member = new Member();
            member.setId(SnowUtil.getSnowflakeNextId());
            member.setMobile(memberSendCodeReq.getMobile());
            memberMapper.insert(member);
        } else {
            LOG.info("手机号存在,不插入数据库");
        }

//        String code = RandomUtil.randomString(4);
        String code = "8888";
        LOG.info("生成验证码: {}", code);

        // 保存短信记录表：手机号，短信验证码，有效期，是否已使用，业务类型，发送时间，使用时间
        LOG.info("保存短信记录表");

        // 对接短信通道，发送短信
        LOG.info("对接短信通道");
    }

    public MemberLoginResp login(MemberLoginReq memberLoginReq) {

        String mobile = memberLoginReq.getMobile();
        String code = memberLoginReq.getCode();
        Member memberDB = selectByMobile(mobile);
        // 如果手机号不存在，则插入一条记录
        if (ObjectUtil.isNull(memberDB)) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_NOT_EXIST);
        }

        // 校验短信验证码
        if (!"8888".equals(code)) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_CODE_ERROR);
        }
        MemberLoginResp memberLoginResp = BeanUtil.copyProperties(memberDB, MemberLoginResp.class);
        return memberLoginResp;


    }

    private Member selectByMobile(String mobile) {
        MemberExample memberExample = new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);
        List<Member> members = memberMapper.selectByExample(memberExample);
        if(CollUtil.isEmpty(members)) {
            return null;
        } else {
            return members.get(0);
        }
    }


}
