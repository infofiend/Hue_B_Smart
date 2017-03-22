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
 *
 *	Version 1.3 -- re-added setColor; hue slider range now 1-100; now always sends xy color values to Hue Hub; Sliders now conform to the colormode; 
 * 			   	turning colorLoop off now returns to the previous colormode and settings. 
 *
 *	Version 1.3b -- When light is off, adjustments to hue / saturation or colorTemp (WITHOUT a level or switch command) will not be sent to Hue Hub.  Instead, the DTH will save
 *				those settings and apply if light is then turned on.  
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
        command "colorFromHSB"
        command "colorFromHex"
        command "colorFromXY"
        command "getHSfromRGB"
        command "pivotRGB"
        command "revPivotRGB"
        command "setHue"
        command "setHueUsing100"               
        command "setSaturation"
        command "sendToHub"
        command "setLevel"
        command "setColor"
        command "setColorTemperature"
        command "applyRelax"
        command "applyConcentrate"
        command "applyReading"
        command "applyEnergize"

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
		attribute "colormode", "enum", ["XY", "CT", "HS", "LOOP"]
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
				attributeState "level", label: 'Level: ${currentValue}', action:"switch level.setLevel", range:"(0..100)"
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
            state "-1", label: 'Hue: N/A'   
        }
        controlTile("hue", "device.hue", "slider", inactiveLabel: false,  width: 4, height: 1) { 
        	state "setHue", action:"setHue", range:"(1..100)"
		}
		valueTile("valueSat", "device.saturation", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "saturation", label: 'Sat: ${currentValue}'
            state "-1", label: 'Sat: N/A'   
        }
        controlTile("saturation", "device.saturation", "slider", inactiveLabel: false,  width: 4, height: 1) { 
        	state "setSaturation", action:"setSaturation"
		}
        
        
        /* Color Temperature */
		valueTile("valueCT", "device.colorTemperature", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "colorTemperature", label: 'Color Temp:  ${currentValue}'
            state "-1", label: 'Color Temp: N/A'   
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
		standardTile("ttdown", "device.transitionTime", decoration: "flat", width: 2, height: 1) {
			state "default", label: "TT -100ms", action:"ttDown"
		}
		standardTile("ttup", "device.transitionTime", decoration: "flat", width: 2, height: 1) {
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
			state "colorloop", label:"Color Loop On", action:"colorloopOff", nextState: "updating", icon:"https://raw.githubusercontent.com/infofiend/Hue-Lights-Groups-Scenes/master/smartapp-icons/hue/png/colorloop-on.png"
            state "none", label:"Color Loop Off", action:"colorloopOn", nextState: "updating", icon:"https://raw.githubusercontent.com/infofiend/Hue-Lights-Groups-Scenes/master/smartapp-icons/hue/png/colorloop-off.png"
            state "updating", label:"Working", icon: "st.secondary.secondary"
		}
	}
	main(["rich-control"])
	details(["rich-control","colormode","hueID","valueHue", "hue", "valueSat", "saturation", "valueCT","colorTemperature","ttlabel","ttdown","ttup","toggleColorloop","flash", "reset","refresh"])	//  "host", "username",
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

def setTransitionTime(transTime) {
	log.trace "Hue B Smart Bulb: setTransitionTime( ${transTime} ): "
    sendEvent(name: "transitionTime", value: transTime, displayed: state.notiSetting2)
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
    
    def sendBody = [:]
    sendBody = ["on": true, "bri": level, "transitiontime": tt]
    if (state.xy) {
    	sendBody["xy"] = state.xy 
        state.xy = [:]
    } else if (state.ct) {
    	sendBody["ct"] = state.ct as Integer
        state.ct = null
    }
    
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
}

/**
 * capability.colorControl 
 **/
