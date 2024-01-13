package com.cjj.train.business.service;

import com.cjj.train.business.domain.*;
import com.cjj.train.business.mapper.CustDailyTrainTicketMapper;
import com.cjj.train.business.mapper.DailyTrainSeatMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class AfterConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(AfterConfirmOrderService.class);


    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;

    @Resource
    private CustDailyTrainTicketMapper custDailyTrainTicketMapper;

    /**
     *  选中座位后事务处理：
     *     座位表修改售卖情况sell；
     *     余票详情表修改余票；
     *     为会员增加购票记录
     *     更新确认订单为成功
     */
    @Transactional
    public void afterDoConfirm(DailyTrainTicket dailyTrainTicket,List<DailyTrainSeat> finalList) {
        for (DailyTrainSeat trainSeat : finalList) {
            //修改售卖情况sell
            DailyTrainSeat dailyTrainSeat = new DailyTrainSeat();
            dailyTrainSeat.setSell(trainSeat.getSell());
            dailyTrainSeat.setId(trainSeat.getId());
            dailyTrainSeat.setUpdateTime(new Date());
            dailyTrainSeatMapper.updateByPrimaryKeySelective(dailyTrainSeat);
            //扣减车票信息
            Integer startIndex =  dailyTrainTicket.getStartIndex();
            Integer endIndex = dailyTrainTicket.getEndIndex();
            //获得sell信息
            char[] chars = dailyTrainSeat.getSell().toCharArray();
            //获得影响的区间
            Integer maxStartIndex = endIndex - 1;
            Integer minEndIndex = startIndex + 1;
            Integer minStartIndex = 0;
            for(int i = startIndex - 1; i >= 0; i--) {
                if(chars[i] == '1') {
                    minStartIndex = i + 1;
                    break;
                }
            }
            LOG.info("影响出发站区间"+ minStartIndex + "-"+ maxStartIndex);
            Integer maxEndIndex = chars.length;
            //如果后面都是0,最大站就是sell的长度
            for(int j = endIndex; j < chars.length; j++) {
                if(chars[j] == '1') {
                    maxEndIndex = j;
                    break;
                }
            }
            LOG.info("影响到达站区间"+ minEndIndex + "-"+ maxEndIndex);
            custDailyTrainTicketMapper.updateCountBySell(trainSeat.getDate(),
                    trainSeat.getTrainCode(), trainSeat.getSeatType(),
                    minStartIndex, maxStartIndex, minEndIndex, maxEndIndex);
        }




    }

}
