/**
 *  Hue B Smart White Ambiance
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
 */
metadata {
	definition (name: "Hue B Smart White Ambiance", namespace: "info_fiend", author: "Anthony Pastor") {
		capability "Switch Level"
		capability "Actuator"
	        capability "Color Temperature"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        	capability "Configuration"
        
        command "reset"
        command "refresh"
        command "flash"
        command "flash_off"
        command "setTransitionTime"
        command "ttUp"
        command "ttDown"
        command "updateStatus"
		command "getHextoXY"

 		attribute "colorTemp", "number"
		attribute "bri", "number"
		attribute "sat", "number"
        attribute "level", "number"
		attribute "reachable", "string"
		attribute "hue", "number"
		attribute "on", "string"
        attribute "transitionTime", "NUMBER"
        attribute "hueID", "STRING"
        attribute "host", "STRING"
        attribute "hhName", "STRING"
		attribute "colormode", "enum", ["XY", "CT", "HS"]
        attribute "effect", "enum", ["none", "colorloop"]
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"rich-control", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#C6C7CC", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#C6C7CC", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel", range:"(0..100)"
			}

		}

		/* reset / refresh */	
		standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-single"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        /* Color Temperature */
		valueTile("valueCT", "device.colorTemp", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "colorTemp", label: 'Color Temp:  ${currentValue}'
        }
        controlTile("colorTemp", "device.colorTemp", "slider", inactiveLabel: false,  width: 4, height: 1, range:"(2000..6500)") { 
        	state "setCT", action:"setColorTemperature"
		}
        
        /* alert / flash */
		standardTile("flash", "device.flash", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Flash", action:"flash", icon:"st.lights.philips.hue-single"
		}
        
        /* transition time */
		valueTile("ttlabel", "transitionTime", decoration: "flat", width: 4, height: 1) {
			state "default", label:'Transition Time: ${currentValue}00ms'
		}
		valueTile("ttdown", "device.transitionTime", decoration: "flat", width: 1, height: 1) {
			state "default", label: "-", action:"ttDown"
		}
		valueTile("ttup", "device.transitionTime", decoration: "flat", width: 1, height: 1) {
			state "default", label:"+", action:"ttUp"
		}
        
        /* misc */
        
        valueTile("hueID", "device.hueID", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "default", label: 'ID: ${currentValue}'
		}
		valueTile("colormode", "device.colormode", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
			state "default", label: 'Colormode: ${currentValue}'
		}
        valueTile("host", "device.host", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
			state "default", label: 'Host: ${currentValue}'
        }
        valueTile("username", "device.username", inactiveLabel: false, decoration: "flat", width: 3, height: 1) {
			state "default", label: 'User: ${currentValue}'
        }

	}
	main(["rich-control"])
	details(["rich-control","ttlabel","ttdown","ttup","valueCT","colorTemp","hueID","reset","refresh"])	//  "host", "username",
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
    if (tt == null) { tt = 4 }
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
	        body: [on: true, bri: lvl, transitiontime: tt]
		])
	)    
}

def setHue(hue) {
	def sat = this.device.currentValue("sat") ?: 100
    def level = this.device.currentValue("level") ?: 100
	setColor([level:level, saturation:sat, hue:hue])
}

def setSaturation(sat) {
	def hue = this.device.currentValue("hue") ?: 70
    def level = this.device.currentValue("level") ?: 100
	setColor([level:level, saturation:sat, hue:hue])
}

/**
 * capability.colorTemperature 
 **/
def setColorTemperature(temp) {
	log.debug("Setting color temperature to ${temp}")
    
    def ct = temp ?: this.device.currentValue("colorTemp")
    ct = Math.round(1000000/temp)
    
	def commandData = parent.getCommandData(device.deviceNetworkId)
    def tt = device.currentValue("transitionTime") as Integer ?: 0
    
    
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on:true, ct: ct, transitiontime: tt]
		])
	)
    sendEvent(name: "colormode", value: "CT", isStateChange: true) 
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
	log.debug "Resetting color."
//    sendEvent(name: "hue", value: 23, displayed, false)
//    sendEvent(name: "sat", value: 56, displayed, false, isStateChange: true)
    def value = [level:100, sat:56, hue:23]
    setColor(value)
}

/**
 * capability.alert (flash)
 **/
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


/**
 * Update Status
 **/
def updateStatus(action, param, val) {
	log.debug "updating status: ${param}:${val}"
	if (action == "state") {
		switch(param) {
        	case "on":
            	def onoff
            	if (val == true) {
                	sendEvent(name: "switch", value: on, isStateChange: true)                	     
                
                } else {
	            	sendEvent(name: "switch", value: off)
                	sendEvent(name: "effect", value: "none", isStateChange: true)    
                }    
                break
            case "bri":
            	sendEvent(name: "level", value: parent.scaleLevel(val)) //parent.scaleLevel(val, true, 255))
                break
			case "hue":
            	sendEvent(name: "hue", value: parent.scaleLevel(val)) //parent.scaleLevel(val, false, 65535))
                break
            case "sat":
            	sendEvent(name: "sat", value: parent.scaleLevel(val)) //parent.scaleLevel(val))
                break
			case "ct": 
            	sendEvent(name: "colorTemp", value: Math.round(1000000/val))  //Math.round(1000000/val))
                break
            case "xy": 
            	
                break    
			case "reachable":
				sendEvent(name: "reachable", value: val, isStateChange: true)
				break
            case "colormode":
            	sendEvent(name: "colormode", value: val, isStateChange: true)
                break
            case "transitiontime":
            	sendEvent(name: "transitionTime", value: val, isStateChange: true)
                break
            case "effect":
            	sendEvent(name: "effect", value: val, isStateChange: true)
                break
			case "alert":
            	if (val == "none") {
            		flash_off() 	//sendEvent(name: "alert", value: val, isStateChange: true)
                } else if (val == "lselect") {
                	flash_on()
                }
                break
    
			default: 
				log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}

def getDeviceType() { return "lights" }
