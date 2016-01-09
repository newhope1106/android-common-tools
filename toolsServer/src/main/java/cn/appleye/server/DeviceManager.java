package cn.appleye.server;

import java.util.ArrayList;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;

import cn.appleye.server.bridge.DeviceBridge;
import cn.appleye.server.utils.LogUtil;

public class DeviceManager {
	private static final String TAG = "DeviceManager";
	private static DeviceBridge sBridge = DeviceBridge.getInstance();	
	private static ArrayList<IDevice> sDeviceList = new ArrayList<IDevice>();
	private static DeviceChangedListener sListener;
	
	public static ArrayList<String> getDeviceSerialNumberList() {
		
		ArrayList<String> serialNumberList = new ArrayList<String>();
		
		IDevice[] devices = sBridge.getDevices();
		int length = devices.length;
		for (int i=0; i<length; i++) {
			IDevice device = devices[i];
			DeviceBridge.setupDeviceForward(device);
			sDeviceList.add(device);
			serialNumberList.add(device.getSerialNumber());
		}
		
		return serialNumberList;
	}
	
	public static void setupDevicesChangedListener() {
		if (sListener == null) {
			sListener = new DeviceChangedListener();
			AndroidDebugBridge.addDeviceChangeListener(sListener);
		}
	}
	
	private static class DeviceChangedListener implements AndroidDebugBridge.IDeviceChangeListener{

		public void deviceChanged(IDevice device, int param) {
			LogUtil.logd(TAG, "device changed, serials number = " + device.getSerialNumber());
			if (((param & 0x1) != 0) && (device.isOnline())) {
				DeviceBridge.setupDeviceForward(device);
				DeviceBridge.pushScriptFiles(device);
			}
		}

		public void deviceConnected(IDevice device) {
			LogUtil.logd(TAG, "device connected, serials number = " + device.getSerialNumber());
			DeviceBridge.setupDeviceForward(device);
			DeviceBridge.pushScriptFiles(device);
			sDeviceList.add(device);
		}

		public void deviceDisconnected(IDevice device) {
			DeviceBridge.removeDeviceForward(device);
			sDeviceList.remove(device);
		}
	}
}
