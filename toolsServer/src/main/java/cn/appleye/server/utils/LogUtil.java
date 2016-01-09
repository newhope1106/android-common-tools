package cn.appleye.server.utils;

public class LogUtil {
	
	public static void logd(String tag, String message){
		
	}
	
	public static void logd(Object obj, String message) {
		logd(obj.getClass().getSimpleName(), message);
	}
}
