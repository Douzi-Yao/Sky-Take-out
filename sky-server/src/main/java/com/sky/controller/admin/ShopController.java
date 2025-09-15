package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "店铺相关接口")
@Slf4j
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 设置店铺营业状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    @ApiOperation("设置店铺营业状态")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺的营业状态为:{}", status == 1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set(KEY, status.toString());
        return Result.success();
    }

    /**
     * 获取店铺营业状态
     * 出现Integer不能转String的是因为之前在RedisConfiguration中设置了序列化器，
     * 这个序列化器将key，value全部序列化为Stirng，导致异常，只要把它换成正常的就好了
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺营业状态")
    public Result<Integer> getStatus(){
        String status = (String) redisTemplate.opsForValue().get(KEY);
        log.info("获取到店铺的营业状态为:{}", status.equals("1") ? "营业中" : "打烊中");
        return Result.success(Integer.parseInt(status));
    }
}
