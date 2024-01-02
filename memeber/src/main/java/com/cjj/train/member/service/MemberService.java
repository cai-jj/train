package com.cjj.train.member.service;

import cn.hutool.core.collection.CollUtil;
import com.cjj.train.member.domain.Member;
import com.cjj.train.member.domain.MemberExample;
import com.cjj.train.member.mapper.MemberMapper;
import com.cjj.train.member.req.MemberRegisterReq;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {
    @Autowired
    public MemberMapper memberMapper;


    public int count() {
        return Math.toIntExact(memberMapper.countByExample(null));
    }

    public long register(MemberRegisterReq memberRegisterReq) {
        String mobile = memberRegisterReq.getMobile();
        //select * from member where mobile = xxx;
        //构造查询条件，号码不能重复
        MemberExample memberExample = new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);
        List<Member> members = memberMapper.selectByExample(memberExample);
        if(CollUtil.isNotEmpty(members)) {
            throw new RuntimeException("电话号码已经存在");
        }
        Member member = new Member();
        member.setId(System.currentTimeMillis());
        member.setMobile(mobile);
        memberMapper.insert(member);
        return member.getId();
    }
}
