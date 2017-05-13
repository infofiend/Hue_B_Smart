/**
 *  Hue B Smart Scene
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
 *  version 1.1: added setTo2Groups() - use this function to set scene to two different groups at once 
 */
metadata {
	definition (name: "Hue B Smart Scene", namespace: "info_fiend", author: "Anthony Pastor") {
		capability "Actuator"
        capability "Switch"
        capability "Momentary"
        capability "Sensor"
        capability "Configuration"
        
		command "setToGroup"
        command "setTo2Groups"        
        command "updateScene"
        command	"updateSceneFromDevice"
        command "updateStatus"
        command "applySchedule"
        command "quickFix"
        command "noFix"   
        command "refresh"   
        
        attribute "getSceneID", "STRING"        
        attribute "lights", "STRING"  
        attribute "sceneID", "string"
        attribute "scheduleId", "NUMBER"
        attribute "host", "string"
        attribute "username", "string"
        attribute "group", "NUMBER"
        attribute "lightStates", "json_object"  
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2) {
	    multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on",  label:'Push', action:"momentary.push", icon:"st.lights.philips.hue-multi", backgroundColor:"#79b821"
			}
		
//        	tileAttribute ("lights", key: "SECONDARY_CONTROL") {
//                attributeState "lights", label:'The scene controls Hue lights ${currentValue}.'
//            }
		}
    
	    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 3, height: 2) {
			state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
		}

    	standardTile("sceneID", "device.sceneID", inactiveLabel: false, decoration: "flat", width: 6, height: 2) { //, defaultState: "State1"
	       	state "sceneID", label: 'SceneID: ${currentValue} ' //, action:"getSceneID" //, backgroundColor:"#BDE5F2" //, nextState: "State2"
    	}

		standardTile("updateScene", "device.updateScene", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
    	   	state "Ready", label: 'Update<br>Scene', action:"updateSceneFromDevice", backgroundColor:"#FBB215"
	    }
	
 		valueTile("lights", "device.lights", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
			state "default", label: 'Lights: ${currentValue}'
        }
        
        valueTile("lightStates", "device.lightStates", inactiveLabel: false, decoration: "flat", width: 6, height: 2) {
			state "default", label: 'lightStates: ${currentValue}'
        }
        
        valueTile("scheduleId", "device.scheduleId", inactiveLabel: false, decoration: "flat", width: 3, height: 2) {
			state "scheduleId", label: 'Schedule: ${currentValue} ' //, action:"getScheduleID"
        }
        valueTile("schedule", "device.schedule",  width: 4, height: 2) {	//decoration: "flat"
    	   	state "off", label: '.          QFix Off             .', action:"quickFix", backgroundColor:"#BDE5F2" //, nextState: "Enabled"
            state "on", label: ' .         QFix On              .', action:"noFix", backgroundColor:"#FFA500"//, defaultState: "Disabled"
	    }
        
    main "switch"
    details (["switch", "lights", "lightStates", "schedule", "scheduleId", "sceneID", "updateScene", "refresh"]) 	// "scheduleId",
	}
}


private configure() {		
    def commandData = parent.getCommandData(device.deviceNetworkId)
    log.debug "${commandData = commandData}"
    sendEvent(name: "sceneID", value: commandData.deviceId, displayed:true, isStateChange: true)
    sendEvent(name: "scheduleId", value: commandData.scheduleId, displayed:true, isStateChange: true)
    sendEvent(name: "host", value: commandData.ip, displayed:false, isStateChange: true)
    sendEvent(name: "username", value: commandData.username, displayed:false, isStateChange: true)
    sendEvent(name: "lights", value: commandData.lights, displayed:false, isStateChange: true)
    sendEvent(name: "lightStates", value: commandData.lightStates, displayed:false, isStateChange: true)
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	
}

/** 
 * capability.switch
 **/
def on() {
	push()
}

def off() {

}

/**
 * capablity.momentary
 **/
def push() {
	def theGroup = device.currentValue("group") ?: 0
    sendEvent(name: "momentary", value: "pushed", isStateChange: true)    
	setToGroup()
}

def setToGroup ( Integer inGroupID = 0) {
	log.debug("setToGroup ${this.device.label}: Turning scene on for group ${inGroupID}!")

 	def commandData = parent.getCommandData(this.device.deviceNetworkId)
	log.debug "setToGroup: ${commandData}"
    
	def sceneID = commandData.deviceId
//    def groupID = inGroupID ?: 0

	log.debug "${this.device.label}: setToGroup: sceneID = ${sceneID} "
    log.debug "${this.device.label}: setToGroup: theGroup = ${inGroupID} "
    String gPath = "/api/${commandData.username}/groups/${inGroupID}/action"

    parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "${gPath}",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [scene: "${commandData.deviceId}"]
		])
	)

}

