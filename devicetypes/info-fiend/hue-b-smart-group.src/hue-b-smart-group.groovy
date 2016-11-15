/**
 *  Hue B Smart Group
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
 *	Version 1.1 -- fixed Hue and Saturation updates, added hue and saturation tiles & sliders, added Flash tile, conformed device layout with Hue Bulb DTH, 
 * 					and added setHuefrom100 function (for using a number 1-100 in CoRE - instead of the 1-360 that CoRE normally uses)
 * 
 */
 
 
metadata {
	definition (name: "Hue B Smart Group", namespace: "info_fiend", author: "Anthony Pastor") {
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
        command "updateStatus"
        command "flash"
		command "ttUp"
        command "ttDown"
        command "setColorTemperature"
        command "setTransitionTime"
        command "colorloopOn"
        command "colorloopOff"
        command "getHextoXY"
        command "setHue"
        command "setHueUsing100"               
        command "setSaturation"
        command "setColor"
        command "setLevel"

        
        attribute "lights", "STRING"       
		attribute "transitionTime", "NUMBER"
        attribute "colorTemp", "number"
		attribute "bri", "number"
		attribute "saturation", "number"
//		attribute "reachable", "string"
		attribute "hue", "number"
		attribute "on", "string"
		attribute "colormode", "enum", ["XY", "CT", "HS"]
		attribute "effect", "enum", ["none", "colorloop"]
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
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-multi", backgroundColor:"#00FFFF", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-multi", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-multi", backgroundColor:"#00FFFF", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-multi", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            
            
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel", range:"(0..100)"
            }

			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setAdjustedColor"
			}
		}

		standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-multi"
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
		valueTile("valueCT", "device.colorTemp", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "colorTemp", label: 'Color Temp:  ${currentValue}'
        }
        controlTile("colorTemp", "device.colorTemp", "slider", inactiveLabel: false,  width: 4, height: 1, range:"(2000..6500)") { 
        	state "setCT", action:"setColorTemperature"
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
			state "default", label: "TT -100ms", action:"ttDown"
		}
		valueTile("ttup", "device.transitionTime", decoration: "flat", width: 2, height: 1) {
			state "default", label:"TT +100ms", action:"ttUp"
		}
        
        /* misc */
        valueTile("lights", "device.lights", inactiveLabel: false, decoration: "flat", width: 5, height: 1) {
			state "default", label: 'Lights: ${currentValue}'
        }
        valueTile("groupID", "device.groupID", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "default", label: 'GroupID: ${currentValue}'
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
	details(["rich-control","colormode","groupID","valueHue","hue","valueSat","saturation","valueCT","colorTemp","ttlabel","ttdown","ttup","lights","toggleColorloop","flash","reset","refresh"]) //  "host", "username", 
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
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on:true, bri: lvl, transitiontime: tt]
		])
	)    
}

/**
 * capability.colorControl 
 **/