def sendToHub(values) {
	log.trace "Hue B Smart Bulb: sendToHub ( ${values} ): "
    
	def validValues = [:]
	def commandData = parent.getCommandData(device.deviceNetworkId)
    def sendBody = [:]

	def bri
	if (values.level) {
    	bri = values.level 
    	validValues.bri = parent.scaleLevel(bri, true, 254)
        sendBody["bri"] = validValues.bri
        
		if (values.level > 0) { 
        	sendBody["on"] = true
        } else {
        	sendBody["on"] = false
		}            
	} else {
    	bri = device.currentValue("level") as Integer ?: 100
    } 

	if (values.switch == "off" ) {
    	sendBody["on"] = false
    } else if (values.switch == "on") {
		sendBody["on"] = true
	}

    sendBody["transitiontime"] = device.currentValue("transitionTime") as Integer ?: 0
    
    def isOn = this.device.currentValue("switch")
    if (values.switch == "on" || values.level || isOn == "on") {
    	state.xy = [:]
        state.ct = null
        
	    if (values.hex != null) {
			if (values.hex ==~ /^\#([A-Fa-f0-9]){6}$/) {
				validValues.xy = colorFromHex(values.hex)		// getHextoXY(values.hex)
            	sendBody["xy"] = validValues.xy
			} else {
    	        log.warn "$values.hex is not a valid color"
        	}
		} else if (values.xy) {
        	validValues.xy = values.xy
		}
		
    	if (validValues.xy ) {
    
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

        	sendEvent(name: "colorTemperature", value: -1, displayed: false, isStateChange: true)
            
		} else {
    		def h = values.hue.toInteger()
        	def s = values.saturation.toInteger()
	    	log.trace "sendToHub: no XY values, so get from Hue & Saturation."
			validValues.xy = colorFromHSB(h, s, bri) 	//values.hue, values.saturation)		// getHextoXY(values.hex)
        	sendBody["xy"] = validValues.xy
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
        
	        sendEvent(name: "colorTemperature", value: -1, displayed: false, isStateChange: true)
    	   
    	}
	} else {
    	if (values.hue && values.saturation) {
            validValues.xy = colorFromHSB(values.hue, values.saturation, bri)
          	log.debug "Light off, so saving xy value ${validValues.xy} for later."   
            state.xy = validValues.xy
            state.ct = null
            
            sendEvent(name: "colormode", value: "HS", displayed: state.notiSetting2) //, isStateChange: true) 
            sendEvent(name: "hue", value: values.hue, displayed: state.notiSetting2) //, isStateChange: true) 
        	sendEvent(name: "saturation", value: values.saturation, displayed: state.notiSetting2, isStateChange: true) 
            sendEvent(name: "colorTemperature", value: -1, displayed: false, isStateChange: true)
        }    
    }    
        
}
def setColor(inValues) {   
    log.debug "Hue B Smart Bulb: setColor( ${inValues} )."
   
	sendToHub(inValues)
}	

def setHue(inHue) {
	log.debug "Hue B Smart Bulb: setHue( ${inHue} )."
    def sat = this.device.currentValue("saturation") ?: 100
    if (sat == -1) { sat = 100 }
	sendToHub([saturation:sat, hue:inHue])
}

