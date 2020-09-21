import jssc.SerialPortList;

public class test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String[] portNames = SerialPortList.getPortNames();
		for(int i=0; i<portNames.length; i++) {
			System.out.println(portNames[i]);
		}

	}

}
