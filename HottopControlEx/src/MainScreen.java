import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.*;

import jssc.*;

public class MainScreen extends JFrame{
	/* 시리얼 통신 관련 상수 */
	static SerialPort serialPort;
	
	/* GUI 관련 변수  */
	private JPanel inputPanel;
	private JTextField inputMsg;
	private JButton inputButton;
	
	private JPanel buttonPanel;
	private JButton[] buttons = new JButton[5];
	private String[] buttonTexts = {"0", "1", "2", "3", "4"}; 
	
	private JPanel logPanel;
	private JTextArea logs;
	private JScrollPane scroll;
	
/* inputPanel 관련 이벤트: 송신 버튼을 눌렀을 때 처리 */
	private class SendListener implements ActionListener { 
		@Override
		public void actionPerformed(ActionEvent e) {
			String data = inputMsg.getText();
			if(!data.equals("")) {
				inputMsg.setText("");
				logs.append("Request> " + data + "\n");
				byte[] encodedData = hexStringToByteArray(data);
				sendData(encodedData);
			}
		}
	}
	
/* buttonPanel 관련 이벤트: 버튼을 눌렀을 떄 처리 */
	private class ButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			JButton btn = (JButton) e.getSource();
			String data = btn.getText();
			byte[] encodedData = null;
			logs.append("Request> " + data + "\n");
			if(data.equals("확인")) {
				encodedData = makeProtocol("UR" + Integer.parseInt("10", 16));
			} 
			else {
				encodedData = makeProtocol("UM"+data);
			}
			sendData(encodedData);
		}
	}
	
/* SerialPort 이벤트 리스너 */
	private class SerialPortReader implements SerialPortEventListener {
		@Override
		public void serialEvent(SerialPortEvent event) {	
			byte[] buffer;
			try {
				buffer = serialPort.readBytes(9);
				System.out.println("IN try: " + new String(buffer));
				
				printLogs(buffer);
			} catch (SerialPortException e) {
				System.out.println(e);
			}
		}
	}
	
/* Constructor */
	public MainScreen() {
		super("시리얼 통신 예제");
		
		/* 레이아웃 지정 */
		Container con = getContentPane();
		con.setLayout(new BorderLayout(0, 40));
		
		/* 입력창 생성 */
		inputPanel = new JPanel(new FlowLayout(2));
		inputMsg = new JTextField(43);
		inputButton = new JButton("송신");
		inputMsg.addActionListener(new SendListener());		// 이벤트 추가
		inputButton.addActionListener(new SendListener()); 	// 이벤트 추가
		inputPanel.add(inputMsg);
		inputPanel.add(inputButton);
		
		/* 버튼 생성 */
		buttonPanel = new JPanel(new FlowLayout(5));
		for(int i=0; i<buttons.length; i++) {
			buttons[i] = new JButton(buttonTexts[i]);
			buttons[i].setPreferredSize(new Dimension(80, 80));		// 크기 지정
			buttons[i].setFont(new Font("맑은고딕", Font.BOLD, 20));	// 폰트 지정
			buttons[i].addActionListener(new ButtonListener());
			buttonPanel.add(buttons[i]);
		}
		
		/* 수신 LOG 출력 생성 */
		logPanel = new JPanel();
		logs = new JTextArea(15, 50);
		logs.setEditable(false);
		scroll = new JScrollPane(logs);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		logPanel.add(scroll);
		
		
		/* 컨테이너에 추가하기 */
		con.add(inputPanel, BorderLayout.NORTH);
		con.add(buttonPanel, BorderLayout.CENTER);
		con.add(logPanel, BorderLayout.SOUTH);
		
		/*기본 설정*/
		setSize(600, 500);
		setVisible(true);
		
		/* 시리얼 설정 */
		serialPort = new SerialPort("COM4");
		try {
			serialPort.openPort();
			serialPort.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			printLogs(serialPort.getPortName() + " Port Connected");
			int mask = SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR;
			serialPort.setEventsMask(mask);
			serialPort.addEventListener(new SerialPortReader());
		} catch (Exception e) {
			printLogs("### Failed " + serialPort.getPortName() + " Port Connected ###");
			System.out.println(e);
		}
		
		/* 창을 닫았을 때 설정 */
		addWindowListener(new WindowAdapter() { 

			@Override
			public void windowClosing(WindowEvent e) {
				try {
					serialPort.closePort();
				} catch (Exception ex) {
					System.out.println(ex);
				}
				System.exit(0);
			}
		});
	}
	
/* 장치로 데이터 전송 */
	private void sendData(byte[] data) {
		try {
			serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
			serialPort.writeBytes(data);
		} catch (SerialPortException e) {
			System.out.println(e);
		}
	}
	
/* log 출력 (String) */
	private void printLogs(String str) {
		logs.append("Response> " + str + "\n");
	}
	
/* log 출력 (Byte 배열) */
	private void printLogs(byte[] bytes) {
		String str = byteArrayTOHexString(bytes);
		logs.append("Response> " + str + "\n");
	}
	
/* 통신 프로토콜 생성 */
	public byte[] makeProtocol(String str) {
		int i,j, checkSum=0;
		byte[] data = new byte[8+str.length()-2];
		
		data[0] = 2;													// START
		data[1] = (byte) (data.length - 2);								// LENGTH
		data[2] = (byte) str.charAt(0);									// COMMAND
		data[3] = (byte) str.charAt(1); 								// SUB_COMMAND 
		data[4] = 0;													// ADDRESS
		data[5] = (byte) (str.length() - 2); 							// DATA_LENGTH
		
		for(i=6, j=2; j<str.length(); i++, j++) {
			data[i] = (byte) Integer.parseInt(str.substring(j, j+1)); 	// DATA
		}
		data[i++] = 3;													// END
		
		for(int k=0; k<data.length-1; k++) {							//CHECKSUM
			checkSum += data[k];
		}
		data[i] =(byte)(Integer.parseInt("100", 16) - checkSum);		//CHECKSUM

		return data;
	}
	
/* 16진수 문자열을  16 byte[]로  바꿈 */
	public byte[] hexStringToByteArray(String str) {
		int len = str.length();
		byte[] data = new byte[len/2];
		for(int i=0; i<len; i+= 2) {
			data[i/2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + (Character.digit(str.charAt(i+1), 16)));
		}
		return data;
	}
	
/* 16 byte[]을 16진수 문자열로 바꿈 */
	public String byteArrayTOHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for(byte b: bytes) {
			sb.append(String.format("%02X", b&0xff));
		}
		return sb.toString();
	}
	
/* MAIN */
	public static void main(String[] args) {
		MainScreen m = new MainScreen();
	}

}