def setTo2Groups ( group1, group2 ) {
	log.debug("setTo2Groups ${this.device.label}: Turning scene on for groups ${group1} , ${group2}!")

 	def commandData = parent.getCommandData(this.device.deviceNetworkId)
	log.debug "setTo2Groups: ${commandData}"
    
	def sceneID = commandData.deviceId

	log.debug "${this.device.label}: setTo2Groups: sceneID = ${sceneID} "
    log.debug "${this.device.label}: setTo2Groups: group1 = ${group1} "
    
    String gPath = "/api/${commandData.username}/groups/${group1}/action"

    parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "${gPath}",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [scene: "${commandData.deviceId}"]
		])
	)

    log.debug "${this.device.label}: setTo2Groups: group2 = ${group2} "
    gPath = "/api/${commandData.username}/groups/${group2}/action"

    parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "${gPath}",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [scene: "${commandData.deviceId}"]
		])
	)


}

def turnGroupOn(inGroupID) {
	log.debug "Executing 'turnGroupOn ( ${inGroupID} )'"

    def commandData = parent.getCommandData(device.deviceNetworkId)
    
        return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/${inGroupID}/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true]
		])

	parent.doDeviceSync()
}


def quickFix() {
	log.debug "Turning QuickFix ON (if schedule exists)"
    if (this.device.currentValue("scheduleId")) {
		def schId = this.device.currentValue("scheduleId")
        def sceneId = this.device.currentValue("sceneID")   
    	def devId = this.device.deviceNetworkId
	    log.debug "sceneId = ${sceneId} & schId = ${schId} s/b ${this.device.currentValue("scheduleId")}"    	    
		parent.quickFixON(devId, sceneId, schId)
        
	} else { 
    	log.debug "Scene ${sceneId} doesn't have a Hue Hub schedule"
	    
	}
}

def noFix() {
	log.debug "Turning QuickFix OFF for this scene, if enabled"
    if (this.device.currentValue("scheduleId")) {
	    def schId = this.device.currentValue("scheduleId")
    	def sceneId = this.device.currentValue("sceneID")    
		parent.noFix(this.device.deviceNetworkId, sceneId, schId)
	} else { 
    	log.debug "Scene ${sceneId} doesn't have a Hue Hub schedule"
	}    
}

def updateScene() {
	log.debug "Updating scene!"
    def commandData = parent.getCommandData(this.device.deviceNetworkId)
	log.debug "${commandData}"
    def sceneLights = this.device.currentValue("lights")
    log.debug "sceneLights = ${sceneLights}"
    parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/scenes/${commandData.deviceId}/",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [storelightstate: true]
		])
	)	
}



def updateSceneFromDevice() {
	log.trace "${this}: Update updateSceneFromDevice Reached."

    def sceneIDfromD = device.currentValue("sceneID")

    log.debug "Retrieved sceneIDfromD: ${sceneIDfromD}."

	String myScene = sceneIDfromD

    if (sceneIDfromD) {	// == null) {
//    	def sceneIDfromP = parent.getID(this) - "s"
//    	log.debug "Retrieved sceneIDfromP: ${sceneIDfromP}."
 //       myScene = sceneIDfromP
//    }

    	updateScene()
//		log.debug "Executing 'updateScene' for ${device.label} using sceneID ${myScene}."
	}
}

def updateStatus(type, param, val) {

	//log.debug "updating status: ${type}:${param}:${val}"
	if (type == "scene") {
		if (param == "lights") {

            sendEvent(name: "lights", value: val, displayed:false, isStateChange: true)
        
        } else if (param == "lightStates") {
			log.trace "update lightsStates! = ${lightStates}"
            sendEvent(name: "lightStates", value: val, displayed:true, isStateChange: true)
            
        } else if (param == "scheduleId") {
        
           	"log.debug Should be updating scheduleId with value of ${val}"
           	sendEvent(name: "scheduleId", value: val, displayed:false, isStateChange: true)
                
		} else if (param == "schedule") {

			sendEvent(name: "schedule", value: val, displayed:false, isStateChange: true)
            
		} else {                

			log.debug("Unhandled parameter: ${param}. Value: ${val}")    
        }
    }
}

def refresh() {
	log.trace "refresh(): "
	parent.doDeviceSync()
    configure()
}

def getDeviceType() { return "scenes" }
