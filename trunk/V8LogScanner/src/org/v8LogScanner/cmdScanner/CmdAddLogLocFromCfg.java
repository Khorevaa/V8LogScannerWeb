/////////////////////////////////////
//COMMAND PATTERN

package org.v8LogScanner.cmdScanner;

import java.util.List;
import org.v8LogScanner.LocalTCPLogScanner.LanScanProfile;
import org.v8LogScanner.LocalTCPLogScanner.V8LogScannerClient;
import org.v8LogScanner.cmdAppl.CmdCommand;

public class CmdAddLogLocFromCfg implements CmdCommand{
	
	public void execute() {
		
		V8LogScannerAppl appl = V8LogScannerAppl.instance();
		appl.cmdAppl.out.println();
		
		List<V8LogScannerClient> clients = appl.clientsManager.getClients();
		String input = appl.cmdAppl.askInputFromList("Select numeric index of the server from list above", clients);

		if (input == null)
			return;
		
		V8LogScannerClient client = clients.get(Integer.parseInt(input));
		
		List<String> logsLoc = client.scanLogsInCfgFile();
		
		if (logsLoc == null)
			return;
		
		input = appl.cmdAppl.askInputFromList("Select logs location:", logsLoc);
		
		if (input == null)
			return;
		
		int inputNum = Integer.parseInt(input);
		
		LanScanProfile profile = (LanScanProfile) client.getProfile(); 
		appl.addLogPath(profile, logsLoc.get(inputNum));
	
	}

	public String getTip() {
		return "";
	}
}