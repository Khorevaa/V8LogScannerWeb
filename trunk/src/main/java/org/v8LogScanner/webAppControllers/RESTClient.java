package org.v8LogScanner.webAppControllers;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.v8LogScanner.rgx.RegExp;
import org.v8LogScanner.rgx.RegExp.EventTypes;
import org.v8LogScanner.rgx.RegExp.PropTypes;
import org.v8LogScanner.rgx.ScanProfile;
import org.v8LogScanner.rgx.ScanProfile.GroupTypes;
import org.v8LogScanner.logs.LogsOperations;

@RestController
@CrossOrigin
public class RESTClient {
  
  @Autowired
  private ScanProfile profile;
  
  public RESTClient(ScanProfile profile){
    this.profile = profile;
  }

  @RequestMapping(value="/groupTypes", method = RequestMethod.GET)
  public GroupTypes[] getAllGroupTypes() {
      return GroupTypes.values();
  }

  @RequestMapping(value="/logPaths", method = RequestMethod.GET,
      produces="application/json")
  public @ResponseBody List<String> getLogPaths() {
    return profile.getLogPaths();
  }

  @RequestMapping(value="/scanLogPaths", method = RequestMethod.GET,
    produces="application/json")
  public @ResponseBody List<String> scanLogsInCfgFile() {
    return LogsOperations.scanLogsInCfgFile();
  }

  @RequestMapping(value = "/eventTypes", method = RequestMethod.GET,
    produces = "application/json")
  public @ResponseBody EventTypes[] eventTypes(){
    return EventTypes.values();
  }

  @RequestMapping(value = "/propTypes", method = RequestMethod.GET,
      produces = "application/json")
  public ResponseEntity<List<PropTypes>> propTypes(@RequestParam(value= "event")String event){
    
    EventTypes eventType = null;
    try { 
      eventType = EventTypes.valueOf(event);
    }
    catch (Exception e){
      return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }
    RegExp rgx = new RegExp(eventType);
    
    return new ResponseEntity<>(rgx.getPropsForFiltering(), HttpStatus.OK);
  }

  @RequestMapping(value = "/startRgxOp", method = RequestMethod.POST)
  public void startRgxOp(@RequestBody String eventData ){
  }

}
