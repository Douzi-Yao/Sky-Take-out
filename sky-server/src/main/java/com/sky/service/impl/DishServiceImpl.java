package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    /**
     * 新增菜品和对应口味数据
     * 不仅需要保存菜品,还需要保存菜品口味
     * @param dishDTO
     */
    @Transactional // 为了保证事务一致性,保证方法要么全成功,要么全失败
    public void saveWithFlavor(DishDTO dishDTO) {

        // 由于dishDTO包括菜品口味flavour,而向菜品表插入数据时不需要插入flavour,因此new一个Dish对象
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 向菜品表插入1条数据
        dishMapper.insert(dish);

        // 由于此时菜品已经保存插入完成,已经分配好了id
        // 但是目前getId无法直接获得到,要在DishMapper设置useGeneratedKeys="true"来获得insert语句生成的主键值
        // DishMapper的 keyProperty="id"表示insert语句执行完后创建的主键值会赋给Dish的id属性
        // 获取insert语句生成的主键值
        Long dishId = dish.getId();

        // 向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 口味非必须,需要判断用户是否提交口味
        if(flavors != null && flavors.size() > 0){
            // forEach + lambda表达式dishId值
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });

            // 向口味表插入n条数据
            // 无需遍历集合,可以直接插入。
            // Mybatis的动态sql支持批量插入<foreach>
            dishFlavorMapper.insertBatch(flavors);
        }
    }
}
