package com.shuai.shuatiji.satoken;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.shuai.shuatiji.model.entity.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.shuai.shuatiji.constant.UserConstant.USER_LOGIN_STATE;

public class StpInterfaceImpl implements StpInterface {
    /**
     * 返回一个账号所拥有的权限码集合
     * @param o
     * @param s
     * @return
     */
    @Override
    public List<String> getPermissionList(Object o, String s) {
        return new ArrayList<>();
    }

    /**
     * 返回一个账号所拥有的角色标识集合（权限与角色可分开校验）
     * @param o
     * @param s
     * @return
     */
    @Override
    public List<String> getRoleList(Object o, String s) {
        User user = (User) StpUtil.getSessionByLoginId(o).get(USER_LOGIN_STATE);
        return Collections.singletonList(user.getUserRole());
    }
}
