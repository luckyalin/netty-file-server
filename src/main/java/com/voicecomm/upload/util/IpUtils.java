package com.voicecomm.upload.util;

import org.apache.commons.lang3.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class IpUtils {

	public static String localHostIp = null;

	public static String getLocalHostIp() {
		if (StringUtils.isNotBlank(localHostIp)) {
			return localHostIp;
		}
		try {
			Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
			InetAddress ip = null;
			while (allNetInterfaces.hasMoreElements()) {
				NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
				if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
					continue;
				} else {
					Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
					while (addresses.hasMoreElements()) {
						ip = addresses.nextElement();
						if (ip != null && ip instanceof Inet4Address) {
							localHostIp = ip.getHostAddress();
							return localHostIp;
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("IP地址获取失败" + e.toString());
		}
		return "";
	}

}
