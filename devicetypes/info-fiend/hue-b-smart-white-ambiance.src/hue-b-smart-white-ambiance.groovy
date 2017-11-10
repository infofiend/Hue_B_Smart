/**
 *  Hue B Smart White Ambiance
 *
 *  Copyright 2016 Anthony Pastor
 *
 *  Thanks to @tmleafs for his help on this addition to the Hue B Smart DTHs!
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
 *
 */
preferences {
	input("tt", "integer", defaultValue: 4, title: "Time it takes for the lights to transition (default: 4 = 400ms)")   
	input("notiSetting", "enum", title: "Notifications", description: "Level of Notifications for this Device?",
	    options: ["All", "Only On / Off", "None"] )
}  
 
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
	capability "Health Check"
	capability "Light"
        
        command "reset"
        command "refresh"
        command "flash"
	command "flashCoRe"
        command "flash_off"
        command "setColorTemperature"        
        command "updateStatus"
	command "getHextoXY"
        command "sendToHub"
	command "applyRelax"
        command "applyConcentrate"
        command "applyReading"
        command "applyEnergize"
        command "scaleLevel"

 	attribute "colorTemperature", "number"
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
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#FFFFFF", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#FFFFFF", nextState:"turningOn"
				}
				tileAttribute ("device.level", key: "SLIDER_CONTROL") {
					attributeState "level", action:"switch level.setLevel", range:"(0..100)"
				}

		}

		standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-single"
		}
		
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        valueTile("valueCT", "device.colorTemperature", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "colorTemperature", label: 'Color Temp:  ${currentValue}'
        }
        
		controlTile("colorTemperature", "device.colorTemperature", "slider", inactiveLabel: false,  width: 4, height: 1, range:"(2200..6500)") { 
        	state "setCT", action:"setColorTemperature"
		}
        
        standardTile("flash", "device.flash", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Flash", action:"flash", icon:"st.lights.philips.hue-single"
		}
		
		valueTile("transitiontime", "device.transitionTime", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
            state "transitiontime", label: 'Transitiontime is set to ${currentValue}'
        }     
		
		valueTile("reachable", "device.reachable", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
			state "default", label: 'Reachable: ${currentValue}'
		}
	}
	main(["rich-control"])
	details(["rich-control","valueCT","colorTemp", "colorTemperature","reset","flash","refresh", "transitiontime", "reachable"])
}

void installed() {
	sendEvent(name: "DeviceWatch-Enroll", value: "{\"protocol\": \"LAN\", \"scheme\":\"untracked\", \"hubHardwareId\": \"${device.hub.hardwareID}\"}", displayed: false)
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
	log.trace "Hue B Smart Ambience Bulb: setLevel( ${inLevel} ): "
    
    def level = scaleLevel(inLevel, true, 254)
    def commandData = parent.getCommandData(device.deviceNetworkId)    
    def tt = this.device.currentValue("transitionTime") ?: 0
    
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: level, transitiontime: tt]
		])
	)    
}


def sendToHub(values) {
	log.trace "Hue B Smart Ambience Bulb: sendToHub ( ${values} ): "
    
	def validValues = [:]
	def commandData = parent.getCommandData(device.deviceNetworkId)
    def sendBody = [:]

	if (values.level) {
    	def bri = values.level 
    	validValues.bri = parent.scaleLevel(bri, true, 254)
        sendBody["bri"] = validValues.bri
		if (values.level > 0) { 
        	sendBody["on"] = true
        } else {
        	sendBody["on"] = false
		}            
	}

	if (values.switch == "off" ) {
    	sendBody["on"] = false
    } else if (values.switch == "on") {
		sendBody["on"] = true
	}
        
    sendBody["transitiontime"] = device.currentValue("transitionTime") as Integer ?: 0
    
	if (values.hue || values.saturation ) {
		def hue = values.hue ?: this.device.currentValue("hue")
    	validValues.hue = scaleLevel(hue, true, 65535)
		sendBody["hue"] = validValues.hue
		def sat = values.saturation ?: this.device.currentValue("saturation")
	    validValues.saturation = scaleLevel(sat, true, 254)
		sendBody["sat"] = validValues.saturation
        
	}
    
    log.debug "Sending ${sendBody} "

	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	       	headers: [
	   	    	host: "${commandData.ip}"
			],
	        body: sendBody 
		])
	)    
	sendEvent(name: "colormode", value: "HS") //, isStateChange: true) 
    sendEvent(name: "hue", value: values.hue) //, isStateChange: true) 
    sendEvent(name: "saturation", value: values.saturation, isStateChange: true) 
    
}

def setHue(inHue) {
	log.debug "Hue B Smart Ambience Bulb: setHue()."
        
	def sat = this.device.currentValue("saturation") ?: 100
	sendToHub([saturation:sat, hue:inHue])

}

def setSaturation(inSat) {
	log.debug "Hue B Smart Ambience Bulb: setSaturation( ${inSat} )."

    def hue = this.device.currentValue("hue") ?: 100
	sendToHub([saturation:inSat, hue: hue])
    
}


/**
 * capability.colorTemperature 
**/

