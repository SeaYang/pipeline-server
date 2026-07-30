package com.ci.pipeline.common.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 本机 IP 工具类。
 * <p>定时任务执行日志需要记录执行实例 IP，用于多实例部署下跨实例路由"停止任务"请求。
 */
public final class IpUtils {

    private static volatile String cachedLocalIp;

    private IpUtils() {
    }

    /**
     * 获取本机首选内网 IPv4 地址，进程内缓存一次。
     */
    public static String getLocalIp() {
        if (cachedLocalIp != null) {
            return cachedLocalIp;
        }
        synchronized (IpUtils.class) {
            if (cachedLocalIp == null) {
                cachedLocalIp = resolveLocalIp();
            }
            return cachedLocalIp;
        }
    }

    private static String resolveLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
                        continue;
                    }
                    if (addr.getHostAddress().indexOf(':') >= 0) {
                        continue; // 跳过 IPv6
                    }
                    return addr.getHostAddress();
                }
            }
        } catch (SocketException e) {
            // 忽略，走兜底逻辑
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
