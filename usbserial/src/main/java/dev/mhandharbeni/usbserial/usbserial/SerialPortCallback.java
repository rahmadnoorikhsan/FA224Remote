package dev.mhandharbeni.usbserial.usbserial;

import java.util.List;

public interface SerialPortCallback {
    void onSerialPortsDetected(List<UsbSerialDevice> serialPorts);
}
