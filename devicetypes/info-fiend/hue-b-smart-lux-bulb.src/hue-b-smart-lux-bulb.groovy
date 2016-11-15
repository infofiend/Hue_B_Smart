/**
 *  Hue B Smart Lux Bulb
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
 *	Version 1.0b - fixed icon to single
 * 
 */

// for the UI
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Hue B Smart Lux Bulb", namespace: "info_fiend", author: "Anthony Pastor") {
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        capability "Configuration"
       
        command "reset"
        command "refresh"
        command "updateStatus"
        command "flash"
        command "flash_off"
		command "ttUp"
        command "ttDown"
        command "setTransitionTime"
       
       	attribute "lights", "STRING"       
		attribute "transitionTime", "NUMBER"
		attribute "bri", "number"
		attribute "on", "string"
        attribute "reachable", "string"
        attribute "hueID", "string"
        attribute "host", "string"
        attribute "username", "string"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"rich-control", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00FFFF", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00FFFF", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            
            
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel", range:"(0..100)"
            }

			
		}

		standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Reset", action:"reset", icon:"st.lights.philips.hue-multi"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        /* Flash / Alert */
		standardTile("flash", "device.flash", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Flash", action:"flash", icon:"st.lights.philips.hue-multi"
		}
        
                
        /* transition time */
		valueTile("ttlabel", "transitionTime", decoration: "flat", width: 2, height: 1) {
			state "default", label:'Transition: ${currentValue}00ms     '
		}
		valueTile("ttdown", "device.transitionTime", decoration: "flat", width: 2, height: 1) {
			state "default", label: "Transition -", action:"ttDown"
		}
		valueTile("ttup", "device.transitionTime", decoration: "flat", width: 2, height: 1) {
			state "default", label:"Transition +", action:"ttUp"
		}
        
        /* misc */
        valueTile("hueID", "device.hueID", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label: 'BulbID: ${currentValue}'
		}    
        
        valueTile("host", "device.host", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Host: ${currentValue}'
        }
        valueTile("username", "device.username", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
			state "default", label: 'User: ${currentValue}'
        }

	}
	main(["rich-control"])
	details(["rich-control","hueID","ttlabel","ttup","ttdown","flash","reset","refresh"]) //  "host", "username", 
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


def ttUp() {
	def tt = this.device.currentValue("transitionTime") ?: 0

    log.debug "ttup ${tt}"
    sendEvent(name: "transitionTime", value: tt + 1)
}

def ttDown() {
	def tt = this.device.currentValue("transitionTime") ?: 0
    tt = tt - 1
    if (tt < 0) { tt = 0 }
    log.debug "ttdown ${tt}"
    sendEvent(name: "transitionTime", value: tt)
}


/** 
 * capability.switchLevel 
 **/
def setLevel(level) {
	def lvl = parent.scaleLevel(level, true, 254)
	log.debug "Setting level to ${lvl}."

    def commandData = parent.getCommandData(device.deviceNetworkId)
    log.debug "commandData = ${commandData}."
    
    def tt = this.device.currentValue("transitionTime") ?: 0
    log.debug "transitionTime = ${tt}."
    
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on:true, bri: lvl, transitiontime: tt]
		])
	)    
}

/** 
 * capability.switch
 **/
def on() {
	log.debug "Executing 'on'"

    def commandData = parent.getCommandData(device.deviceNetworkId)
	//log.debug "commandData = ${commandData}."
    
	def tt = device.currentValue("transitionTime") as Integer ?: 0
    log.debug "transitionTime = ${tt}."
    def percent = device.currentValue("level") as Integer ?: 100
    def lvl = parent.scaleLevel(percent, true, 254)
    log.debug "level = ${lvl}."    
    
        return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: lvl, transitiontime: tt]
		])
//	)
	parent.doDeviceSync()
}

def off() {
	log.debug "Executing 'off"
    def commandData = parent.getCommandData(device.deviceNetworkId)
    log.debug "commandData = ${commandData}."
    def tt = device.currentValue("transitionTime") as Integer ?: 0
    log.debug "transitionTime = ${tt}."
    
    //parent.sendHubCommand(
    return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: false, transitiontime: tt]
		])
//	)
	parent.doDeviceSync()
}

/** 
 * capability.polling
 **/
def poll() {
	refresh()
}

/**
 * capability.refresh
 **/
def refresh() {
	parent.doDeviceSync()
    configure()
}

def reset() {
    setLevel(75)
}

def flash() {
	log.debug "Flashing..."
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
	log.debug "Flash off."
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

                
def updateStatus(action, param, val) {
	log.debug "Received Lux Bulb status from Hue Hub: ${param}:${val}"
	if (action == "state") {
		switch(param) {
        	case "on":
            	def onoff
            	if (val == true) {
                	sendEvent(name: "switch", value: on, isStateChange: true)                	     
                
                } else {
	            	sendEvent(name: "switch", value: off)
                	sendEvent(name: "alert", value: "none", isStateChange: true)    
                }    
                break
            case "bri":
            	sendEvent(name: "level", value: parent.scaleLevel(val)) 
                break
            case "transitiontime":
            	sendEvent(name: "transitionTime", value: val, isStateChange: true)
                break                
/**            case "alert":
            	if (val == "none") {
            		flash_off() 	//sendEvent(name: "alert", value: val, isStateChange: true)
                } else {
                	flash()
                }
                break
**/                
			case "reachable":
				sendEvent(name: "reachable", value: val, isStateChange: true)
				break
			default: 
				log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}


def getDeviceType() { return "lux bulb" }
