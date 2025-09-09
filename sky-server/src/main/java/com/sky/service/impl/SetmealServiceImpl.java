package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    public void saveWithDish(SetmealDTO setmealDTO) {
        // 赋值setmeal表
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 向套餐表插入数据
        setmealMapper.insert(setmeal);

        // 得到套餐id,存入setmeal_dish表
        Long setmealId = setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            //保存套餐和菜品的关联关系,foreach批量插入
            setmealDishMapper.insertBatch(setmealDishes);
        }

    }

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除套餐
     * 分两个循环：更安全，逻辑清楚 → 推荐
     * 合并成一个循环：代码更简洁，但要完全依赖事务回滚，否则可能删到一半抛异常。
     * @param ids
     */
    @Transactional // 处理两个表
    public void deleteBatch(List<Long> ids) {
        // 判断是否为启售状态，若不启售，则报错
        // 根据id查询套餐启售状态
//        ids.forEach(id -> {
//            Setmeal setmeal = setmealMapper.getById(id);
//            if(setmeal.getStatus() == StatusConstant.ENABLE){
//                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
//            }
//        });

//        ids.forEach(setmealId -> {
//            // 删除套餐表中的数据
//            setmealMapper.deleteById(setmealId);
//            // 删除套餐菜品关系表中的数据
//            setmealDishMapper.deleteById(setmealId);
//        });

        // 判断ids中status=1的数量(count),大于0则说明有启售套餐,不能删除
        int count = setmealMapper.countByIdsAndStatus(ids);
        if(count > 0){
            throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
        }

        // 批量删除动态sql foreach
        setmealMapper.deleteByIdBatch(ids);
        // 批量删除套餐和分类id绑定 setmeal_dish
        setmealDishMapper.deleteBySetmealIdBatch(ids);

    }
}
