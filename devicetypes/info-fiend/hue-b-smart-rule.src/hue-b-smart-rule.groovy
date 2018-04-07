/**
 *  Hue B Smart Rule
 *
 *  Copyright 2018 Anthony Pastor
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *	Version 1 
 *      
 *	
 */
preferences {	
	input("notiSetting", "enum", required:true ,title: "Notifications", description: "Level of IDE Notifications for this Device?", options: ["All", "Only On / Off", "None"], defaultValue: "All")
}  
 
metadata {
	definition (name: "Hue B Smart Rule", namespace: "info_fiend", author: "Anthony Pastor") {
	capability "Actuator"
	capability "Switch"
	capability "Polling"
	capability "Refresh"
	capability "Sensor"
    capability "Configuration"
	
        
	command "reset"
	command "refresh"
	command "getRuleStatus"
	command "enableStatus"
	command "disableStatus"
	command "doRefresh"

	command "updateStatus"
	        
	attribute "ruleStatus", "STRING"       
	attribute "ruleConditions", "STRING"
    attribute "rawConds", "STRING"
	attribute "ruleActions", "STRING"       
    attribute "rawActs", "STRING"
    attribute "ruleID", "string"
	attribute "host", "string"
	attribute "username", "string"
	attribute "idelogging", "string"
        
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"ruleStatus", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.ruleStatus", key: "PRIMARY_CONTROL") {
				attributeState "enabled", label:'${name}', action:"disableStatus", icon:"st.lights.philips.hue-multi", backgroundColor:"#00a0dc", nextState:"disabling"
				attributeState "disabled", label:'${name}', action:"enableStatus", icon:"st.lights.philips.hue-multi", backgroundColor:"#ffffff", nextState:"enabling"
				attributeState "enabling", label:'${name}', action:"disableStatus", icon:"st.lights.philips.hue-multi", backgroundColor:"#00a0dc", nextState:"disabling"
				attributeState "disabling", label:'${name}', action:"enableStatus", icon:"st.lights.philips.hue-multi", backgroundColor:"#ffffff", nextState:"enabling"
			}
            		
		}

	valueTile("ruleConditions", "device.ruleConditions", inactiveLabel: false, decoration: "flat", width: 6, height: 6) {
		state "default", label: 'CONDITIONS:\n\n${currentValue}'
	}
	valueTile("ruleActions", "device.ruleActions", inactiveLabel: false, decoration: "flat", width: 6, height: 6) {
		state "default", label: 'ACTIONS:\n\n${currentValue}'
	}
    
    valueTile("rawConds", "device.rawConds", inactiveLabel: false, decoration: "flat", width: 6, height: 6) {
		state "default", label: 'CONDITIONS IN RAW:\n\n${currentValue}'
	}
	valueTile("rawActs", "device.rawActs", inactiveLabel: false, decoration: "flat", width: 6, height: 6) {
		state "default", label: 'ACTIONS IN RAW:\n\n${currentValue}'
	}

	valueTile("ruleID", "device.ruleID", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
		state "default", label: 'ruleID: ${currentValue}'
	}		

	standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 3) {
		state "default", label:"", action:"doRefresh", icon:"st.secondary.refresh"
	}
    
	}
	main(["ruleStatus"])
	details(["ruleStatus","ruleID","ruleConditions","ruleActions","refresh"])	// "ruleStatus",
}

private configure() {		
    def commandData = parent.getCommandData(device.deviceNetworkId)
    log.debug "${commandData = commandData}"
    sendEvent(name: "ruleID", value: commandData.deviceId, displayed:true, isStateChange: true)
    sendEvent(name: "host", value: commandData.ip, displayed:false, isStateChange: true)
    sendEvent(name: "username", value: commandData.username, displayed:false, isStateChange: true)
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def installed() {
	log.debug "Installed with settings: ${settings}"	
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	idelogs()
}

def idelogs() {
	if (notiSetting == null || notiSetting == "Only On / Off"){
    	sendEvent(name: "idelogging", value: "OnOff")
    	}else if(notiSetting == "All"){
    	state.IDELogging = All
    	sendEvent(name: "idelogging", value: "All")
    	}else {
    	sendEvent(name: "idelogging", value: "None")
    	}
}

def initialize() {
	state.xy = [:]
    if (notiSetting == null){sendEvent(name: "idelogging", value: "OnOff")}   
}


/** 
 * capability.switch
 **/
def enableStatus() {
	if(device.currentValue("idelogging") == "All" || device.currentValue("idelogging") == "OnOff"){
	log.trace "Hue B Smart Rule: on(): enable Rule on Hue Hub"}
	
	if(device.currentValue("idelogging") == null){
	idelogs()
	log.trace "IDE Logging Updated" //update old users IDE Logs
	}
		
    def commandData = parent.getCommandData(device.deviceNetworkId)
  //  log.trace "commandData Rule ${commandData.deviceId} = ${commandData}"
    def sendBody = [:]
    sendBody = ["status": "enabled"]
    
    parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/rules/${commandData.deviceId}",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: sendBody
		])
	)   
    
