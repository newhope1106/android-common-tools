package cn.appleye.server;

import java.util.ArrayList;

public class ToolsServer {
	public static void main(String[] args) {
		DeviceManager.setupDevicesChangedListener();
		ArrayList<String> devicesSerialNumbers = DeviceManager.getDeviceSerialNumberList();
		
		for (String serialNumber : devicesSerialNumbers) {
			System.out.println("serialNumber = " + serialNumber);
		}
	}

}
