package test;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.v8LogScanner.LocalTCPConnection.SocketTemplates;
import org.v8LogScanner.LocalTCPLogScanner.ClientsManager;
import org.v8LogScanner.LocalTCPLogScanner.V8LanLogScannerClient;
import org.v8LogScanner.LocalTCPLogScanner.V8LogScannerClient;
import org.v8LogScanner.LocalTCPLogScanner.LanScanProfile;
import org.v8LogScanner.commonly.ExcpReporting;
import org.v8LogScanner.rgx.RegExp;
import org.v8LogScanner.rgx.RegExp.EventTypes;
import org.v8LogScanner.rgx.ScanProfile;

//@RunWith(MockitoJUnitRunner.class)
public class testV8LogScannerClient {
	
	@Test
	public void tesCreateClientFromHostIP() throws Exception {
		
		ExcpReporting.out = System.out;
		
		String host = "localhost";
		String hostIP = "127.0.0.1";
		
		String res_ip = SocketTemplates.instance().getHostIP(host);
		
		String res_host = SocketTemplates.instance().getHostName(hostIP);
		
		assertNotNull(res_ip);
		assertEquals(res_ip, hostIP);
		
		assertNotNull(res_host);
		
		V8LanLogScannerClient clientIP = new V8LanLogScannerClient(res_ip);
		assertEquals(clientIP.getHostIP(), hostIP);
		
		V8LanLogScannerClient clientName = new V8LanLogScannerClient(res_host);
		assertEquals(clientName.getHostIP(), hostIP);
		
	}
	
	@Test
	public void addClientByIPorHost() throws Exception{
		
		ExcpReporting.out = System.out;
		
		//String host = "Kudakov-R";
		//String hostIP = "10.70.2.97";
		
		//V8LanLogScannerClient client = mock(V8LanLogScannerClient.class);
		//when(client.pingServer()).thenReturn(true);
		
		
		
	//	assertTrue(client.pingServer());
		
	}
	
	@Test
	public void scanProfileSHouldContainEssentialParameters() throws Exception {
		
		ClientsManager clm = new ClientsManager();
		
		V8LogScannerClient clientLocal = clm.localClient();
		LanScanProfile profile = (LanScanProfile) clientLocal.getProfile();
		profile.getLogPaths().add("c:\\localFile");
		profile.getRgxList().add(new RegExp(EventTypes.EXCP));
		
		V8LanLogScannerClient clientLan = new V8LanLogScannerClient("11.11.11"); 
		clm.addClient(clientLan);
			
		clientLan.setProfile(profile);
		LanScanProfile lanProfile = (LanScanProfile) clientLan.getProfile();
		lanProfile.getLogPaths().add("c:\\lanFile");
		ScanProfile profile2 = profile.clone();
		profile2.setLogPaths(clientLan.getProfile().getLogPaths());	
		clientLan.setProfile(profile2);
		
		List<String> logFiles = new ArrayList<>();
		List<RegExp> events = new ArrayList<>();
		
		clm.forEach(client -> {
			logFiles.addAll(client.getProfile().getLogPaths());
			events.addAll(client.getProfile().getRgxList());
		});
		assertEquals(logFiles.stream().distinct().count(), 2);
		
		long eventsCount = events
		.stream()
		.map(n -> n.getEventType())
		.distinct().count();
		assertEquals(eventsCount, 1l);
		
	}
	
}
