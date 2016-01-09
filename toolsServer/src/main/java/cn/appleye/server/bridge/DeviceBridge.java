package cn.appleye.server.bridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.SyncService;

import cn.appleye.server.utils.LogUtil;

public class DeviceBridge {
	private static final String TAG = "DeviceBridge";
	
	private static DeviceBridge sInstance;
	
	private static AndroidDebugBridge sBridge;
	
	private static final String COMMAND_SOURCE_PATH = "./data/";
	private static final String COMMAND_TARGET_PAT = "/system/bin/";
	
	private static final int TIME_OUT = 3000;
	private static final int NEXT_LOCAL_PORT = 4939;
	
	private static final String UIRECORD_PATCH = COMMAND_TARGET_PAT + "uirecord";
	private static final String UIREPLAY_PATCH = COMMAND_TARGET_PAT + "uireplay";

	private DeviceBridge() {
		initDebugBridge();
	}
	
	public synchronized static DeviceBridge getInstance() {
		if (sInstance == null) {
			sInstance = new DeviceBridge();
		}
		return sInstance;
	}
	
	private static void initDebugBridge(){
		if (sBridge == null) {
			AndroidDebugBridge.init(false);
		}
		
		DdmPreferences.setTimeOut(TIME_OUT);
		
		if ((sBridge == null) || (!sBridge.isConnected())) {
			sBridge = AndroidDebugBridge.createBridge("adb", true);
		}
	}
	
	public static boolean pushScriptFile(IDevice device) {
		initDebugBridge();
		final boolean commandResult[] = new boolean[1];
		try {
			if (device.isOnline()) {
				device.executeShellCommand("adb root", new BooleanResultReader(){
					@Override
					public void setBooleanResult(boolean result) {
						commandResult[0] = result;
					}
				});
				
				commandResult[0] = screenbuffer();
				if (!commandResult[0]) {
					Runtime.getRuntime().exec(
							"cmd.exe /C del .\\data\\package.dat");
					Process pro = Runtime
							.getRuntime()
							.exec("cmd.exe /C adb shell getprop ro.build.version.release ");
					BufferedReader input = new BufferedReader(
							new InputStreamReader(pro.getInputStream()));
					String line = null;
					int version = 440;
					while ((line = input.readLine()) != null) {
						line = line.trim();
						if (!line.equals("")) {
							line = line.replace(".", "");
							version = Integer.parseInt(line);
							if (version / 100 == 0) {
								version *= 100;
							}
							if (version < 410) {
								Runtime.getRuntime()
										.exec("cmd.exe /C copy .\\selects\\getLaunchPackage_4.0.jar .\\getLaunchPackage.jar");
								LogUtil.logd(TAG, "======use getLaunchPackage_4.0.jar android 4.0");
							} else {
								Runtime.getRuntime()
										.exec("cmd.exe /C copy .\\selects\\getLaunchPackage_4.1.jar .\\getLaunchPackage.jar");
								LogUtil.logd(TAG, "======use getLaunchPackage_4.1.jar");
							}
						}
					}
					pushFile(device, COMMAND_SOURCE_PATH + "uirecord", COMMAND_TARGET_PAT + "uirecord");
					pushFile(device, COMMAND_SOURCE_PATH + "uireplay", COMMAND_TARGET_PAT + "uireplay");
					pushFile(device, COMMAND_SOURCE_PATH + "busybox", COMMAND_TARGET_PAT + "busybox");
					pushFile(device, COMMAND_SOURCE_PATH + "getLaunchPackage.jar", "/data/getLaunchPackage.jar");
					pushFile(device, COMMAND_SOURCE_PATH + "getLaunchPackage", COMMAND_TARGET_PAT + "getLaunchPackage");
					
					if (version < 500) {
						pushFile(device, COMMAND_SOURCE_PATH + "popenCount_32", COMMAND_TARGET_PAT + "popenCount");
					} else {
						pushFile(device, COMMAND_SOURCE_PATH + "popenCount", COMMAND_TARGET_PAT + "popenCount");
					}
					
					pushFile(device, COMMAND_SOURCE_PATH + "autoTestAppCrash.jar", "system/framework/autoTestAppCrash.jar");
					pushFile(device, COMMAND_SOURCE_PATH + "autoTestAppCrash", COMMAND_TARGET_PAT + "autoTestAppCrash");
					
					device.executeShellCommand(
							"chmod 7777 system/bin/autoTestAppCrash", new BooleanResultReader(){
								@Override
								public void setBooleanResult(boolean result) {
									commandResult[0] = result;
								}
							});

					device.executeShellCommand(
							"chmod 777 /system/bin/uirecord",
							new BooleanResultReader(){
								@Override
								public void setBooleanResult(boolean result) {
									commandResult[0] = result;
								}
							});
					device.executeShellCommand(
							"chmod 777 /system/bin/uireplay",
							new BooleanResultReader(){
								@Override
								public void setBooleanResult(boolean result) {
									commandResult[0] = result;
								}
							});
					device.executeShellCommand("chmod 777 /system/bin/busybox",
							new BooleanResultReader(){
						@Override
						public void setBooleanResult(boolean result) {
							commandResult[0] = result;
						}
					});
					device.executeShellCommand(
							"chmod 777 /system/bin/getLaunchPackage",
							new BooleanResultReader(){
								@Override
								public void setBooleanResult(boolean result) {
									commandResult[0] = result;
								}
							});
					
					device.executeShellCommand(
							"chmod 777 /system/bin/popenCount",
							new BooleanResultReader(){
								@Override
								public void setBooleanResult(boolean result) {
									commandResult[0] = result;
								}
							});
					
				} else {
					JOptionPane.showMessageDialog(null,
							"手机没有root权限，请获取root权限后重试！", "提示", 2);
					return false;
				}
				return true;
			}
		} catch (Exception e) {
			LogUtil.logd(TAG,
					"====== recordPrepareTask Exception: " + e.toString());
		}
		return commandResult[0];
	}
	
