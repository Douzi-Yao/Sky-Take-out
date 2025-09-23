package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * 需要返回主键值，因为后面需要插入订单明细，需要通过id关联是哪个订单
     * @param orders
     */
    void insert(Orders orders);
}
