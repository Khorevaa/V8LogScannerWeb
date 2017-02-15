package org.v8LogScanner.cmdScanner;

import java.util.List;

import org.v8LogScanner.LocalTCPLogScanner.LanScanProfile;
import org.v8LogScanner.LocalTCPLogScanner.V8LogScannerClient;
import org.v8LogScanner.cmdAppl.CmdCommand;

public class CmdAddLogLocOwn implements CmdCommand{

	@Override
	public String getTip() {
		return "";
	}

	@Override
	public void execute() {
			
		V8LogScannerAppl appl = V8LogScannerAppl.instance();
		
		List<V8LogScannerClient> clients = appl.clientsManager.getClients();
		
		String userInput = appl.cmdAppl.askInputFromList("select host:", clients);
		
		if (userInput == null)
			return;
		
		V8LogScannerClient client = clients.get(Integer.parseInt(userInput));
		
		String[] message = {"Input 1c log location (directory or *.log file)"};
		String path = appl.cmdAppl.askInput(
			message,
			n -> client.logPathExist(n), true);
		if (path != null) {
			LanScanProfile profile =  (LanScanProfile) client.getProfile();
			appl.addLogPath(profile, path);
		}
	}
	
}