def setSaturation(inSat) {
	log.debug "Hue B Smart Bulb: setSaturation( ${inSat} )."
	def hue = this.device.currentValue("hue") ?: 70
	if (hue == -1) { hue = 70 }        
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
    
    def isOn = this.device.currentValue("switch")
    if (isOn == "on") {
    	
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

	state.ct = null

    } else {
    	state.ct = colorTemp
	}

	sendEvent(name: "colormode", value: "CT", displayed: state.notiSetting2, isStateChange: true) 
    sendEvent(name: "hue", value: -1, displayed: false, isStateChange: true)
   	sendEvent(name: "saturation", value: -1, displayed: false, isStateChange: true)
    sendEvent(name: "colorTemperature", value: inCT, displayed: otherNotice, isStateChange: true)
    
	state.xy = [:]   
        
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
	log.trace "Hue B Smart Bulb: on(): "

    def commandData = parent.getCommandData(device.deviceNetworkId)    
	def tt = device.currentValue("transitionTime") as Integer ?: 0
    def percent = device.currentValue("level") as Integer ?: 100
    def level = parent.scaleLevel(percent, true, 254)
    
    def sendBody = [:]
    sendBody = ["on": true, "bri": level, "transitiontime": tt]
    if (state.xy) {
    	sendBody["xy"] = state.xy
        state.xy = [:]
    } else if (state.ct) {
    	sendBody["ct"] = state.ct
        state.ct = null
    }
            
    return new physicalgraph.device.HubAction(
    [
       	method: "PUT",
		path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	    headers: [
	       	host: "${commandData.ip}"
		],
	    body: sendBody
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
		                log.debug "NO Update Needed for switch."                	
        	        }

                } else {
       	         	if (curValue != off) { 
                		log.debug "Update Needed: Current Value of switch = true & newValue = ${val}"               	                	                
		            	sendEvent(name: "switch", value: off, displayed: onoffNotice)
    	            	sendEvent(name: "effect", value: "none", displayed: otherNotice, isStateChange: true)    
					} else {
		                log.debug "NO Update Needed for switch."                	
	                }

                }    
                break
            case "bri":
	            curValue = device.currentValue("level")
                val = parent.scaleLevel(val)
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of level = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "level", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	                log.debug "NO Update Needed for level."                	
                }
                
                break
			case "hue":
            	curValue = device.currentValue("hue")
                val = parent.scaleLevel(val, false, 65535)
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of hue = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "hue", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	                log.debug "NO Update Needed for hue."                	
                }            	
                break
            case "sat":
	            curValue = device.currentValue("saturation")
                val = parent.scaleLevel(val)
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of saturation = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "saturation", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	                log.debug "NO Update Needed for saturation."                	
                }
                break
			case "ct": 
            	curValue = device.currentValue("colorTemperature")
                val = Math.round(1000000/val)
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of colorTemperature = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "colorTemperature", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	                log.debug "NO Update Needed for colorTemperature."                	
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
                sendEvent(name: "hue", value: newHue, displayed: true, isStateChange: true) 	//parent.scaleLevel(val, false, 65535)
                sendEvent(name: "saturation", value: newSat, displayed: true, isStateChange: true) //parent.scaleLevel(val)
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
	                log.debug "NO Update Needed: Current Value of reachable = ${curValue} & newValue = ${val}"                	
                }				
                break
            case "colormode":
            	curValue = device.currentValue("colormode")
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of colormode = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "colormode", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	                log.debug "NO Update Needed for colormode."                	
                }	
                break
            case "transitiontime":
	            curValue = device.currentValue("transitionTime")
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of transitionTime = ${curValue} & newValue = ${val}"                	
	            	sendEvent(name: "transitionTime", value: val, displayed: otherNotice, isStateChange: true)
                } else {
	                log.debug "NO Update Needed for transitionTime."                	
                }    
                break                
            case "effect":
            	curValue = device.currentValue("effect")
                if (curValue != val) { 
               		log.debug "Update Needed: Current Value of effect = ${curValue} & newValue = ${val}" 
	            	sendEvent(name: "effect", value: val, displayed: otherNotice, isStateChange: true) 
				} else {
	                log.debug "NO Update Needed for effect "                	
                }
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
def colorloopOn() {
    log.debug "Executing 'colorloopOn'"
    def tt = device.currentValue("transitionTime") as Integer ?: 0
    
    def dState = device.latestValue("switch") as String ?: "off"
	def level = device.currentValue("level") ?: 100
    if (level == 0) { percent = 100}
    
    def dMode = device.currentValue("colormode") as String
    if (dMode == "CT") {
    	state.returnTemp = device.currentValue("colorTemperature")
    } else {
	    state.returnHue = device.currentValue("hue")
	    state.returnSat = device.currentValue("saturation")        
    }
    state.returnMode = dMode    

    sendEvent(name: "effect", value: "colorloop", isStateChange: true)
    sendEvent(name: "colormode", value: "LOOP", isStateChange: true)
    
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
    
    sendEvent(name: "hue", value: -1, displayed: false, isStateChange: true)
    sendEvent(name: "saturation", value: -1, displayed: false, isStateChange: true)
    sendEvent(name: "colorTemperature", value: -1, displayed: false, isStateChange: true)
        
}

