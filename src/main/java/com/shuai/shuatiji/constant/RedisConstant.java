package com.shuai.shuatiji.constant;

/**
 * redis的常量类
 */
public interface RedisConstant {

    /**
     * 用户签到的key的前缀
     */
    String USER_SIGN_IN_REDIS_KEY_PREFIX = "user:signins:";

    /**
     * 获取用户的签到的key
     * @param year 年份
     * @param userId 用户id
     * @return
     */
    static String getUserSignInRedisKey(int  year,long userId){
        return String.format(USER_SIGN_IN_REDIS_KEY_PREFIX+":"+year+":"+userId);
    }
}
