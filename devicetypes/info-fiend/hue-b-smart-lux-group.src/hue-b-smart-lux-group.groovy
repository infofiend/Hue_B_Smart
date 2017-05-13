/**
 *  Hue B Smart Lux Group
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
 *
 *	Version 1.1 -- Conformed DTHs

 */
preferences {
	input("tt", "integer", defaultValue: 4, title: "Time it takes for the lights to transition (default: 4 = 400ms)")
    input("notiSetting", "enum", title: "Notifications", description: "Level of Notifications for this Device?",
	    options: ["All", "Only On / Off", "None"] )
}  
 
metadata {
	definition (name: "Hue B Smart Lux Group", namespace: "info_fiend", author: "Anthony Pastor") {
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
        command "sendToHub"
        command "setLevel"
        command "scaleLevel"
                       
        attribute "lights", "STRING"       
		attribute "transitionTime", "NUMBER"
		attribute "bri", "number"
        attribute "level", "number"
		attribute "on", "string"
        attribute "groupID", "string"
        attribute "host", "string"
        attribute "username", "string"
        
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"rich-control", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-multi", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-multi", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-multi", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-multi", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            
            
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel", range:"(0..100)"
            }

			
		}

		/* reset / refresh */	
		standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-multi"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        /* Flash / Alert */
		standardTile("flash", "device.flash", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Flash", action:"flash", icon:"st.lights.philips.hue-multi"
		}
        valueTile("transitiontime", "device.transitionTime", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
            state "transitiontime", label: 'Transitiontime is set to ${currentValue}'
        }

	}
	main(["rich-control"])
	details(["rich-control","reset","flash", "refresh", "transitiontime"])
}

private configure() {		
    def commandData = parent.getCommandData(device.deviceNetworkId)
    log.debug "${commandData = commandData}"
    sendEvent(name: "groupID", value: commandData.deviceId, displayed:true, isStateChange: true)
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
	log.debug "Updated Preferences"
	sendEvent(name: "transitionTime", value: tt)
    initialize()
}

def initialize() {
	state.xy = [:]

    state.notiSetting1 = true
    state.notiSetting2 = true
    log.trace "Initialize(): notiSetting is ${notiSetting}"
	if (notiSetting == "All" ) {
    
    } else if (notiSetting == "Only On / Off" ) {
   		state.notiSetting2 = false

	} else if (notiSetting == "None" ) {
		state.notiSetting1 = false
	    state.notiSetting2 = false
    }    
    log.debug "state.notiSetting1 = ${state.notiSetting1}"
    log.debug "state.notiSetting2 = ${state.notiSetting2}"    
}

/** 
 * capability.switchLevel 
 **/
def setLevel(inLevel) {
	log.trace "Hue B Smart Lux Group: setLevel ( ${inLevel} ): "
	def level = scaleLevel(inLevel, true, 254)

    def commandData = parent.getCommandData(device.deviceNetworkId)
    def tt = this.device.currentValue("transitionTime") ?: 0
    
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: level, transitiontime: tt]
		])
	)    
}


/** 
 * capability.switch
 **/
def on() {
	log.trace "Hue B Smart Lux Group: on(): "

    def commandData = parent.getCommandData(device.deviceNetworkId)
	def tt = device.currentValue("transitionTime") as Integer ?: 0
    def percent = device.currentValue("level") as Integer ?: 100
    def level = scaleLevel(percent, true, 254)
    
        return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: level, transitiontime: tt]
		])
}

def off() {
	log.trace "Hue B Smart Lux Group: off(): "
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
    def tt = device.currentValue("transitionTime") as Integer ?: 0
    
    //parent.sendHubCommand(
    return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        //body: [on: false, transitiontime: tt]
			body: [on: false]
		])
//	)
}

/** 
 * capability.polling
 **/
def poll() {
	log.trace "Hue B Smart Lux Group: poll(): "
	refresh()
}

/**
 * capability.refresh
 **/
def refresh() {
	log.trace "Hue B Smart Lux Group: refresh(): "
	parent.doDeviceSync()
    configure()
}

