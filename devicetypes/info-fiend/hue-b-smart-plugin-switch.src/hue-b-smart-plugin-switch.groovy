/**
 *  Hue B Smart Plugin Switch
 *
 *  Copyright 2016 Anthony Pastor
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
 *	Version 1 TMLeafs Fork
 *	1.2 Added command flashCoRe for webcore
 *	1.4 Fixed IDE Logging Information + Other Bug Fixes
 *	1.5 Added Light capability for smartapps
 * 
 */
preferences {
	input("tt", "number", title: "Time it takes for the lights to transition (default: 2 = 200ms)")   
	input("notiSetting", "enum", required:true ,title: "Notifications", description: "Level of IDE Notifications for this Device?", options: ["All", "Only On / Off", "None"], defaultValue: "All")
} 

// for the UI
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Hue B Smart Plugin Switch", namespace: "info_fiend", author: "Anthony Pastor") {
	capability "Actuator"
	capability "Switch"
	capability "Polling"
	capability "Refresh"					
	capability "Sensor"
	capability "Configuration"
    	capability "Light"
       
	command "refresh"
	command "updateStatus"
	command "flash"
	command "flashCoRe"
	command "flash_off"
	command "sendToHub"
      
	attribute "lights", "STRING"       
	attribute "transitionTime", "NUMBER"
	attribute "on", "string"
	attribute "reachable", "string"
	attribute "hueID", "string"
	attribute "host", "string"
	attribute "username", "string"
	attribute "idelogging", "string"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"rich-control", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
			}
	}

	standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
		state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
	}
        
        standardTile("flash", "device.flash", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
		state "default", label:"Flash", action:"flash", icon:"st.lights.philips.hue-multi"
	}

	valueTile("reachable", "device.reachable", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
		state "default", label: 'Reachable: ${currentValue}'
	}
        
        valueTile("transitiontime", "device.transitionTime", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
            state "transitiontime", label: 'Transition Time: ${currentValue}'
        }

	}
	main(["rich-control"])
	details(["rich-control","flash", "refresh", "transitiontime", "reachable"])
}


private configure() {		
    def commandData = parent.getCommandData(device.deviceNetworkId)
    log.debug "${commandData = commandData}"
    sendEvent(name: "hueID", value: commandData.deviceId, displayed:true, isStateChange: true)
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

def updated(){
	log.debug "Updated with settings: ${settings}"
	sendEvent(name: "transitionTime", value: tt)
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
def on() {
	if(device.currentValue("idelogging") == "All" || device.currentValue("idelogging") == "OnOff"){
	log.trace "Hue B Smart Plugin Switch: on(): "}

	if(device.currentValue("idelogging") == null){
	idelogs()
	log.trace "IDE Logging Updated" //update old users IDE Logs
	}

	def commandData = parent.getCommandData(device.deviceNetworkId)    
	def tt = device.currentValue("transitionTime") as Integer ?: 0
	def percent = device.currentValue("level") as Integer ?: 100
	    
        return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, transitiontime: tt]
		])
}

def off() {
	if(device.currentValue("idelogging") == "All" || device.currentValue("idelogging") == "OnOff"){
	log.trace "Hue B Smart Plugin Switch: off(): "}
    
	def commandData = parent.getCommandData(device.deviceNetworkId)
	def tt = device.currentValue("transitionTime") as Integer ?: 0
    
	return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
			body: [on: false]
		])
}

/** 
 * capability.polling
 **/
def poll() {
	if(device.currentValue("idelogging") == 'All'){
	log.trace "Hue B Smart Plugin Switch: poll(): "}
    refresh()
}

/**
 * capability.refresh
 **/
def refresh() {
	if(device.currentValue("idelogging") == 'All'){
	log.trace "Hue B Smart Plugin Switch: refresh(): "}
	parent.doDeviceSync()
	//configure()
}

/**
 * capability.alert (flash)
 **/

def flash() {
	if(device.currentValue("idelogging") == 'All'){
	log.trace "Hue B Smart Plugin Switch: flash(): "}
	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "lselect"]
		])
	)
    
    runIn(5, flash_off)
}

def flashCoRe() {
	if(device.currentValue("idelogging") == 'All'){
	log.trace "Hue B Smart Plugin Switch: flashCoRe(): "}
	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "lselect"]
		])
	)
    
    runIn(5, flash_off)
}

def flash_off() {
	if(device.currentValue("idelogging") == 'All'){
	log.trace "Hue B Smart Plugin Switch: flash_off(): "}
    
	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "none"]
		])
	)
}
              
/**
 * Update Status
 **/
private updateStatus(action, param, val) {
	//log.trace "Hue B Smart Plugin Switch: updateStatus ( ${param}:${val} )"
	if (action == "state") {
	def idelogging = device.currentValue("idelogging")
        def curValue
		switch(param) {
        	case "on":
            	curValue = device.currentValue("switch")
            	if (val == true) {
       	         	if (curValue != on) {
                    	if(idelogging == "All" || idelogging == "OnOff"){
                		log.debug "Update Needed: Current Value of switch = false & newValue = ${val}"}
                		sendEvent(name: "switch", value: on, displayed: true, isStateChange: true)                	     
				} else {
				//log.debug "NO Update Needed for switch."                	
        	        }
                } else {
       	         	if (curValue != off) {
                    	if(idelogging == "All" || idelogging == "OnOff"){
                		log.debug "Update Needed: Current Value of switch = true & newValue = ${val}"}               	                	                
		            	sendEvent(name: "switch", value: off, displayed: true)
				sendEvent(name: "effect", value: "none", displayed: false, isStateChange: true)    
				} else {
		  		//log.debug "NO Update Needed for switch."                	
	                }
                }    
                break
            case "reachable":
			if (val == true){
			sendEvent(name: "reachable", value: true, displayed: false, isStateChange: true)
			}else{
			sendEvent(name: "reachable", value: false, displayed: false, isStateChange: true)
                }
                break
            case "transitiontime":
		curValue = device.currentValue("transitionTime")
                if (curValue != val) {
			if(idelogging == 'All'){ 
               		log.debug "Update Needed: Current Value of transitionTime = ${curValue} & newValue = ${val}"}                	
	            	sendEvent(name: "transitionTime", value: val, displayed: true, isStateChange: true)
                	} else {
	     		//log.debug "NO Update Needed for transitionTime."                	
                }    
                break
            case "alert":
            	if (val == "none" && idelogging == "All") {
            		log.debug "Not Flashing"            		
                	} else if(val != "none" && idelogging == "All")  {
                	log.debug "Flashing"
                }
                break
			default: 
				log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}


def getDeviceType() { return "Plugin Switch" }
