package cn.appleye.server.bridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;

import cn.appleye.server.utils.LogUtil;

public class DeviceBridge {
	private static final String TAG = "DeviceBridge";
	
	private static DeviceBridge sInstance;
	
	private static AndroidDebugBridge sBridge;
	
	private static final String COMMAND_SOURCE_PATH = "./data/";
	private static final String COMMAND_TARGET_PAT = "/system/bin/";
	
	private static final HashMap<IDevice, Integer> devicePortMap = new HashMap();
	
	private static final int TIME_OUT = 3000;
	private static int sNextLocalPort = 4939;
	
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
	
	public static void setupDeviceForward(IDevice device) {
		synchronized (devicePortMap) {
			if (device.getState() == IDevice.DeviceState.ONLINE) {
				int port = sNextLocalPort++;
				try {
					device.createForward(port, 4939);
					devicePortMap.put(device, Integer.valueOf(port));
				} catch (TimeoutException e) {
					LogUtil.e(TAG,
							"Timeout setting up port forwarding for " + device);
				} catch (AdbCommandRejectedException e) {
					LogUtil.e(TAG, String.format(
							"Adb rejected forward command for device %s: %s",
							new Object[] { device, e.getMessage() }));
				} catch (IOException e) {
					LogUtil.e(TAG, String.format(
							"Failed to create forward for device %s: %s",
							new Object[] { device, e.getMessage() }));
				}
			}
		}
	}

	public static void removeDeviceForward(IDevice device) {
		synchronized (devicePortMap) {
			Integer localPort = (Integer) devicePortMap.get(device);
			if (localPort != null) {
				try {
					device.removeForward(localPort.intValue(), 4939);
					devicePortMap.remove(device);
				} catch (TimeoutException e) {
					LogUtil.e(TAG,
							"Timeout removing port forwarding for " + device);
				} catch (AdbCommandRejectedException e) {
					LogUtil.e(TAG,
							String.format(
									"Adb rejected remove-forward command for device %s: %s",
									new Object[] { device, e.getMessage() }));
				} catch (IOException e) {
					LogUtil.e(TAG, String.format(
							"Failed to remove forward for device %s: %s",
							new Object[] { device, e.getMessage() }));
				}
			}
		}
	}
	
	public static App[] loadApps(IDevice device) {
		initDebugBridge();
		File file = new File("./data/package.dat");
		if (!file.exists()) {
			boolean[] result = new boolean[1];
			try {
				if (device.isOnline()) {
					device.executeShellCommand("/system/bin/getLaunchPackage",
							new BooleanResultReader(){

								@Override
								public void setBooleanResult(boolean result) {
									
								}
						
					});
				}
			} catch (Exception e) {
				LogUtil.logd(TAG, "====== loadApps exception " + e.toString());
			}
			LogUtil.logd(TAG, "====== Get installed app list from Device");
			pullFile(device, "/data/package.dat", "./data/package.dat");
		}
		ArrayList<App> apps = null;
		apps = new ArrayList();
		BufferedReader in = null;
		try {
			in = new BufferedReader(
					new FileReader("./data/package.dat"));
		} catch (Exception e) {
			LogUtil.logd(TAG, "====== loadApps parce package.dat exception "
					+ e.toString());
		}
		try {
			String line;
			while ((line = in.readLine()) != null) {
				int index = line.indexOf(' ');
				App app = new App();
				app.packageName = line.substring(0, index);
				app.activityName = line.substring(index + 1);
				app.packageName = app.packageName.trim();
				app.activityName = app.activityName.trim();
				apps.add(app);
			}
		} catch (Exception e) {
			LogUtil.logd(TAG, "====== loadApps Exception: " + e.toString());
		}
		if (apps.size() > 0) {
			return (App[]) apps.toArray(new App[apps.size()]);
		}
		return null;
	}
	
	public static boolean pushScriptFiles(IDevice device) {
		initDebugBridge();
		try {
			if (device.isOnline()) {
				device.executeShellCommand("adb remount", new BooleanResultReader(){

					@Override
					public void setBooleanResult(boolean result) {
						LogUtil.logd(TAG, "adb remount, result = " + result);
					}
				});
				boolean isRoot = isDeviceRoot(device);
				if (isRoot) {
					pushFile(device, "./data/getLaunchPackage.jar",
							"/data/getLaunchPackage.jar");
					pushFile(device, "./data/getLaunchPackage",
							"/system/bin/getLaunchPackage");

					device.executeShellCommand(
							"chmod 777 /system/bin/getLaunchPackage",
							new BooleanResultReader(){

								@Override
								public void setBooleanResult(boolean result) {
								}
							});
					
				} else {
					LogUtil.logd(TAG, "手机没有root权限，请获取root权限后重试！");
					return false;
				}
				return true;
			}
		} catch (Exception e) {
			LogUtil.logd(TAG,"====== Exception: " + e.toString());
			return false;
		}
		return true;
	}
	
	private static void pullFile(IDevice device, String remoteFilepath,
			String local) {
		try {
			SyncService sync = device.getSyncService();
			if (sync != null) {
				SyncService.ISyncProgressMonitor monitor = SyncService
						.getNullProgressMonitor();
				sync.pullFile(remoteFilepath, local, monitor);
				LogUtil.logd(TAG, "====== pull file from devices successfully");
				sync.close();
			}
		} catch (Exception e) {
			LogUtil.logd(TAG, "====== pull file " + remoteFilepath + ", " + local + " from devices error");
		}
	}
	
	private static abstract class BooleanResultReader extends MultiLineReceiver {

		public void processNewLines(String[] strings) {
			if (strings.length > 0) {
				System.out.println(strings[0]);
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
	
	public static class App {
		public String packageName;
		public String activityName;
		public boolean isCancelled;
		public int AttemptCount;

		public String toString() {
			return this.activityName;
		}

		public App() {
			this.isCancelled = false;
			this.AttemptCount = 1;
		}

		public App(String pkg, String aty) {
			this.isCancelled = false;
			this.AttemptCount = 1;
			this.packageName = pkg;
			this.activityName = aty;
		}
	}
}
