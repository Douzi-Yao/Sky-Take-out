package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     *
     * @param user
     */
    void insert(User user);

    /**
     * 根据id查询用户信息
     * @param userId
     * @return
     */
    @Select("select * from user where id = #{id}")
    User getById(Long userId);

    /**
     * 根据动态条件统计用户数据
     *
     * @param map
     * @return
     */
    int countByMap(Map map);

    /**
     * 截至endTime计算用户总数
     * @param endTime
     * @return
     */
    @Select("select count(id) from user where create_time < #{endTime}")
    Integer countByEnd(LocalDateTime endTime);
}
