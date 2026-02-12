// JEIReflectionHelper.java
package com.ming_hai.foodstats.compat;

/**
 * 为了向后兼容而创建的适配器类
 */
public class JEIReflectionHelper {
    
    public static boolean isJeiLoaded() {
        return JEIIntegration.isJeiLoaded();
    }
    
    public static void logJeiInfo() {
        JEIIntegration.logJeiInfo();
    }
}