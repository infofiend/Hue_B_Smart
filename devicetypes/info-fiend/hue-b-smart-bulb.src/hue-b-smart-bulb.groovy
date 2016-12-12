/**
 *  Hue B Smart Bulb
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
 *	Version 1.1 -- fixed Hue and Saturation updates, added hue and saturation tiles & sliders, added Flash tile, conformed device layout with Hue Group DTH, 
 * 					and added setHuefrom100 function (for using a number 1-100 in CoRE - instead of the 1-360 that CoRE normally uses)
 *
 *	Version 1.2 -- Conformed DTHs
 *
 *	Version 1.2b -- attribute colorTemp is now colorTemperature - changing colorTemperature no longer turns on device
 */

 preferences {
    input("notiSetting", "enum", title: "Notifications", description: "Level of Notifications for this Device?",
	    options: ["All", "Only On / Off", "None"] )
}
 
metadata {
	definition (name: "Hue B Smart Bulb", namespace: "info_fiend", author: "Anthony Pastor") {
		capability "Switch Level"
		capability "Actuator"
		capability "Color Control"
        capability "Color Temperature"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        capability "Configuration"
        
        command "setAdjustedColor"
        command "reset"
        command "refresh"
        command "flash"
        command "flash_off"
        command "setTransitionTime"
        command "ttUp"
        command "ttDown"
        command "colorloopOn"
		command "colorloopOff"
        command "updateStatus"
		command "getHextoXY"
        command "setHue"
        command "setHueUsing100"               
        command "setSaturation"
        command "sendToHub"
        command "setLevel"
        command "setColorTemperature"

 		attribute "colorTemperature", "number"
		attribute "bri", "number"
		attribute "saturation", "number"
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
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setAdjustedColor"
			}
		}

		/* reset / refresh */	
		standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-single"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
        /* Hue & Saturation */
		valueTile("valueHue", "device.hue", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "hue", label: 'Hue: ${currentValue}'
        }
        controlTile("hue", "device.hue", "slider", inactiveLabel: false,  width: 4, height: 1) { 
        	state "setHue", action:"setHue"
		}
		valueTile("valueSat", "device.saturation", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "saturation", label: 'Sat: ${currentValue}'
        }
        controlTile("saturation", "device.saturation", "slider", inactiveLabel: false,  width: 4, height: 1) { 
        	state "setSaturation", action:"setSaturation"
		}
        
        
        /* Color Temperature */
		valueTile("valueCT", "device.colorTemperature", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "colorTemperature", label: 'Color Temp:  ${currentValue}'
        }
        controlTile("colorTemperature", "device.colorTemperature", "slider", inactiveLabel: false,  width: 4, height: 1, range:"(2200..6500)") { 
        	state "setCT", action:"setColorTemperature"
		}
        
        /* alert / flash */
		standardTile("flash", "device.flash", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Flash", action:"flash", icon:"st.lights.philips.hue-single"
		}
        
        /* transition time */
		valueTile("ttlabel", "transitionTime", decoration: "flat", width: 2, height: 1) {
			state "default", label:'Transition Time: ${currentValue}00ms'
		}
		valueTile("ttdown", "device.transitionTime", decoration: "flat", width: 2, height: 1) {
			state "default", label: "TT -100ms", action:"ttDown"
		}
		valueTile("ttup", "device.transitionTime", decoration: "flat", width: 2, height: 1) {
			state "default", label:"TT +100ms", action:"ttUp"
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

        standardTile("toggleColorloop", "device.effect", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "colorloop", label:"On", action:"colorloopOff", nextState: "updating", icon:"https://raw.githubusercontent.com/infofiend/Hue-Lights-Groups-Scenes/master/smartapp-icons/hue/png/colorloop-on.png"
            state "none", label:"Off", action:"colorloopOn", nextState: "updating", icon:"https://raw.githubusercontent.com/infofiend/Hue-Lights-Groups-Scenes/master/smartapp-icons/hue/png/colorloop-off.png"
            state "updating", label:"Working", icon: "st.secondary.secondary"
		}
	}
	main(["rich-control"])
	details(["rich-control","colormode","hueID","valueHue", "hue", "valueSat", "saturation", "valueCT","colorTemp","ttlabel","ttdown","ttup","toggleColorloop","flash", "reset","refresh"])	//  "host", "username",
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

def updated() {
	log.debug "Updated with settings: ${settings}"
	
	initialize()
}