//	sendEvent(name: "ruleStatus", value: "enabled", displayed: true, isStateChange: true)
}

def disableStatus() {
	if(device.currentValue("idelogging") == "All" || device.currentValue("idelogging") == "OnOff"){
		log.trace "Hue B Smart Rule: off(): disable Rule on Hue Hub"
    }
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
    def sendBody = [:]
    sendBody = ["status": "disabled"]
    
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/rules/${commandData.deviceId}",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: sendBody
		])
	)
 //   sendEvent(name: "ruleStatus", value: "disabled", displayed: true, isStateChange: true)
}

/** 
 * capability.polling
 **/
def poll() {
	if(device.currentValue("idelogging") == 'All'){
	log.trace "Hue B Smart Rule: poll(): "}
	doRefresh()
}

/**
 * capability.refresh
 **/

/**
def refresh() {
	if(device.currentValue("idelogging") == 'All'){
		log.trace "Hue B Smart Rule: refresh(): "
    }

    def commandData = parent.getCommandData(device.deviceNetworkId)
    
	new physicalgraph.device.HubAction(
    	[
			method: "GET",
			path: "/api/${commandData.username}/rules/${commandData.deviceId}",
	        headers: [
	        	host: "${commandData.ip}"
			]
    	]
    )        
}
**/

def doRefresh() {
	if(device.currentValue("idelogging") == 'All'){
		log.trace "Hue B Smart Rule: doRefresh(): "
    }

    def commandData = parent.getCommandData(device.deviceNetworkId)
    
	parent.doRulesSync()

}



                
/**
 * Update Status
 **/
private updateStatus(action, param, val) {
//	log.debug "Hue B Smart Rule: updateStatus ( ${param}:${val} )"
	if (action == "rule") {
		def idelogging = device.currentValue("idelogging")
		def curValue
        def curValueSw
		switch(param) {
        	case "status":
            	curValue = device.currentValue("ruleStatus")  
				if (val != curValue) {
	            log.debug "Hue B Smart Rule: updating Status to ${val}"
					sendEvent(name: "ruleStatus", value: val, displayed: true, isStateChange: true) 
        		}      
            	break
                
            case "conditions":
            	
                curValue = device.currentValue("rawConds")  
				if (val != curValue) {
					def newCond = []
    		        def newCondSum = ""
	        	    def x = 1
            		val.each { con ->
            	    	newCondSum = newCondSum + "Condition ${x}:\n Address: ${con.address}\n Operator: ${con.operator}\n"
        	            if (con.value) {
    	                   	newCondSum = newCondSum + "Value: ${con.value}\n\n"  
	                    } else {
	                	   	newCondSum = newCondSum + "\n"
	            	    }    
						x=x+1
        		    }
					log.debug "Hue B Smart Rule: updating Conditions" // to ${newCondSum}"    
    	            sendEvent(name: "ruleConditions", value: newCondSum, displayed: false, isStateChange: true) 			
	                sendEvent(name: "rawConds", value: val, displayed: false, isStateChange: true)
                }
                break
            case "actions":
            	curValue = device.currentValue("rawActs")  
				if (val != curValue) {
					def newAct = []
					def newActSum = ""
        	        def x = 1
            	    val.each { act ->
                	    newActSum = newActSum + "Action ${x}:\n Address: ${act.address}\n Body: ${act.body}\n\n"  
	                	x=x+1
					}
					log.debug "Hue B Smart Rule: updating Actions" // to ${newActSum}"    
        	        sendEvent(name: "ruleActions", value: newActSum, displayed: false, isStateChange: true) 			
            	    sendEvent(name: "rawActs", value: val, displayed: false, isStateChange: true)
                 }   
                	break

            default: 
		log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}


def getDeviceType() { return "rules" }