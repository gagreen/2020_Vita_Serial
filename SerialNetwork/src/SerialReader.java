import java.io.IOException;
import java.io.InputStream;

public class SerialReader implements Runnable{
	InputStream in;
	
	public SerialReader(InputStream in) {
		this.in = in;
	}
	
	public void run() {
		byte[] buffer = new byte[1024]; // 가져온 것을 담을 byte 배열 생성 
		int len = -1;
		try {
			while((len=this.in.read(buffer)) > -1) { // 버퍼 안에 데이터가 있을 때까지
				System.out.println(new String(buffer, 0, len)); // 콘솔로 데이터 출력
			}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
