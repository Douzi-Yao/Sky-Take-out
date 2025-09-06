package com.sky.service;

import com.sky.dto.DishDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

public interface DishService {



    /**
     * 新增菜品和对应口味数据
     * 不仅需要保存菜品,还需要保存菜品口味
     * @param dishDTO
     */
    public void saveWithFlavor(DishDTO dishDTO);
}
