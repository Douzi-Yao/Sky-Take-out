package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;

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

    /**
     * 根据id查询套餐，用于修改页面回显数据
     * @param id
     * @return
     */
    public SetmealVO getById(Long id) {
        // 创建SetmealVO对象,把查询回来的Setmeal对象BeanUtils赋值
        SetmealVO setmealVO = new SetmealVO();
        Setmeal setmeal = setmealMapper.getById(id);
        BeanUtils.copyProperties(setmeal, setmealVO);

        // 从setmeal_dish表中根据setmealId获取SetmealDish类对象
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //1、修改套餐表，执行update
        setmealMapper.update(setmeal);

        //2、删除套餐和菜品的关联关系，操作setmeal_dish表，执行delete
        // 修改setmeal_dish关系表,先将该setmealId对应的dish全删除,然后再重新赋值
        setmealDishMapper.deleteById(setmeal.getId());

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        // 注意：重新赋值需要重新赋值每一个setmealId吗？
        // 答案：需要的，因为前端传来的setmealId是非必须的，传进来的数据可能没有包括setmealId
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmeal.getId());
        });

        //3、重新插入套餐和菜品的关联关系，操作setmeal_dish表，执行insert
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐起售停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        // 判断setmealId对应dish是否有停售菜品,如果有,静止停售
        // 从setmeal_dish表查找对应dish_id
        // 去dish表查找对应id启售数目

        if(status == StatusConstant.ENABLE){
            // 通过外键关联查询套餐内菜品的启售状态，确保套餐启售时所有菜品均为启售状态，防止数据不一致。
            // select d.* from dish d left join setmeal_dish sd on d.id = sd.dish_id where sd.setmeal_id = #{setmealId}
            List<Dish> dishes = dishMapper.getBySetmealId(id);

            // 对dishes为空的情况做防护性判断
            if(dishes != null && dishes.size() > 0){
                // 当前代码中 count < dishes.size() 表示有部分菜品未启售，抛出异常是合理的
                List<Long> dishIds = new ArrayList<>();
                dishes.forEach(dish -> {
                    dishIds.add(dish.getId());
                });
                int count = dishMapper.countByIdsAndStatus(dishIds);
                if(count < dishes.size()){
                    throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }
        }

        Setmeal setmeal = Setmeal.builder()
                .status(status)
                .id(id)
                .build();
        setmealMapper.update(setmeal);
    }


}