def initialize() {
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

def ttUp() {
	log.trace "Hue B Smart Bulb: ttUp(): "
    def tt = this.device.currentValue("transitionTime") ?: 0
    if (tt == null) { tt = 4 }
    log.debug "ttup ${tt}"
    sendEvent(name: "transitionTime", value: tt + 1, displayed: state.notiSetting2)
}

def ttDown() {
	log.trace "Hue B Smart Bulb: ttDown(): "
	def tt = this.device.currentValue("transitionTime") ?: 0
    tt = tt - 1
    if (tt < 0) { tt = 0 }
    sendEvent(name: "transitionTime", value: tt, displayed: state.notiSetting2)
}

/** 
 * capability.switchLevel 
 **/
def setLevel(inLevel) {
	log.trace "Hue B Smart Bulb: setLevel ( ${inLevel} ): "
	def level = parent.scaleLevel(inLevel, true, 254)
	log.debug "Setting level to ${level}."

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

/**
 * capability.colorControl 
 **/
def sendToHub(values) {
	log.trace "Hue B Smart Bulb: sendToHub ( ${values} ): "
    
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
    
    if (values.hex != null) {
		if (values.hex ==~ /^\#([A-Fa-f0-9]){6}$/) {
			validValues.xy = getHextoXY(values.hex)
            sendBody["xy"] = validValues.xy
		} else {
            log.warn "$values.hex is not a valid color"
        }
	}

    if (validValues.xy) {
    
		log.debug "XY value found.  Sending ${sendBody} " 

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
        
		sendEvent(name: "colormode", value: "XY", displayed: state.notiSetting2, isStateChange: true) 
        sendEvent(name: "hue", value: values.hue as Integer, displayed: state.notiSetting2) 
        sendEvent(name: "saturation", value: values.saturation as Integer, displayed: state.notiSetting2, isStateChange: true) 
        
	} else {
    
    	log.trace "sendToHub: no XY values, so using Hue & Saturation."
		def hue = values.hue ?: this.device.currentValue("hue")
    	validValues.hue = parent.scaleLevel(hue, true, 65535)
		sendBody["hue"] = validValues.hue
		def sat = values.saturation ?: this.device.currentValue("saturation")
	    validValues.saturation = parent.scaleLevel(sat, true, 254)
		sendBody["sat"] = validValues.saturation
        
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
		sendEvent(name: "colormode", value: "HS", displayed: state.notiSetting2) //, isStateChange: true) 
        sendEvent(name: "hue", value: values.hue, displayed: state.notiSetting2) //, isStateChange: true) 
        sendEvent(name: "saturation", value: values.saturation, displayed: state.notiSetting2, isStateChange: true) 
    }
}

def setHue(inHue) {
	log.debug "Hue B Smart Bulb: setHue( ${inHue} )."
    
    def sat = this.device.currentValue("saturation") ?: 100
	sendToHub([saturation:sat, hue:inHue])
}

def setSaturation(inSat) {
	log.debug "Hue B Smart Bulb: setSaturation( ${inSat} )."
    
	def hue = this.device.currentValue("hue") ?: 70
	sendToHub([saturation:inSat, hue:hue])
}

def setHueUsing100(inHue) {
	log.debug "Hue B Smart Bulb: setHueUsing100( ${inHue} )."
    
	if (inHue > 100) { inHue = 100 }
    if (inHue < 0) { inHue = 0 }
	def sat = this.device.currentValue("saturation") ?: 100

	sendToHub([saturation:sat, hue:inHue])
}


/**
 * capability.colorTemperature 
 **/
def setColorTemperature(inCT) {
	log.trace "Hue B Smart Bulb: setColorTemperature ( ${inCT} ): "
    
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
	        body: [on:true, ct: colorTemp, transitiontime: tt]
		])
	)
    sendEvent(name: "colormode", value: "CT", displayed: state.notiSetting2, isStateChange: true) 
}

/** 
 * capability.switch
 **/
def on() {
	log.trace "Hue B Smart Bulb: on(): "

    def commandData = parent.getCommandData(device.deviceNetworkId)    
	def tt = device.currentValue("transitionTime") as Integer ?: 0
    def percent = device.currentValue("level") as Integer ?: 100
    def level = parent.scaleLevel(percent, true, 254)
    
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
	log.trace "Hue B Smart Bulb: on(): "
    
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
	        body: [on: false, transitiontime: tt]
		])
//	)
}

/** 
 * capability.polling
 **/
def poll() {
	log.trace "Hue B Smart Bulb: poll(): "
    refresh()
}

/**
 * capability.refresh
 **/
def refresh() {
	log.trace "Hue B Smart Bulb: refresh(): "
    parent.doDeviceSync()
    configure()
}

def reset() {
	log.trace "Hue B Smart Bulb: reset(): "

	def value = [level:100, saturation:56, hue:23]
    sendToHub(value)
}

/**
 * capability.alert (flash)
 **/

