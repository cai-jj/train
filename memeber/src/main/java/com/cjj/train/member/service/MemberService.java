package com.cjj.train.member.service;

import com.cjj.train.member.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemberService {
    @Autowired
    public MemberMapper memberMapper;


    public Integer count() {
        return memberMapper.count();
    }
}
