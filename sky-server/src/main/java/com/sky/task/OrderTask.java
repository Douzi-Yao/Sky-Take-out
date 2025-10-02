package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    /**
     * 处理超时订单的方法
     */
    @Scheduled(cron = "0 * * * * ? ") // 每分钟触发一次
    public void processTimeoutOrder(){
        log.info("定时处理超时订单:{}", LocalDateTime.now());
//
//        // 当前时间+(-15)分钟
//        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
//
//        // select * from orders where status = ? and order_time < 当前时间 - 15分钟
//        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
//
//        if(ordersList != null && ordersList.size() > 0){
//            for (Orders orders : ordersList) {
//                orders.setStatus(Orders.CANCELLED);
//                orders.setCancelReason("订单超时，自动取消");
//                orders.setCancelTime(LocalDateTime.now());
//                orderMapper.update(orders);
//            }
//        }

        orderService.processTimeoutOrder(Orders.PENDING_PAYMENT, LocalDateTime.now());
    }

    /**
     * 处理一直处于派送中状态的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单:{}", LocalDateTime.now());

//        // 筛选出那些“预计送达时间”小于“当前时间”的，这样可以避免刚下单的用户被自动完成！
//        LocalDateTime time = LocalDateTime.now().plusHours(-1);
//
//        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
//
//        if(ordersList != null && ordersList.size() > 0){
//            for (Orders orders : ordersList) {
//                orders.setStatus(Orders.COMPLETED);
//                orderMapper.update(orders);
//            }
//        }

        orderService.processDeliveryOrder(Orders.DELIVERY_IN_PROGRESS, LocalDateTime.now());
    }
}
