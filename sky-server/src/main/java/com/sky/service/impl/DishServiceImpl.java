package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

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

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 表示开始分页
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Transactional // 由于涉及多个数据表操作,因此需要添加事务注解
    public void deleteBatch(List<Long> ids) {
        // 判断当前菜品是否能够删除 -- 是否存在起售中的菜品
//        for (Long id : ids) {
//            Dish dish = dishMapper.getById(id);
//            if (dish.getStatus() == StatusConstant.ENABLE){
//                // 当前菜品处于起售中,不能删除
//                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
//            }
//        }
        // 这个ids传参是List<Long> ids，必须使用动态sql<foreach>遍历
        // 因为#{ids}会将整个List作为单个参数处理，不会展开为多个值
        int count = dishMapper.countByIdsAndStatus(ids);
        if(count > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }

        // 判断当前菜品是否能够删除 -- 判断菜品是否被套餐关联
        // 直接sql中子查询把状态和套餐都过滤掉,返回操作数据的条数,如果是0就说明不能删除,直接抛异常就行,如果>0再去删除dish_flavor表中的数据
        // 这里可以直接返回count(0),然后在业务层直接判断是否等于0就行
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size() > 0){
            // 当前菜品被套餐关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        // 先删除菜品关联的口味数据，再删菜品，防止口味删除失败导致外键为空

        // 删除菜品表中的菜品数据
        // 缺点:每一次for循环遍历都会发出两条sql,若遍历次数多,则删除数量多,可能引发性能方面问题
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            // 删除菜品关联的口味数据
//            dishFlavorMapper.deleteByDishId(id    );
//        }

        // 根据菜品id集合批量删除关联的口味数据
        dishFlavorMapper.deleteByDishIds(ids);
        // 根据菜品id集合批量删除菜品数据
        dishMapper.deleteByIds(ids);
    }

    /**
     * 根据id查询菜品和对应的口味数据
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        // 根据id查询菜品数据
        Dish dish = dishMapper.getById(id);

        // 根据菜品id查询口味数据
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);

        // 将查询到的数据封装到VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        // 不能对flavors进行对象拷贝BeanUtils.copyProperties(flavors, dishVO)
        // 因为BeanUtils把flavors这个List对象本身当成“源 bean”，而List接口里并没有一个名叫flavors的属性，更没有getFlavors()方法
        // 因此BeanUtils找不到同名字段，复制量为 0
        dishVO.setFlavors(flavors);

        return dishVO;
    }

    /**
     * 根据id修改菜品基本信息和对应的口味信息
     * 需要加事务：该方法涉及两个数据库操作
     * 这两个操作必须保持原子性，即要么全部成功，要么全部失败回滚。
     * 如果不加事务，可能在删除旧口味后、插入新口味前出现异常，导致数据不一致。因此需使用 @Transactional 注解保证事务一致性。
     * @param dishDTO
     */
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        // 修改菜品表基本信息
        // 可以直接传dishDTO,但不合适。因为dishDTO还包含了口味数据,而当前只修改菜品表基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        //关联表可以这么做,但其他表不可以。前提是口味表不能作为父表，也就是其他表的外键，因为每次修改表口味的id会变
        //删除原有口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        //重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        // 口味非必须,需要判断用户是否提交口味
        if(flavors != null && flavors.size() > 0){
            // forEach + lambda表达式dishId值
            // dishId没有设值 因此需要循环遍历集合flavors 给里面的dishId赋上对应菜品的id
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });

            // 向口味表插入n条数据
            // 无需遍历集合,可以直接插入。
            // Mybatis的动态sql支持批量插入<foreach>
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品起售停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setStatus(status);
        dishMapper.update(dish);

        // 如果执行停售操作，则包含此菜品的套餐也需要停售。
        if(status == StatusConstant.DISABLE){
            // 查看该菜品关联什么套餐
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            // select setmeal_id from setmeal_dish where dish_id in (?,?,?)
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);

            // 给对应的套餐进行停售
            if(setmealIds != null && setmealIds.size() > 0){
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .status(StatusConstant.DISABLE)
                            .id(setmealId)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }
    }
}