def colorloopOff() {
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
    
    if (state.returnMode == "CT") {
    	def retCT = state.returnTemp as Integer
        setColorTemperature(retCT)
    } else {
    	def retHue = state.returnHue as Integer
        def retSat = state.returnSat as Integer
        setColor(hue: retHue, saturation: retSat)
    }
}

/**
 * Color Conversions
 **/
private colorFromHex(String colorStr) {
	/**
     * Gets color data from hex values used by Smartthings' color wheel. 
     */
    log.trace "colorFromHex( ${colorStr} ):"
    
    def colorData = [colormode: "HEX"]
    
// GET HUE & SATURATION DATA   
    def r = Integer.valueOf( colorStr.substring( 1, 3 ), 16 )
    def g = Integer.valueOf( colorStr.substring( 3, 5 ), 16 )
    def b = Integer.valueOf( colorStr.substring( 5, 7 ), 16 )

	def HS = [:]
    HS = getHSfromRGB(r, g, b) 	
	
    def h = HS.hue * 100
    def s = HS.saturation * 100
    
	sendEvent(name: "hue", value: h, isStateChange: true)
	sendEvent(name: "saturation", value: s, isStateChange: true)

// GET XY DATA   
    float red, green, blue;

    // Gamma Corrections
    red = pivotRGB( r / 255 )
    green = pivotRGB( g / 255 )
    blue = pivotRGB( b / 255 )

	// Apply wide gamut conversion D65
    float X = (float) (red * 0.664511 + green * 0.154324 + blue * 0.162028);
    float Y = (float) (red * 0.283881 + green * 0.668433 + blue * 0.047685);
    float Z = (float) (red * 0.000088 + green * 0.072310 + blue * 0.986039);

	// Calculate the xy values
    float x = (X != 0 ? X / (X + Y + Z) : 0);
    float y = (Y != 0 ? Y / (X + Y + Z) : 0);

	double[] xy = new double[2];
    xy[0] = x;
    xy[1] = y;
	//colorData = [xy: xy]

	log.debug "xy from hex = ${xy} ."
    return xy;
}


private colorFromHSB (h, s, level) {
	/**
     * Gets color data from Hue, Sat, and Brightness(level) slider values .
     */
	log.trace "colorFromHSB ( ${h}, ${s}, ${level}):  really h ${h/100*360}, s ${s/100}, v ${level/100}"
    
 //   def colorData = [colormode: "HS"]
    
// GET RGB DATA       
	
    // Ranges are 0-360 for Hue, 0-1 for Sat and Bri
    def hue = (h * 360 / 100).toInteger()
    def sat = (s / 100).toInteger()
    def bri = (level / 100)	//.toInteger()
//	log.debug "hue = ${hue} / sat = ${sat} / bri = ${bri}"
    float i = Math.floor(hue / 60) % 6;
    float f = hue / 60 - Math.floor(hue / 60);
//	log.debug "i = ${i} / f = ${f} "
    
    bri = bri * 255
    float v = bri //as Integer //.toInteger()
    float p = (bri * (1 - sat)) //as Integer	// .toInteger();
    float q = (bri * (1 - f * sat)) //as Integer //.toInteger();
	float t = (bri * (1 - (1 - f) * sat)) //as Integer //.toInteger();

//	log.debug "v = ${v} / p = ${p} / q = ${q} / t = ${t} "
    
	def r, g, b
    
    switch(i) {
    	case 0: 
        	r = v
            g = t
            b = p
            break;
        case 1: 
        	r = q
            g = v
            b = p
            break;
        case 2: 
        	r = p
            g = v
            b = t
            break;
        case 3: 
        	r = p
            g = q
            b = v
            break;
        case 4: 
        	r = t
            g = p
            b = v
            break;
        case 5: 
        	r = v
            g = p
            b = q
            break;
	}
    
    r = r * 255
    g = g * 255
    b = b * 255
    
//	colorData = [R: r, G: g, B: b]

// GET XY DATA	
    float red, green, blue;
	
    // Gamma Corrections
    red = pivotRGB( r / 255 )
    log.debug "red = ${red} / r = ${r}"
    green = pivotRGB( g / 255 )
    blue = pivotRGB( b / 255 )

	// Apply wide gamut conversion D65
    float X = (float) (red * 0.664511 + green * 0.154324 + blue * 0.162028);
    float Y = (float) (red * 0.283881 + green * 0.668433 + blue * 0.047685);
    float Z = (float) (red * 0.000088 + green * 0.072310 + blue * 0.986039);

	// Calculate the xy values
    float x = (X != 0 ? X / (X + Y + Z) : 0);
    float y = (Y != 0 ? Y / (X + Y + Z) : 0);

	double[] xy = new double[2];
    xy[0] = x as Double;
    xy[1] = y as Double;
//	colorData = [xy: xy]

	log.debug "xy from HSB = ${xy[0]} , ${xy[1]} ."
    return xy;
    
}