def setColorTemperature(inCT) {
	log.debug("Hue B Smart Ambience Bulb: setColorTemperature ( ${inCT} )")
    
    def colorTemp = inCT ?: this.device.currentValue("colorTemperature")
    colorTemp = Math.round(1000000/colorTemp)    
	def commandData = parent.getCommandData(device.deviceNetworkId)
    def tt = device.currentValue("transitionTime") as Integer ?: 0
    
    
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [ct: colorTemp, transitiontime: tt]
		])
	)
}

def applyRelax() {
	log.info "applyRelax"
	setColorTemperature(2141)
}

def applyConcentrate() {
	log.info "applyConcentrate"
    setColorTemperature(4329)
}

def applyReading() {
	log.info "applyReading"
    setColorTemperature(2890)
}

def applyEnergize() {
	log.info "applyEnergize"
    setColorTemperature(6410)
}


/** 
 * capability.switch
 **/
def on() {
	log.debug "Hue B Smart Ambience: on()"

    def commandData = parent.getCommandData(device.deviceNetworkId)    
	def tt = device.currentValue("transitionTime") as Integer ?: 0
    def percent = device.currentValue("level") as Integer ?: 100
    def level = scaleLevel(percent, true, 254)
    
        return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: level, transitiontime: tt]
		])
}

def off() {
	log.debug "Hue B Smart Ambience Bulb: off()"
    def commandData = parent.getCommandData(device.deviceNetworkId)
    def tt = device.currentValue("transitionTime") as Integer ?: 0
    
    //parent.sendHubCommand(
    return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        //body: [on: false, transitiontime: tt]
			body: [on: false]
		])
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
	log.debug "Hue B Smart Ambience Bulb: refresh()."
	parent.doDeviceSync()
    configure()
}

def reset() {
	log.debug "Hue B Smart Ambience Bulb: reset()."
    sendToHub ([level: 70, sat: 56, hue: 23])
}

/**
 * capability.alert (flash)
 **/
def flash() {
	log.debug "Hue B Smart Ambience Bulb: flash()"
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
	log.trace "Hue B Smart Lux Group: flashCoRe(): "
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
	log.debug "Hue B Smart Ambience Bulb: flash_off()"
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
private updateStatus(action, param, val) {
	//log.trace "Hue B Smart White Ambiance: updateStatus ( ${param}:${val} )"
	if (action == "state") {
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
		//                log.debug "NO Update Needed for switch."                	
        	        }

                } else {
       	         	if (curValue != off) { 
                		log.debug "Update Needed: Current Value of switch = true & newValue = ${val}"               	                	                
		            	sendEvent(name: "switch", value: off, displayed: onoffNotice)
    	            	sendEvent(name: "effect", value: "none", displayed: otherNotice, isStateChange: true)    
					} else {
		  //              log.debug "NO Update Needed for switch."                	
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
	      //          log.debug "NO Update Needed for level."                	
                }
                
                break
			case "hue":
            	curValue = device.currentValue("hue")
                val = scaleLevel(val, false, 65535)
                if (val > 100) { val = 100 }
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of hue = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "hue", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	 //               log.debug "NO Update Needed for hue."                	
                }            	
                break
            case "sat":
	            curValue = device.currentValue("saturation")
                val = scaleLevel(val)
                if (val > 100) { val = 100 }                
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of saturation = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "saturation", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	     //           log.debug "NO Update Needed for saturation."                	
                }
                break
			case "ct": 
            	curValue = device.currentValue("colorTemperature")
                val = Math.round(1000000/val)
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of colorTemperature = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "colorTemperature", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	 //               log.debug "NO Update Needed for colorTemperature."                	
                }
                break
            case "xy": 
            	def x = val[0]
                def y = val[1]
                def colorData = [:]
                colorData = colorFromXY(x, y)
                log.debug "colorData from XY = ${colorData}"
                def newHue = Math.round(colorData.hue * 100) /// 100 
                def newSat = Math.round(colorData.saturation * 100) // 100
                log.debug "newHue = ${newHue}, newSat = ${newSat}"
                sendEvent(name: "hue", value: newHue, displayed: true, isStateChange: true) 	//scaleLevel(val, false, 65535)
                sendEvent(name: "saturation", value: newSat, displayed: true, isStateChange: true) //scaleLevel(val)
                break
            case "reachable":
            	if (device.currentValue("reachable") != val) {
					sendEvent(name: "reachable", value: val, displayed: otherNotice, isStateChange: true)
				}
                break   
			case "reachable":
				curValue = device.currentValue("reachable")
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of reachable = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "reachable", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	    //            log.debug "NO Update Needed: Current Value of reachable = ${curValue} & newValue = ${val}"                	
                }				
                break
            case "colormode":
            	curValue = device.currentValue("colormode")
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of colormode = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "colormode", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	      //          log.debug "NO Update Needed for colormode."                	
                }	
                break
            case "transitiontime":
	            curValue = device.currentValue("transitionTime")
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of transitionTime = ${curValue} & newValue = ${val}"                	
	            	sendEvent(name: "transitionTime", value: val, displayed: otherNotice, isStateChange: true)
                } else {
	     //           log.debug "NO Update Needed for transitionTime."                	
                }    
                break
            case "alert":
            	if (val == "none") {
            		log.debug "Not Flashing"            		
                } else {
                	log.debug "Flashing"
                }
                break
            case "effect":
            	curValue = device.currentValue("effect")
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of effect = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "effect", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	    //            log.debug "NO Update Needed for effect "                	
                }
                break
 
			default: 
				log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}

def getDeviceType() { return "lights" }
