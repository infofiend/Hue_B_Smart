/**
 *  Hue B Smart Bridge
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
	definition (name: "Hue B Smart Bridge", namespace: "info_fiend", author: "Anthony Pastor") {
		capability "Actuator"

		attribute "serialNumber", "string"
		attribute "networkAddress", "string"
		attribute "username", "string"
		attribute "host", "string"
        
		command "discoverItems"
        command "discoverBulbs"
        command "discoverGroups"
        command "discoverScenes"
        command "discoverSchedules"
        
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
        standardTile("bridge", "device.username", width: 6, height: 4) {
        	state "default", label:"Hue Bridge", inactivelabel:true, icon:"st.Lighting.light99-hue", backgroundColor: "#F3C200"
        }
		main "bridge"
		details "bridge"
	}
}

def discoverItems(inItems = null) {
	log.debug "Bridge discovering all items on Hue hub."

		def host = this.device.currentValue("networkAddress") + ":80"
		def username = this.device.currentValue("username")
		log.debug "*********** ${username} ********"
		def result 
        
        if (!inItems) {
	        result = new physicalgraph.device.HubAction(
					method: "GET",
					path: "/api/${username}/",
					headers: [
							HOST: host
					]
			)
        }    
            
            
		return result
//	}        
}

def discoverBulbs() {
	log.debug("discoverBulbs: discovering bulbs from Hue hub.")

	    def host = this.device.currentValue("networkAddress") + ":80"
		def username = this.device.currentValue("username")
		def result = new physicalgraph.device.HubAction(
			method: "GET",
			path: "/api/${username}/lights/",
			headers: [
					HOST: host
			]
	)
	return result
}

def discoverGroups() {
	log.debug("discoverGroups: discovering groups from Hue hub.")

	    def host = this.device.currentValue("networkAddress") + ":80"
		def username = this.device.currentValue("username")
		def result = new physicalgraph.device.HubAction(
			method: "GET",
			path: "/api/${username}/groups/",
			headers: [
					HOST: host
			]
	)
	return result
}

def discoverScenes() {
	log.debug("discoverScenes: discovering scenes from Hue hub.")

	    def host = this.device.currentValue("networkAddress") + ":80"
		def username = this.device.currentValue("username")
		def result = new physicalgraph.device.HubAction(
			method: "GET",
			path: "/api/${username}/scenes/",
			headers: [
					HOST: host
			]
	)
	return result
}

def discoverSchedules() {
	log.debug("discoverSchedules: discovering schedules from Hue hub.")

	    def host = this.device.currentValue("networkAddress") + ":80"
		def username = this.device.currentValue("username")
		def result = new physicalgraph.device.HubAction(
			method: "GET",
			path: "/api/${username}/schedules/",
			headers: [
					HOST: host
			]
	)
	return result
}


def handleParse(desc) {
log.debug("handle")
	parse(desc)
}

// parse events into attributes
def parse(String description) {
	def parsedEvent = parseLanMessage(description)
	if (parsedEvent.headers && parsedEvent.body) {
		def headerString = parsedEvent.headers.toString()
		if (headerString.contains("application/json")) {
			def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)
			def bridge = parent.getBridge(parsedEvent.mac)
            def group 

	/* responses from bulb/group/scene/schedule command. Figure out which device it is, then pass it along to the device. */
			if (body[0] != null && body[0].success != null) {
            	log.trace "${body[0].success}"
				body.each{
					it.success.each { k, v ->
						def spl = k.split("/")
						log.debug "k = ${k}, split1 = ${spl[1]}, split2 = ${spl[2]}, split3 = ${spl[3]}, split4= ${spl[4]}, value = ${v}"                            
						def devId = ""
                        def d
                        def groupScene
						
                      
						if (spl[4] == "schedules" || it.toString().contains("command")) {		
                    			devId = bridge.value.mac + "/SCHEDULE" + k
                    	        log.debug "SCHEDULES: k = ${k}, split3 = ${spl[1]}, split4= ${spl[2]}, value = ${v}"
	                            sch = parent.getChildDevice(devId)
    	                        schId = spl[2]
								def username = this.device.currentValue("username")
            	                def host = this.device.currentValue("networkAddress") + ":80"
                            
								log.debug "schedule ${schId} successfully enabled/disabled."

                    	        parent.doDeviceSync("schedules")

						} else if (spl[4] == "scene" || it.toString().contains( "lastupdated") ) {	//if ( it.toString().contains( "lastupdated") ) {				// (spl[4] == "scene") {

                    			devId = bridge.value.mac + "/SCENE" + v
	                            d = parent.getChildDevice(devId)
    	                        groupScene = spl[2]
								def username = this.device.currentValue("username")
            	                def host = this.device.currentValue("networkAddress") + ":80"
                            
								log.debug "Scene ${d} successfully run on group ${groupScene}."
						//		log.debug "calling ${d}" + ".updateStatus(" + "${spl[3]} , ${spl[4]} , ${v}" + ")"
                        
                    	        parent.doDeviceSync("scenes")
						} else { 
	                        if (spl[1] == "groups" && spl[2] != 0 ) {    //if ( it.toString().contains( "action" ) ) {
            	        			devId = bridge.value.mac + "/" + spl[1].toUpperCase()[0..-2] + spl[2]
        	    	                log.debug "GROUP: devId = ${devId}"                            
	
									d = parent.getChildDevice(devId)
							//		log.debug "calling ${d}" + ".updateStatus(" + "${spl[3]} , ${spl[4]} , ${v}" + ")"
									d.updateStatus(spl[3], spl[4], v) 
							} else {
									if (spl[1] == "lights") {
										spl[1] = "BULBS"
								
										devId = bridge.value.mac + "/" + spl[1].toUpperCase()[0..-2] + spl[2]
										d = parent.getChildDevice(devId)
	                    	    //	    log.debug "calling ${d}" + ".updateStatus(" + "${spl[3]} , ${spl[4]} , ${v}" + ")"
										d.updateStatus(spl[3], spl[4], v)
									}                                    
            	            }    
						}
					}
				}	
			} else if (body[0] != null && body[0].error != null) {
				log.debug("Error: ${body}")
			} else if (bridge) {
            	
				def bulbs = [:] //bridge.value.bulbs
				def groups = [:] //bridge.value.groups
				def scenes = [:] //bridge.value.scenes
                def schedules = [:] //bridge.value.schedules

					body.lights?.each { k, v ->
						bulbs[k] = [id: k, name: v.name, type: v.type, state: v.state]
				    }
	                body.groups?.each { k, v -> 
                    	def devIdsGLights = []
/**
						v.lights.each { b->
                        	def devIdGLight = bridge.value.mac + "/BULBS" + ${b}
                            devIdsGLights << devIdGLight
                        }    
**/                        
    	                groups[k] = [id: k, name: v.name, type: v.type, action: v.action, all_on: v.state.all_on, any_on: v.state.any_on, lights: v.lights, groupLightDevIds: devIdsGLights]
					}
	                body.scenes?.each { k, v -> 
//                    	log.trace "k=${k} and v=${v}"
                        				
                    	scenes[k] = [id: k, name: v.name, type: "scene", lights: v.lights]
                            
					}
                    
                    body.schedules?.each { k, v -> 
                    	log.trace "schedules k=${k} and v=${v}"
                    	def schCommand = v.command.address
                        log.debug "schCommand = ${schCommand}"
                        def splCmd = schCommand.split("/")
//                        log.debug "splCmd[1] = ${splCmd[1]} / splCmd[2] = ${splCmd[2]} / splCmd[3] = ${splCmd[3]} / splCmd[4] = ${splCmd[4]}"                        
                        def schGroupId = splCmd[4] 
						log.debug "schGroupId = ${schGroupId}"
//                    	def schSceneId = bridge.value.mac + "/SCENES" + ${v.command.body.scene}
    	                schedules[k] = [id: k, name: v.name, type: "schedule", sceneId: v.command.body.scene, groupId: schGroupId, status: v.status]
					}

                return createEvent(name: "itemDiscovery", value: device.hub.id, isStateChange: true, data: [bulbs, scenes, groups, schedules, bridge.value.mac])

				if (bulbs) {                
                	return createEvent(name: "bulbDiscovery", value: device.hub.id, isStateChange: true, data: [bulbs, null, null, null, bridge.value.mac])
				} else if (groups) {                
                	return createEvent(name: "groupDiscovery", value: device.hub.id, isStateChange: true, data: [null, null, groups, null, bridge.value.mac])
				} else if (scenes) {                
                	return createEvent(name: "sceneDiscovery", value: device.hub.id, isStateChange: true, data: [null, scenes, null, null, bridge.value.mac])
				} else if (schedules) {                
                	return createEvent(name: "scheduleDiscovery", value: device.hub.id, isStateChange: true, data: [null, null, null, schedules, bridge.value.mac])
                    
             	}       
                
			}
		} else {
			log.debug("Unrecognized messsage: ${parsedEvent.body}")
		}
	}
    state.limitation = "none"
	return []
}