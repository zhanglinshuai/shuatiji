package com.shuai.shuatiji.satoken;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.shuai.shuatiji.common.ErrorCode;
import com.shuai.shuatiji.exception.ThrowUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 获取登录设备工具类
 */
public class DeviceUtils {
    public static String getRequestDevice(HttpServletRequest request) {
        String userAgentStr = request.getHeader(Header.USER_AGENT.toString());
        UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
        ThrowUtils.throwIf(userAgent == null, ErrorCode.PARAMS_ERROR);
        String device = "pc";
        if (isMiniProgram(userAgentStr)) {
            //是否是小程序
            device = "miniProgram";
        } else if (isPad(userAgentStr)) {
            //是否是平板
            device = "pad";
        }else if(userAgent.isMobile()){
            //是否是手机
            device = "mobile";
        }
        return  device;
    }

    /**
     * 判断是否是ipad
     * 同时支持ios和安卓的pad的检测
     *
     * @param userAgentStr
     * @return
     */
    public static boolean isPad(String userAgentStr) {
        //检查iPad的UserAgent的标志
        boolean isIphone = StrUtil.containsIgnoreCase(userAgentStr, "iPad");
        boolean isAndroid = StrUtil.containsIgnoreCase(userAgentStr, "iPad") && !StrUtil.containsIgnoreCase(userAgentStr, "Mobile");
        return isIphone || isAndroid;
    }

    /**
     * 判断是否是小程序
     *
     * @param userAgentStr
     * @return
     */
    public static boolean isMiniProgram(String userAgentStr) {
        //通过判断userAgent是否包含MicroMessenger来判断是否是微信小程序
        return StrUtil.containsIgnoreCase(userAgentStr, "MicroMessenger")
                && StrUtil.containsIgnoreCase(userAgentStr, "MiniProgram");

    }


}