	public static boolean screenbuffer() {
		boolean isroot = false;
		boolean fast = false;
		do{
			try {
				Process process = Runtime.getRuntime().exec(
						"cmd.exe /C adb root");
				BufferedReader input = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
				String line = null;
				Pattern pattern = Pattern.compile("^((?!already).)*$");
				Matcher matcher = null;
				while ((line = input.readLine()) != null) {
					if (!line.equals("")) {
						fast = true;
						matcher = pattern.matcher(line);
						isroot = matcher.matches();
					}
				}
				return isroot;
			} catch (Exception e) {
			}
		}while(!fast);
		
		return isroot;
	}
	
	public static  boolean isDeviceRoot(IDevice device) {
		initDebugBridge();
		final boolean commandResults[] = new boolean[1];
		if (device.isOnline()) {
			try{
				device.executeShellCommand("adb root",
						new BooleanResultReader(){
							@Override
							public void setBooleanResult(boolean result) {
								commandResults[0] = result;
							}
				});
			}catch (Exception e) {
			}
			
		}
		
		return commandResults[0];
	}
	
	private static String buildIsServerRunningShellCommand() {
		return String.format("service call window %d",
				new Object[] { Integer.valueOf(3) });
	}
	
	public IDevice[] getDevices() {
		return sBridge.getDevices();
	}
	
	private static void pushFile(IDevice device, String local,
			String remoteFilepath) {
		LogUtil.logd(TAG, "[pushFile] " + local + "->" + remoteFilepath);
		try {
			SyncService sync = device.getSyncService();
			if (sync != null) {
				SyncService.ISyncProgressMonitor monitor = SyncService
						.getNullProgressMonitor();
				sync.pushFile(local, remoteFilepath, monitor);
				LogUtil.logd(TAG, "[pushFile] success");
				sync.close();
			}
		} catch (Exception e) {
			LogUtil.logd(TAG, "[pushFile] failed");
			e.printStackTrace();
		}
	}
	
	private static abstract class BooleanResultReader extends MultiLineReceiver {

		public void processNewLines(String[] strings) {
			if (strings.length > 0) {
				Pattern pattern = Pattern
						.compile(".*?\\([0-9]{8} ([0-9]{8}).*");
				Matcher matcher = pattern.matcher(strings[0]);
				if ((matcher.matches())
						&& (Integer.parseInt(matcher.group(1)) == 1)) {
					setBooleanResult(true);
					return;
				}
			}
			
			setBooleanResult(false);
		}
		
		public abstract void setBooleanResult(boolean result);

		public boolean isCancelled() {
			return false;
		}
	}
}
