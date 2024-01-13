package com.cjj.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.cjj.train.business.domain.*;
import com.cjj.train.business.enums.ConfirmOrderStatusEnum;
import com.cjj.train.business.enums.SeatColEnum;
import com.cjj.train.business.enums.SeatTypeEnum;
import com.cjj.train.business.mapper.ConfirmOrderMapper;
import com.cjj.train.business.req.ConfirmOrderDoReq;
import com.cjj.train.business.req.ConfirmOrderQueryReq;
import com.cjj.train.business.req.ConfirmOrderTicketReq;
import com.cjj.train.business.resp.ConfirmOrderQueryResp;
import com.cjj.train.common.context.LoginMemberContext;
import com.cjj.train.common.exception.BusinessException;
import com.cjj.train.common.resp.PageResp;
import com.cjj.train.common.util.SnowUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.cjj.train.common.exception.BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR;

@Service
public class ConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;

    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

    @Resource
    private AfterConfirmOrderService afterConfirmOrderService;

    public void save(ConfirmOrderDoReq req) {
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if (ObjectUtil.isNull(confirmOrder.getId())) {
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        } else {
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id desc");
        ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        PageInfo<ConfirmOrder> pageInfo = new PageInfo<>(confirmOrderList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<ConfirmOrderQueryResp> list = BeanUtil.copyToList(confirmOrderList, ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        confirmOrderMapper.deleteByPrimaryKey(id);
    }

    public void doConfirm(ConfirmOrderDoReq req) {
        // 省略业务数据校验，如：车次是否存在，余票是否存在，车次是否在有效期内，tickets条数>0，同乘客同车次是否已买过

        // 保存确认订单表，状态初始

        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setCreateTime(now);
        confirmOrder.setUpdateTime(now);
        confirmOrder.setId(SnowUtil.getSnowflakeNextId());
        confirmOrder.setMemberId(LoginMemberContext.getId());
        confirmOrder.setDate(req.getDate());
        confirmOrder.setTrainCode(req.getTrainCode());
        confirmOrder.setStart(req.getStart());
        confirmOrder.setEnd(req.getEnd());
        confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        //下单买的票数，前端以json格式的形式传过来，经过映射变为list
        List<ConfirmOrderTicketReq> tickets = req.getTickets();
        confirmOrder.setTickets(JSONUtil.toJsonStr(tickets));
        LOG.info("票型{}:", JSONUtil.toJsonStr(tickets));
        confirmOrderMapper.insert(confirmOrder);
        // 查出数据库车票记录，需要得到真实的库存
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(
                req.getDate(), req.getTrainCode(), req.getStart(), req.getEnd());
        LOG.info("余票记录为{}", dailyTrainTicket);
        // 扣减余票数量，并判断余票是否足够
        reduceTickets(req, dailyTrainTicket);
        LOG.info("扣减后余票记录为{}", dailyTrainTicket);
        // 选座
        //保存最终选座的结果
        List<DailyTrainSeat> finalSeatList = new ArrayList<>();
        //是否可以选座,只需要查看ConfirmOrderTicketReq的seat字段，字段为空，不支持选座
        if (StrUtil.isNotBlank(tickets.get(0).getSeat())) {
            //不为空，支持选座
            LOG.info("本次购票有选座");
            //计算相对于第一个座位的偏移值 比如C1 D1 偏移值[0, 1] A1 B1 C1 [0,1,2]
            //先查出本次选座类型都有哪些列，因为一等座与二等座对应的列是不一样的
            //查出一等座或二等座对应的列数
            List<SeatColEnum> colEnumList = SeatColEnum.getColsByType(tickets.get(0).getSeatTypeCode());
            LOG.info("本次选座的类型包含的列为{}", colEnumList);
            //构造和前端一样的两排列表 {A1,C1,D1,F1, A2,C2,D2,F2}
            List<String> seatList = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                for (SeatColEnum seatColEnum : colEnumList) {
                    seatList.add(seatColEnum.getCode() + i);
                }
            }
            LOG.info("构造好的座位列表为{}", seatList);
            //开始计算座位的偏移值
            //先计算绝对偏移值，即座位在seatList的下标
            List<Integer> aboluteOffsetIndex = new ArrayList<>();
            for (ConfirmOrderTicketReq ticket : tickets) {
                int index = seatList.indexOf(ticket.getSeat());
                aboluteOffsetIndex.add(index);
            }
            LOG.info("座位的绝对偏移值{}", aboluteOffsetIndex);
            //计算相对偏移值，绝对偏移值的元素下标减去第一个元素的下标
            List<Integer> offsetIndex = new ArrayList<>();
            for (int i = 0; i < aboluteOffsetIndex.size(); i++) {
                offsetIndex.add(aboluteOffsetIndex.get(i) - aboluteOffsetIndex.get(0));
            }
            LOG.info("座位的相对偏移值{}", offsetIndex);
            //已经选了座位，座位类型统一，根据传入的座位类型进行选座，一次性选出所有的座位。
            getSeat(finalSeatList,
                    req.getDate(),
                    req.getTrainCode(),
                    tickets.get(0).getSeatTypeCode(),
                    tickets.get(0).getSeat().split("")[0],  //从A1得到A
                    offsetIndex,
                    dailyTrainTicket.getStartIndex(),
                    dailyTrainTicket.getEndIndex());
        } else {
            //为空，不支持选座
            LOG.info("本次购票没有选座");

            for (ConfirmOrderTicketReq ticket : tickets) {
                //没有选座，就根据座位类型一个个选座位
                LOG.info("座位类型{}：", ticket.getSeatTypeCode());
                getSeat(finalSeatList,
                        req.getDate(),
                        req.getTrainCode(),
                        ticket.getSeatTypeCode(),
                        null,
                        null,
                        dailyTrainTicket.getStartIndex(),
                        dailyTrainTicket.getEndIndex());
            }

        }
        LOG.info("最终选座是{}", finalSeatList);

        // 选中座位后事务处理：
        // 座位表修改售卖情况sell；
        afterConfirmOrderService.afterDoConfirm(dailyTrainTicket, finalSeatList);

        // 余票详情表修改余票；
        // 为会员增加购票记录
        // 更新确认订单为成功
    }

    //挑选座位
    private void getSeat(List<DailyTrainSeat> finalSeatList, Date date,
                         String trainCode, String seatType,
                         String column, List<Integer> offsetIndex,
                         Integer startIndex, Integer endIndex) {
        List<DailyTrainCarriage> carriageList = dailyTrainCarriageService.selectBySeatType(date, trainCode, seatType);
        LOG.info("共查出符号条件车厢：{}", carriageList.size());
        //开始选座，一个车厢一个车厢获取座位数据
        // 挑选符合条件的座位，如果这个车箱不满足，则进入下个车箱（多个选座应该在同一个车厢）
        List<DailyTrainSeat> getSeatList = new ArrayList<>();
        for (DailyTrainCarriage carriage : carriageList) {
            getSeatList = new ArrayList<>();
            Integer index = carriage.getIndex();
            LOG.info("开始从第{}车厢选座", index);
            List<DailyTrainSeat> seatList = dailyTrainSeatService.selectByCarriage(date, trainCode, index);
            LOG.info("车厢{}的座位数为{}", index, seatList.size());

            //遍历所有座位，看是否可卖
            for (DailyTrainSeat dailyTrainSeat : seatList) {
                //查看座位是否被选中过，因为一次可能会随机选择好几个空位，如果没有指定选座，就会逐个遍历空位
                boolean alreadyChooseFlag = false;
                for (DailyTrainSeat trainSeat : finalSeatList) {
                    if (dailyTrainSeat.getId().equals(trainSeat.getId())) {
                        //座位重复
                        alreadyChooseFlag = true;
                        break;
                    }
                }
                //座位重复，跳过当前座位
                if (alreadyChooseFlag == true) {
                    LOG.info("当前座位已经被挑选");
                    continue;
                }
                //在查看该座位是否可买之前应该先比对一下列号，选座才有列号，没有选座是随机安排的，不用比对
                String col = dailyTrainSeat.getCol();
                Integer seatIndex = dailyTrainSeat.getCarriageSeatIndex();
                if (StrUtil.isBlank(column)) {
                    //传进来的column是空,没有选座的情况
                    LOG.info("没有选座");
                } else {

                    if (!column.equals(col)) {
                        //座位类型不匹配
                        LOG.info("当前座位{}列值不对，当前列{}, 目标列{}", seatIndex, col, column);
                        continue;
                    }
                }
                //查看当前座位是否可卖
                boolean isChoose = isSell(dailyTrainSeat, startIndex, endIndex);
                if (isChoose) {
                    LOG.info("选中座位");
                    getSeatList.add(dailyTrainSeat);
                } else {
                    LOG.info("未选中座位");
                    continue;
                }
                //根据offset选剩余座位
                boolean getAllOffsetSeat = true;
                if (CollUtil.isNotEmpty(offsetIndex)) {
                    LOG.info("有偏移值{}，校验偏移值的座位是否可选", offsetIndex);
                    //第一个座位已经选了，判断偏移值里接下来的座位是否可选
                    for (int i = 1; i < offsetIndex.size(); i++) {
                        int offset = offsetIndex.get(i);
                        LOG.info("下一个偏移值是{}", offset);
                        //数据库里的座位索引是从1开始的
                        int nextIndex = seatIndex + offset;
                        //有选座肯定要在同一个车厢
                        if (nextIndex > seatList.size()) {
                            LOG.info("座位{}不可选", nextIndex);
                            getAllOffsetSeat = false;
                            break;
                        }
                        //根据相对偏移值获得座位
                        LOG.info("下一个座位号是{}", nextIndex);
                        DailyTrainSeat nextDailyTrainSeat = seatList.get(nextIndex - 1);
                        boolean isChooseNext = isSell(nextDailyTrainSeat, startIndex, endIndex);
                        if (isChooseNext) {
                            LOG.info("座位{}被选中", nextDailyTrainSeat.getCarriageSeatIndex());
                            getSeatList.add(nextDailyTrainSeat);
                            continue;
                        } else {
                            LOG.info("座位{}不可选", nextDailyTrainSeat.getCarriageSeatIndex());
                            getAllOffsetSeat = false;
                            break;
                        }
                    }
                }
                if (getAllOffsetSeat == false) {
                    //没有一次性选成功,清空当前选座
                    getSeatList = new ArrayList<>();
                    continue;
                }
                //全部选座成功，保存好选好的座位
                finalSeatList.addAll(getSeatList);
                return;


            }


        }

    }

    //查看座位是否被卖
    boolean isSell(DailyTrainSeat dailyTrainSeat, Integer startIndex, Integer endIndex) {
        String sell = dailyTrainSeat.getSell();
        String sellPart = sell.substring(startIndex, endIndex);
        if (Integer.parseInt(sellPart) > 0) {
            //不能售卖
            LOG.info("座位{}在本次车站{}~{}区间不能购买", dailyTrainSeat.getCarriageSeatIndex(),
                    startIndex, endIndex);
            return false;
        } else {
            //当前区间的座位都为0,可以被售卖
            LOG.info("座位{}在本次车站{}~{}区间可以购买", dailyTrainSeat.getCarriageSeatIndex(),
                    startIndex, endIndex);
            //区间内的0全部变为1
            String curSell = sellPart.replace('0', '1');
            curSell = StrUtil.fillBefore(curSell, '0', endIndex);
            curSell = StrUtil.fillAfter(curSell, '0', sell.length());
            //将区间内填充的的和之前的按位与
            int newSellInt = NumberUtil.binaryToInt(curSell) | NumberUtil.binaryToInt(sell);
            String newSell = NumberUtil.getBinaryStr(newSellInt);
            newSell = StrUtil.fillBefore(newSell, '0', sell.length());
            LOG.info("座位{}被选中，原售票信息：{}，车站区间为：{}~{}，最终售票信息为：{}",
                    dailyTrainSeat.getCarriageSeatIndex(), sell, startIndex, endIndex, newSell);
            dailyTrainSeat.setSell(newSell);
            return true;
        }
    }

    //扣减车票
    private void reduceTickets(ConfirmOrderDoReq req, DailyTrainTicket dailyTrainTicket) {
        //前端传来的下单的票
        List<ConfirmOrderTicketReq> orderTickets = req.getTickets();
        for (ConfirmOrderTicketReq ticketReq : orderTickets) {
            String seatTypeCode = ticketReq.getSeatTypeCode();
            SeatTypeEnum seatTypeEnum = EnumUtil.getBy(SeatTypeEnum::getCode, seatTypeCode);
            switch (seatTypeEnum) {
                case YDZ -> {
                    //一等座库存
                    int count = dailyTrainTicket.getYdz();
                    //库存少于1
                    if (count < 1) throw new BusinessException(CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    dailyTrainTicket.setYdz(count - 1);
                }
                case EDZ -> {
                    //二等座库存
                    int count = dailyTrainTicket.getEdz();
                    //库存少于1
                    if (count < 1) throw new BusinessException(CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    dailyTrainTicket.setEdz(count - 1);
                }
                case RW -> {
                    int count = dailyTrainTicket.getRw();
                    //库存少于1
                    if (count < 1) throw new BusinessException(CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    dailyTrainTicket.setRw(count - 1);
                }
                case YW -> {
                    int count = dailyTrainTicket.getYw();
                    //库存少于1
                    if (count < 1) throw new BusinessException(CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    dailyTrainTicket.setYw(count - 1);
                }
            }
        }
    }
}
