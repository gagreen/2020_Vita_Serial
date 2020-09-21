import java.io.InputStream;
import java.io.OutputStream;

import gnu.io.*;

public class Serial {
	
	public Serial() {
		super();
	}
	
	void connect(String portName) throws Exception {
		CommPortIdentifier portIdentifler = CommPortIdentifier.getPortIdentifier(portName);
		
		if(portIdentifler.isCurrentlyOwned()) {
			System.out.println("Error: Port is currently in use");
		} else { 
			CommPort commPort = portIdentifler.open(this.getClass().getName(), 2000);

			if(commPort instanceof SerialPort) {
				// 포트 설정 (속도, 데이터비트, 정지비트, 패리티)
				SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
				
				// Input, OutputStream 버터 생성 후 오픈
				InputStream in = serialPort.getInputStream();
				OutputStream out = serialPort.getOutputStream();
				
				// 읽기, 쓰기 쓰레드 생성 및 작동
				(new Thread(new SerialReader(in))).start();
                (new Thread(new SerialWriter(out))).start();
			}
			else {
				System.out.println("ERROR: Only serial ports are handled by this example.");
			}
		}
	}
}
