package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 当前集合用于存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while(!begin.equals(end)){
            // 日期计算，计算指定日期的后一天对应的日期
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 将DateList转为需要的由","分隔开的String类型字符串
        String dateListString = dateList.stream().map(LocalDate::toString).collect(Collectors.joining(","));
//        String dateListString = String.join(",", dateList.stream().map(LocalDate::toString).collect(Collectors.toList()));
//        String dateListString = StringUtils.join(dateList, ",");

        // 存放每天的营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 查询date日期对应的营业额数据，营业额是指:状态为“已完成”的订单金额合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // select sum(amount) from orders where order_time > beginTime and order_time < endTime and status = 5
            Map map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        // 将turnoverList转为需要的由","分隔开的String类型字符串
        String turnoverListString = StringUtils.join(turnoverList, ",");

        // 封装返回结果
        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder()
                .dateList(dateListString)
                .turnoverList(turnoverListString)
                .build();

        return turnoverReportVO;
    }

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 用于计算第一天的前一天用户数量的时间
        LocalDateTime endTime = LocalDateTime.of(begin.plusDays(-1), LocalTime.MAX);

        // 存放从begin到end之间的每天对应的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        String dateListString = dateList.stream().map(LocalDate::toString).collect(Collectors.joining(","));

//        // 存放每天的总用户数量 select count(id) from user where create time < ? and create time >?
//        List<Integer> totalUserList = new ArrayList<>();
//        // 存放每天的新增用户数量 select count(id) from user where create time < ?
//        List<Integer> newUserList = new ArrayList<>();
//        for (LocalDate localDate : dateList) {
//            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
//            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);
//
//            Map map = new HashMap();
//
//            map.put("end", endTime);
//            // 总用户数量
//            Integer totalUserNum = userMapper.countByMap(map);
//            totalUserList.add(totalUserNum);
//
//            map.put("begin", beginTime);
//            // 新增用户数量
//            Integer newUserNum = userMapper.countByMap(map);
//            newUserList.add(newUserNum);
//        }
//        String totalUserListString = StringUtils.join(totalUserList, ",");
//        String newUserListString = StringUtils.join(newUserList, ",");

        // 只查询总的用户数目，后一天的用户数目减去前一天的数目就是新增的
        // 存放每天的总用户数量 select count(id) from user where create time < ? and create time >?
        List<Integer> totalUserList = new ArrayList<>();
        // 存放每天的新增用户数量 select count(id) from user where create time < ?
        List<Integer> newUserList = new ArrayList<>();
        // 计算第一天的前一天用户数量
        Integer lastUserNum = userMapper.countByEnd(endTime);
        Integer newUserNum;
        for (LocalDate localDate : dateList) {
            endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            // 总用户数量
            Integer totalUserNum = userMapper.countByEnd(endTime);
            totalUserList.add(totalUserNum);

            // 新增用户数量
            newUserNum = totalUserNum - lastUserNum;
            lastUserNum = totalUserNum;
            newUserList.add(newUserNum);
        }
        String totalUserListString = StringUtils.join(totalUserList, ",");
        String newUserListString = StringUtils.join(newUserList, ",");

        // 封装结果数据
        UserReportVO userReportVO = UserReportVO.builder()
                .dateList(dateListString)
                .totalUserList(totalUserListString)
                .newUserList(newUserListString)
                .build();

        return userReportVO;
    }

    /**
     * 统计指定时间区间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        // 存放从begin到end之间的每天对应的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 存放每天的订单总数
        List<Integer> orderCountList = new ArrayList<>();
        // 存放每天的有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();
        // 遍历dateList集合，查询每天的有效订单数和订单总数
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 查询每天的订单总数 select count(id) from orders where order time > ? and order time < ?
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            orderCountList.add(orderCount);
            // 查询每天的有效订单数 select count(id) from orders where order time > ? and order time < ? and status = ?
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            validOrderCountList.add(validOrderCount);
        }

        // 计算时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        // 计算时间区间内的有效订单数量
        Integer totalValidOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        // 计算订单完成率
        // 排除totalOrderCount为0的可能性
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : totalValidOrderCount.doubleValue() / totalOrderCount;

        return  OrderReportVO.builder()
            .dateList(StringUtils.join(dateList, ","))
            .orderCountList(StringUtils.join(orderCountList, ","))
            .validOrderCountList(StringUtils.join(validOrderCountList, ","))
            .totalOrderCount(totalOrderCount)
            .validOrderCount(totalValidOrderCount)
            .orderCompletionRate(orderCompletionRate)
            .build();
    }

    /**
     * 根据条件统计订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status){
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }
}