def flash() {
	log.trace "Hue B Smart Bulb: flash(): "
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
	log.trace "Hue B Smart Bulb: flash_off(): "
    
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
	log.trace "Hue B Smart Bulb: updateStatus ( ${param}:${val} )"
	if (action == "state") {
    	def onoffNotice = state.notisetting1
    	def otherNotice = state.notisetting2        
		switch(param) {
        	case "on":
            	def onoff
            	if (val == true) {
                	sendEvent(name: "switch", value: on, displayed: onoffNotice, isStateChange: true)                	     
                
                } else {
	            	sendEvent(name: "switch", value: off, displayed: onoffNotice)
                	sendEvent(name: "effect", value: "none", displayed: otherNotice, isStateChange: true)    
                }    
                break
            case "bri":
            	sendEvent(name: "level", value: parent.scaleLevel(val), displayed: otherNotice, isStateChange: true) 
                break
			case "hue":
            	sendEvent(name: "hue", value: parent.scaleLevel(val, false, 65535), displayed: otherNotice, isStateChange: true) 
                break
            case "sat":
            	sendEvent(name: "saturation", value: parent.scaleLevel(val), displayed: otherNotice, isStateChange: true) //parent.scaleLevel(val))
                break
			case "ct": 
            	sendEvent(name: "colorTemperature", value: Math.round(1000000/val), displayed: otherNotice, isStateChange: true)  //Math.round(1000000/val))
                break
            case "xy": 
            	
                break    
			case "reachable":
				sendEvent(name: "reachable", value: val, displayed: otherNotice, isStateChange: true)
				break
            case "colormode":
            	sendEvent(name: "colormode", value: val, displayed: otherNotice, isStateChange: true)
                break
            case "transitiontime":
            	sendEvent(name: "transitionTime", value: val, displayed: otherNotice, isStateChange: true)
                break
            case "effect":
            	sendEvent(name: "effect", value: val, displayed: otherNotice, isStateChange: true)
                break
 
			default: 
				log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}

void setAdjustedColor(value) {
	log.trace "setAdjustedColor(${value}) ."
	if (value) {

        def adjusted = [:]
        adjusted = value     
        value.level = device.currentValue("level") ?: 100 // null
        sendToHub(value)
        
    } else {
		log.warn "Invalid color input"
	}
}


/**
 * capability.colorLoop
 **/
void colorloopOn() {
    log.debug "Executing 'colorloopOn'"
    def tt = device.currentValue("transitionTime") as Integer ?: 0
    
    def dState = device.latestValue("switch") as String ?: "off"
	def level = device.currentValue("level") ?: 100
    if (level == 0) { percent = 100}

    sendEvent(name: "effect", value: "colorloop", isStateChange: true)
    
	def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on:true, effect: "colorloop", transitiontime: tt]
		])
	)
}

void colorloopOff() {
    log.debug "Executing 'colorloopOff'"
    def tt = device.currentValue("transitionTime") as Integer ?: 0
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
    sendEvent(name: "effect", value: "none", isStateChange: true)    
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on:true, effect: "none", transitiontime: tt]
		])
	)
}


/**
 * Misc
 **/
private getHextoXY(String colorStr) {

    def cred = Integer.valueOf( colorStr.substring( 1, 3 ), 16 )
    def cgreen = Integer.valueOf( colorStr.substring( 3, 5 ), 16 )
    def cblue = Integer.valueOf( colorStr.substring( 5, 7 ), 16 )

    double[] normalizedToOne = new double[3];
    normalizedToOne[0] = (cred / 255);
    normalizedToOne[1] = (cgreen / 255);
    normalizedToOne[2] = (cblue / 255);
    float red, green, blue;

    // Make red more vivid
    if (normalizedToOne[0] > 0.04045) {
       red = (float) Math.pow(
                (normalizedToOne[0] + 0.055) / (1.0 + 0.055), 2.4);
    } else {
        red = (float) (normalizedToOne[0] / 12.92);
    }

    // Make green more vivid
    if (normalizedToOne[1] > 0.04045) {
        green = (float) Math.pow((normalizedToOne[1] + 0.055) / (1.0 + 0.055), 2.4);
    } else {
        green = (float) (normalizedToOne[1] / 12.92);
    }

    // Make blue more vivid
    if (normalizedToOne[2] > 0.04045) {
        blue = (float) Math.pow((normalizedToOne[2] + 0.055) / (1.0 + 0.055), 2.4);
    } else {
        blue = (float) (normalizedToOne[2] / 12.92);
    }

    float X = (float) (red * 0.649926 + green * 0.103455 + blue * 0.197109);
    float Y = (float) (red * 0.234327 + green * 0.743075 + blue * 0.022598);
    float Z = (float) (red * 0.0000000 + green * 0.053077 + blue * 1.035763);

    float x = (X != 0 ? X / (X + Y + Z) : 0);
    float y = (Y != 0 ? Y / (X + Y + Z) : 0);

    double[] xy = new double[2];
    xy[0] = x;
    xy[1] = y;
    return xy;
}

def getDeviceType() { return "lights" }