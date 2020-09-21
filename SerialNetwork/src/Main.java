
public class Main {
	public static void main(String[] args) {
		try {
			(new Serial()).connect("COM3");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