private colorFromXY(xValue, yValue){
	/**
     * Converts color data from xy values
     */
    
    def colorData = [:]
    
    // Get Brightness & XYZ values
	def bri = device.currentValue("level") as Integer ?: 100
    
    float x = xValue
	float y = yValue
    float z = (float) 1.0 - x - y;
    float Y = bri; 
	float X = (Y / y) * x;
	float Z = (Y / y) * z;

// FIND RGB VALUES

    // Convert to r, g, b using Wide gamut D65 conversion
    float r =  X * 1.656492f - Y * 0.354851f - Z * 0.255038f;
	float g = -X * 0.707196f + Y * 1.655397f + Z * 0.036152f;
	float b =  X * 0.051713f - Y * 0.121364f + Z * 1.011530f;
                
	float R, G, B;
    // Apply Reverse Gamma Corrections
    def red = revPivotRGB( r * 255 )
    def green = revPivotRGB( g * 255 )	
    def blue = revPivotRGB( b * 255 )

	colorData = [red: red, green: green, blue: blue]

	log.debug "RGB colorData = ${colorData}"	
/**
	def maxValue = Math.max(r, Math.max(g,b) );
    r /= maxValue;
    g /= maxValue;
    b /= maxValue;
    r = r * 255; if (r < 0) { r = 255 };
    g = g * 255; if (g < 0) { g = 255 };
    b = b * 255; if (b < 0) { b = 255 };
**/	

// GET HUE & SAT VALUES	
    def HS = [:]
    HS = getHSfromRGB(r, g, b) 
    
    colorData = [hue: HS.hue, saturation: HS.saturation]

	log.debug "HS from XY = ${colorData} "    
    return colorData
}

private getHSfromRGB(r, g, b) {
	log.trace "getHSfromRGB ( ${r}, ${g}, ${b}):  " 
    
    r = r / 255
    g = g / 255 
    b = b / 255
    
    def max = Math.max( r, Math.max(g, b) )
    def min = Math.min( r, Math.min(g, b) )
    
    def h, s, v = max;

    def d = max - min;
    s = max == 0 ? 0 : d / max;

    if ( max == min ){
        h = 0; // achromatic
    } else {
        switch (max) {
            case r: 
            	h = (g - b) / d + (g < b ? 6 : 0)
                break;
            case g: 
            	h = (b - r) / d + 2
                break;
            case b: 
            	h = (r - g) / d + 4
                break;
        }
        
        h /= 6;
    }
	
    def colorData = [:]
    
    colorData["hue"] = h
    colorData["saturation"] = s
    log.debug "colorData = ${colorData} "
    return colorData
}

private pivotRGB(double n) {
	return (n > 0.04045 ? Math.pow((n + 0.055) / 1.055, 2.4) : n / 12.92) * 100.0;
}

private revPivotRGB(double n) {
	return (n > 0.0031308 ? (float) (1.0f + 0.055f) * Math.pow(n, (1.0f / 2.4f)) - 0.055 : (float) n * 12.92);
}    

/**
* original color conv
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


/**
 * Misc
 **/


def getDeviceType() { return "lights" }
