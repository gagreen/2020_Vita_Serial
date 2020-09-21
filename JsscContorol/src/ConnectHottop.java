import jssc.*;

public class ConnectHottop {
	static SerialPort serialPort;
		
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		serialPort = new SerialPort("COM4");
		try {
			serialPort.openPort();
			
			serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			byte[] a = {2, 7, 85, 77, 0, 1, 1, 3, 80};
			serialPort.writeBytes(a);
			serialPort.closePort();
			
			System.out.println("YEAH");
			
		} catch (SerialPortException e) {
			// TODO: handle exception
			System.out.println(e);
		}

	}

}