def setColor(value) {
	log.trace "setColor(${value})"
	def validValues = [:]
	def commandData = parent.getCommandData(device.deviceNetworkId)       

        def sendBody = [:]

	if (value.level) {
	    log.debug "value.level: ${value.level}"
    	def bri = value.level //?: this.device.currentValue("level")
	    log.debug "bri: ${bri}" 
    	validValues.bri = parent.scaleLevel(bri, true, 254)
	    log.debug "validValues.bri: ${validValues.bri}"
        sendBody["bri"] = validValues.bri
		if (value.level > 0) { 
        	sendBody["on"] = true
        } else {
        	sendBody["on"] = false
		}            
	}

	if (value.switch == "off" ) {
    	sendBody["on"] = false
    } else if (value.switch == "on") {
		sendBody["on"] = true
	}
        
    sendBody["transitiontime"] = device.currentValue("transitionTime") as Integer ?: 0
    
    if (value.hex != null) {
		if (value.hex ==~ /^\#([A-Fa-f0-9]){6}$/) {
			validValues.xy = getHextoXY(value.hex)
            sendBody["xy"] = validValues.xy
		} else {
            log.warn "$value.hex is not a valid color"
        }
	}

    if (validValues.xy) {
    
		log.debug "setColor: XY value found.  Sending ${sendBody} " //[on:${validValues.on}, xy:${validValues.xy}, bri:${validValues.bri}, transitiontime:${validValues.tt}]"

		parent.sendHubCommand(new physicalgraph.device.HubAction(
    		[
        		method: "PUT",
				path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
		        headers: [
		        	host: "${commandData.ip}"
				],
	        	body: sendBody 	//[on: validValues.on, xy: validValues.xy, bri: validValues.bri, transitiontime: validValues.tt]
			])
		)
		sendEvent(name: "colormode", value: "XY", isStateChange: true) 
        sendEvent(name: "hue", value: value.hue as Integer) //, isStateChange: true) 
        sendEvent(name: "saturation", value: value.saturation as Integer, isStateChange: true) 
	} else {
    	log.trace "setColor: no XY values, so using Hue & Saturation."
		def hue = value.hue ?: this.device.currentValue("hue")
    	validValues.hue = parent.scaleLevel(hue, true, 65535)
		sendBody["hue"] = validValues.hue
		def sat = value.saturation ?: this.device.currentValue("saturation")
	    validValues.saturation = parent.scaleLevel(sat, true, 254)
		sendBody["sat"] = validValues.saturation
        
		log.debug "Sending ${sendBody} " 	//[on: ${validValues.on}, hue:${validValues.hue}, sat:${validValues.saturation}, bri:${validValues.bri}, transitiontime:${validValues.tt}]"

		parent.sendHubCommand(new physicalgraph.device.HubAction(
    		[
    	    	method: "PUT",
				path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        	headers: [
	    	    	host: "${commandData.ip}"
				],
		        body: sendBody 	//[on: validValues.on, hue: validValues.hue, sat: validValues.saturation, bri: validValues.bri, transitiontime: validValues.tt]
			])
		)    
		sendEvent(name: "colormode", value: "HS") //, isStateChange: true) 
        sendEvent(name: "hue", value: value.hue) //, isStateChange: true) 
        sendEvent(name: "saturation", value: value.saturation, isStateChange: true) 
    }
}

def setHue(hue) {
	def sat = this.device.currentValue("saturation") ?: 100
//    def level = this.device.currentValue("level") ?: 100
	setColor([saturation:sat, hue:hue])
}

def setSaturation(sat) {
	def hue = this.device.currentValue("hue") ?: 70
//    def level = this.device.currentValue("level") ?: 100
	setColor([saturation:sat, hue:hue])
}

def setHueUsing100(hue) {
	if (hue > 100) { hue = 100 }
    if (hue < 0) { hue = 0 }
	def sat = this.device.currentValue("saturation") ?: 100
//    def level = this.device.currentValue("level") ?: 100
	setColor([saturation:sat, hue:hue])
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
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
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
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
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
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
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
	log.debug "Resetting color."
//    sendEvent(name: "hue", value: 23, displayed, false)
//    sendEvent(name: "sat", value: 56, displayed, false, isStateChange: true)
    def value = [level:100, saturation:56, hue:23]
    setColor(value)
}

def flash() {
	log.debug "Flashing..."
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
	log.debug "Flash off."
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

                
def updateStatus(action, param, val) {
	log.debug "Receiving Group state from Hue Hub: ${param}:${val}"
	if (action == "action") {
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
//                parent.updateGroupBulbs(this.device.currentValue("lights"), "bri", val)
                break
			case "hue":
            	sendEvent(name: "hue", value: parent.scaleLevel(val, false, 65535)) // parent.scaleLevel(val))
  //              parent.updateGroupBulbs(this.device.currentValue("lights"), "bri", val)                
                break
            case "sat":
            	sendEvent(name: "saturation", value: parent.scaleLevel(val)) //parent.scaleLevel(val))
    //            parent.updateGroupBulbs(this.device.currentValue("lights"), "bri", val)
                break
			case "ct": 
            	sendEvent(name: "colorTemp", value: Math.round(1000000/val))  //Math.round(1000000/val))
                break
            case "xy": 
            	
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

void setAdjustedColor(value) {
	log.trace "setAdjustedColor(${value}) ."
	if (value) {

        def adjusted = [:]
        adjusted = value 
    
        value.level = this.device.currentValue("level") ?: 100
        if (value.level > 100) value.level = 100 // null
        log.debug "adjusted = ${adjusted}"
        setColor(value)
    } else {
		log.warn "Invalid color input"
	}
}

def getDeviceType() { return "groups" }

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
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
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
			path: "/api/${commandData.username}/groups/${commandData.deviceId}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on:true, effect: "none", transitiontime: tt]
		])
	)
}

private getHextoXY(String colorStr) {
    // For the hue bulb the corners of the triangle are:
    // -Red: 0.675, 0.322
    // -Green: 0.4091, 0.518
    // -Blue: 0.167, 0.04

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