def reset() {
	log.trace "Hue B Smart Lux Group: reset(): "

	def value = [level:70, saturation:56, hue:23]
    sendToHub(value)
}

/**
 * capability.alert (flash)
 **/

def flash() {
	log.trace "Hue B Smart Lux Group: flash(): "
    def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "lselect"]
		])
	)
    
    runIn(5, flash_off)
}

def flash_off() {
	log.trace "Hue B Smart Lux Group: flash_off(): "
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "none"]
		])
	)
}

/**
 * scaleLevel
 **/
def scaleLevel(level, fromST = false, max = 254) {
//	log.trace "scaleLevel( ${level}, ${fromST}, ${max} )"
    /* scale level from 0-254 to 0-100 */
    
    if (fromST) {
        return Math.round( level * max / 100 )
    } else {
    	if (max == 0) {
    		return 0
		} else { 	
        	return Math.round( level * 100 / max )
		}
    }    
//    log.trace "scaleLevel returned ${scaled}."
    
}
                
/**
 * Update Status
 **/
/**
private updateStatus(action, param, val) {
	log.trace "Hue B Smart Lux Group: updateStatus ( ${param}:${val} )"
	if (action == "action") {
		switch(param) {
        	case "on":
            	def onoff
            	if (val == true) {
                	sendEvent(name: "switch", value: on, displayed:false, isStateChange: true)                	     
                
                } else {
	            	sendEvent(name: "switch", value: off, displayed:false)
                	sendEvent(name: "alert", value: "none", displayed:false, isStateChange: true)    
                }    
                break
            case "bri":
            	sendEvent(name: "level", value: parent.scaleLevel(val)) //parent.scaleLevel(val, true, 255))
                break
            case "transitiontime":
            	sendEvent(name: "transitionTime", value: val, displayed:false, isStateChange: true)
                break                
            case "alert":
            	if (val == "none") {
            		flash_off() 
                } else {
                	flash()
                }
                break
			case "lights":
            	sendEvent(name: "lights", value: val, displayed:false, isStateChange: true)
                break
            case "scene":
            	log.trace "received scene ${val}"
                break    
			default: 
				log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}
**/

/**
 * Update Status
 **/
private updateStatus(action, param, val) {
	//log.trace "Hue B Lux Group: updateStatus ( ${param}:${val} )"
	if (action == "action") {
    	def onoffNotice = state.notisetting1
    	def otherNotice = state.notisetting2        
        def curValue
		switch(param) {
        	case "on":
            	curValue = device.currentValue("switch")
                def onoff
            	if (val == true) {
       	         	if (curValue != on) { 
                		log.debug "Update Needed: Current Value of switch = false & newValue = ${val}"
                		sendEvent(name: "switch", value: on, displayed: onoffNotice, isStateChange: true)                	     
					} else {
		              //log.debug "NO Update Needed for switch."                	
        	        }

                } else {
       	         	if (curValue != off) { 
                		log.debug "Update Needed: Current Value of switch = true & newValue = ${val}"               	                	                
		            	sendEvent(name: "switch", value: off, displayed: onoffNotice)
    	            	sendEvent(name: "effect", value: "none", displayed: otherNotice, isStateChange: true)    
					} else {
		               //log.debug "NO Update Needed for switch."                	
	                }

                }    
                break
            case "bri":
	            curValue = device.currentValue("level")
                val = scaleLevel(val)
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of level = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "level", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	                //log.debug "NO Update Needed for level."                	
                }
                
                break
            case "transitiontime":
	            curValue = device.currentValue("transitionTime")
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of transitionTime = ${curValue} & newValue = ${val}"                	
	            	sendEvent(name: "transitionTime", value: val, displayed: otherNotice, isStateChange: true)
                } else {
	                //log.debug "NO Update Needed for transitionTime."                	
                }    
                break
            case "alert":
            	if (val == "none") {
            		flash_off() 
                } else {
                	flash()
                }
                break
			case "lights":
            	curValue = device.currentValue("lights")
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of lights = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "lights", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	               //log.debug "NO Update Needed for lights"
                }
                break
            case "scene":
            	log.trace "received scene ${val}"
                break 
			default: 
				log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}

def getDeviceType() { return "lux group" }
