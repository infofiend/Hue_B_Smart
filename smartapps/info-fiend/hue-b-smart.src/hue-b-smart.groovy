/**
 *  Hue B Smart
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
 *	Version 1.1 Thanks to Detmer for changes and testing
 *	Version 1.2 Fixed Update problem due to bulb,scene or group deleted from hue without removing it from smartthings first. Thanks to Collisionc
 *	Version 1.2 Added FlashCoRe for webcore usage
 *      Version 1.3 Added White Ambience Group
 *	Version 1.4 Added Version Number into log to make sure people are running the latest version when they moan it doesnt work
 *	Version 1.5 Fixed install problem for some users
 */
definition(
        name: "Hue B Smart",
        namespace: "info_fiend",
        author: "anthony pastor",
        description: "The Smartest Hue Control App for SmartThings - total control of bulbs, scenes, groups, and schedules",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png",
        singleInstance: true
)

preferences {
    	page(name:"Bridges", content: "bridges")
    	page(name:"linkButton", content: "linkButton")
   	page(name:"linkBridge", content: "linkBridge")
   	page(name:"manageBridge", content: "manageBridge")
	page(name:"chooseBulbs", content: "chooseBulbs")
 	page(name:"chooseScenes", content: "chooseScenes")
 	page(name:"chooseGroups", content: "chooseGroups")
    	page(name:"deleteBridge", content: "deleteBridge")
        page(name:"unlinkBridge", content: "unlinkBridge")
}

def manageBridge(params) {

	state.newSchedule = [:]

    if (params.mac) {
        state.params = params;
    } else {
        params = state.params;
    }

    def bridge = getBridge(params.mac)
	log.debug("Manage Bridge ${bridge}")
    def ip = convertHexToIP(bridge.value.networkAddress)
	log.debug("Manage Bridge ${ip}")
    def mac = params.mac
	log.debug("Manage Bridge Params ${mac}")
    def bridgeDevice = getChildDevice(mac)
	log.debug("Manage Bridge GET ${bridgeDevice}")
    def title = "${bridgeDevice} ${ip}"
    def refreshInterval = 2

    if (!bridgeDevice) {
        log.debug("Bridge device not found?")
        /* Error, bridge device doesn't exist? */
        return
    }
    
	if (params.refreshItems) {
    	params.refreshItems = false
		bridge.value.itemsDiscovered = false
    	state.itemDiscoveryComplete = false        
    }
    
    int itemRefreshCount = !state.itemRefreshCount ? 0 : state.itemRefreshCount as int
    if (!state.itemDiscoveryComplete) {
        state.itemRefreshCount = itemRefreshCount + 1
    }

    // resend request if we haven't received a response in 10 seconds 
    if (!bridge.value.itemsDiscovered && ((!state.inItemDiscovery && !state.itemDiscoveryComplete) || (state.itemRefreshCount == 6))) {
		unschedule() 
        state.itemDiscoveryComplete = false
        state.inItemDiscovery = mac
        bridgeDevice.discoverItems()
        state.itemRefreshCount = 0
        return dynamicPage(name:"manageBridge", title: "Manage bridge ${ip}", refreshInterval: refreshInterval, install: false) {
        	section("Discovering bulbs, scenes, and groups...") {
				href(name: "Delete Bridge", page:"deleteBridge", title:"", description:"Delete bridge ${ip} (and devices)", params: [mac: mac])
			}
		}
    } else if (state.inItemDiscovery) {
        return dynamicPage(name:"manageBridge", title: "Manage bridge ${ip}", refreshInterval: refreshInterval, install: false) {
            section("Discovering bulbs, scenes, and groups...") {
				href(name: "Delete Bridge", page:"deleteBridge", title:"", description:"Delete bridge ${ip} (and devices)", params: [mac: mac])
            }
        }
    }
	/* discovery complete, re-enable device sync */
	runEvery5Minutes(doDeviceSync)
    
    def numBulbs = bridge.value.bulbs.size() ?: 0
    def numScenes = bridge.value.scenes.size() ?: 0
    def numGroups = bridge.value.groups.size() ?: 0
    def numSchedules = bridge.value.schedules?.size() ?: 0

    dynamicPage(name:"manageBridge", install: true) {
        section("Manage Bridge ${ip}") {
			href(name:"Refresh items", page:"manageBridge", title:"Refresh discovered items", description: "", params: [mac: mac, refreshItems: true])
            paragraph ""
			href(name:"Choose Bulbs", page:"chooseBulbs", description:"", title: "Choose Bulbs (${numBulbs} found)", params: [mac: mac])
            href(name:"Choose Scenes", page:"chooseScenes", description:"", title: "Choose Scenes (${numScenes} found)", params: [mac: mac])
			href(name:"Choose Groups", page:"chooseGroups", description:"", title: "Choose Groups (${numGroups} found)", params: [mac: mac])
            paragraph ""
            href(name: "Delete Bridge", page:"deleteBridge", title:"Delete bridge ${ip}", description: "", params: [mac: mac])
            href(name:"Back", page:"Bridges", title:"Back to main page", description: "")
		}
    }
}

def linkBridge() {
    state.params.done = true
    log.debug "linkBridge"
    dynamicPage(name:"linkBridge") {
        section() {
            getLinkedBridges() << state.params.mac
            paragraph "Linked! Please tap Done."
        }
    }
}

def linkButton(params) {
    /* if the user hit the back button, use saved parameters as the passed ones no longer good
     * also uses state.params to pass these on to the next page
     */

    if (params.mac) {
        state.params = params;
    } else {
        params = state.params;
    }

    int linkRefreshcount = !state.linkRefreshcount ? 0 : state.linkRefreshcount as int
    state.linkRefreshcount = linkRefreshcount + 1
    def refreshInterval = 3
    
    params.linkingBridge = true
    if (!params.linkDone) {
        if ((linkRefreshcount % 2) == 0) {
        	log.debug "Sending Developer Request"
            	sendDeveloperReq(params.mac)
        }
        log.debug "linkButton ${params}"
        dynamicPage(name: "linkButton", refreshInterval: refreshInterval, nextPage: "linkButton") {
            section("Hue Bridge ${params.ip}") {
                paragraph "Please press the link button on your Hue bridge."
                image "http://www.developers.meethue.com/sites/default/files/smartbridge.jpg"
            }
            section() {
                href(name:"Cancel", page:"Bridges", title: "", description: "Cancel")
            }
        }
    } else {
        /* link success! create bridge device */
        log.debug "Bridge linked!"
        log.debug("ssdp ${params.ssdpUSN}")
        log.debug("username ${params.username}")
        
        def bridge = getUnlinkedBridges().find{it?.key?.contains(params.ssdpUSN)}
        log.debug("line 171B bridge ${bridge}")

	state.user = params.username
        state.host = params.ip + ":80"
        log.debug "state.user = ${state.user} ******************"
	log.debug "state.host = ${state.host} ******************"
        log.debug "bridge.value.serialNumber = ${bridge.value.serialNumber} *****************"

        def d = addChildDevice("info_fiend", "Hue B Smart Bridge", bridge.value.mac, bridge.value.hub, [label: "Hue B Smart Bridge (${params.ip}", username: "${params.username}", networkAddress: "${params.ip}", host: "${state.host}"])
		
        d.sendEvent(name: "networkAddress", value: params.ip)
        d.sendEvent(name: "serialNumber", value: bridge.value.serialNumber)
        d.sendEvent(name: "username", value: params.username)

        subscribe(d, "itemDiscovery", itemDiscoveryHandler) 

        params.linkDone = false
        params.linkingBridge = false

        bridge.value << ["bulbs" : [:], "groups" : [:], "scenes" : [:], "schedules" : [:]]
        getLinkedBridges() << bridge
        log.debug "Bridge added to linked list"
        getUnlinkedBridges().remove(params.ssdpUSN)
        log.debug "Removed bridge from unlinked list"

        dynamicPage(name: "linkButton", nextPage: "Bridges") {
            section("Hue Bridge ${params.ip}") {
                paragraph "Successfully linked Hue Bridge! Please tap Next."
            }
        }
    }
}

def getLinkedBridges() {
    state.linked_bridges = state.linked_bridges ?: [:]
}

def getUnlinkedBridges() {
    state.unlinked_bridges = state.unlinked_bridges ?: [:]
}

def getVerifiedBridges() {
    getUnlinkedBridges().findAll{it?.value?.verified == true}
}

def getBridgeBySerialNumber(serialNumber) {
    def b = getUnlinkedBridges().find{it?.value?.serialNumber == serialNumber}
    if (!b) {
        return getLinkedBridges().find{it?.value?.serialNumber == serialNumber}
    } else {
        return b
    }
}

def getBridge(mac) {
    def b = getUnlinkedBridges().find{it?.value?.mac == mac}
    if (!b) {
        return getLinkedBridges().find{it?.value?.mac == mac}
    } else {
        return b
    }
}

def bridges() {
    /* Prevent "Unexpected Error has occurred" if the user hits the back button before actually finishing an install.
     * Weird SmartThings bug
     */
    if (!state.installed) {
        return dynamicPage(name:"Bridges", title: "Initial installation", install:true, uninstall:true) {
            section() {
                paragraph "For initial installation, please tap Done, then proceed to Menu -> SmartApps -> Hue B Smart."
            }
        }
    }

    /* clear temporary stuff from other pages */
    state.params = [:]
    state.inItemDiscovery = null
    state.itemDiscoveryComplete = false
    state.numDiscoveryResponses = 0
    state.creatingDevices = false

    int bridgeRefreshCount = !state.bridgeRefreshCount ? 0 : state.bridgeRefreshCount as int
    state.bridgeRefreshCount = bridgeRefreshCount + 1
    def refreshInterval = 3

    if (!state.subscribed) {
        subscribe(location, null, locationHandler, [filterEvents:false])
        state.subscribed = true
    }

    // Send bridge discovery request every 15 seconds
    if ((state.bridgeRefreshCount % 5) == 1) {
        discoverHueBridges()
        log.debug "Bridge discovery sent - TMLEAFS 1.5"
    } else {
        // if we're not sending bridge discovery, verify bridges instead
        verifyHueBridges()
    }

    dynamicPage(name:"Bridges", refreshInterval: refreshInterval, install: true, uninstall: true) {
        section("Linked Bridges") {
            getLinkedBridges().sort { it.value.name }.each {
                def ip = convertHexToIP(it.value.networkAddress)
		    log.debug("Bridges Linked IP ${ip}")
                def mac = "${it.value.mac}"
		    log.debug("Bridges Linked MAC ${mac}")
                state.mac = mac
                def title = "Hue Bridge ${ip}"
                href(name:"manageBridge ${mac}", page:"manageBridge", title: title, description: "", params: [mac: mac])
            }
        }
        section("Unlinked Bridges") {
            paragraph "Searching for Hue bridges. They will appear here when found. Please wait."
            getVerifiedBridges().sort { it.value.name }.each {
                def ip = convertHexToIP(it.value.networkAddress)
		    log.debug("Bridges UnLinked IP ${ip}")
                def mac = "${it.value.mac}"
		    log.debug("Bridges UnLinked MAC ${mac}")
                def title = "Hue Bridge ${ip}"
                href(name:"linkBridge ${mac}", page:"linkButton", title: title, description: "", params: [mac: mac, ip: ip, ssdpUSN: it.value.ssdpUSN])
            }
        }
    }
}

def deleteBridge(params) {

    if (params.mac) {
        state.params = params;
    } else {
        params = state.params;
    }
	
	def bridge = getBridge(params.mac)
    def d = getChildDevice(params.mac)
    log.debug "Deleting bridge ${d.currentValue('networkAddress')} (${params.mac})"
    
	def success = true
	def devices = getChildDevices()
    def text = ""
	devices.each {
    	def devId = it.deviceNetworkId
        if (devId.contains(params.mac) && devId != params.mac) {
        	log.debug "Removing ${devId}"
			try {
    	    	deleteChildDevice(devId)
			} catch (physicalgraph.exception.NotFoundException e) {
	        	log.debug("${devId} already deleted?")
			} catch (physicalgraph.exception.ConflictException e) {
	        	log.debug("${devId} still in use")
				text = text + "${it.label} is still in use. Remove from any SmartApps or Dashboards, then try again.\n"
		        success = false
			}
        }
	}
    if (success) {
		try {
        	unsubscribe(d)
    		deleteChildDevice(params.mac)
		} catch (physicalgraph.exception.NotFoundException e) {
	    	log.debug("${params.mac} already deleted?")
		} catch (physicalgraph.exception.ConflictException e) {
	    	log.debug("${params.mac} still in use")
			text = text + "${params.mac} is still in use. Remove from any SmartApps or Dashboards, then try again.\n"
			success = false
		}
	}
    if (success) {
        getLinkedBridges().remove(bridge.key)
        return dynamicPage(name:"deleteBridge", title: "Delete Bridge", install:false, uninstall:false, nexdtPage: "Bridges") {
            section() {
                paragraph "Bridge ${d.currentValue('networkAddress')} and devices successfully deleted."
            	href(name:"Back", page:"Bridges", title:"", description: "Back to main page")
            }
        }    
    } else {
        return dynamicPage(name:"deleteBridge", title: "Delete Bridge", install:false, uninstall:false, nextPage: "Bridges") {
            section() {
                paragraph "Bridge deletion (${d.currentValue('networkAddress')}) failed.\n${text}"
				href(name:"Back", page:"Bridges", title:"", description: "Back to main page")                
            }
        }    
    }
}

def chooseBulbs(params) {

    if (params.mac) {
        state.params = params;
    } else {
        params = state.params;
    }

	def bridge = getBridge(params.mac)
	def addedBulbs = [:]
    def availableBulbs = [:]
    def user = state.username
    
    bridge.value.bulbs.each {
		def devId = "${params.mac}/BULB${it.key}"
		def name = it.value.name
        
		def d = getChildDevice(devId) 
        if (d) {
        	addedBulbs << it
        } else {
        	availableBulbs << it
        }
    }

	if (params.add) {
	    log.debug("Adding ${params.add} 382")
        def bulbId = params.add
		params.add = null
        def b = bridge.value.bulbs[bulbId]
		def devId = "${params.mac}/BULB${bulbId}"
        if (b.type.equalsIgnoreCase("Dimmable light")) {
			try {
	            def d = addChildDevice("info_fiend", "Hue B Smart Lux Bulb", devId, bridge.value.hub, ["label": b.label])	
				["bri", "reachable", "on"].each { p -> 
					d.updateStatus("state", p, b.state[p])
				}
                d.updateStatus("state", "transitiontime", 2)
                //d.updateStatus("state", "colormode", "HS")                
                d.configure()
                addedBulbs[bulbId] = b
                availableBulbs.remove(bulbId)
			} catch (grails.validation.ValidationException e) {
            	log.debug "${devId} already created"
			}    
	    }
		else if (b.type.equalsIgnoreCase("Color Temperature Light")) {
			 try {
                    def d = addChildDevice("info_fiend", "Hue B Smart White Ambiance", devId, bridge.value.hub, ["label": b.label])
				["ct", "bri", "reachable", "on"].each { p ->
                        		d.updateStatus("state", p, b.state[p])
                		}
                d.updateStatus("state", "transitiontime", 2)
				d.configure()
                addedBulbs[bulbId] = b
                availableBulbs.remove(bulbId)
           		} catch (grails.validation.ValidationException e) {
                log.debug "${devId} already created"
            		}
		}
		else {
			try {
            	def d = addChildDevice("info_fiend", "Hue B Smart Bulb", devId, bridge.value.hub, ["label": b.label])
                ["bri", "sat", "reachable", "hue", "on", "xy", "ct", "effect"].each { p ->
                	d.updateStatus("state", p, b.state[p])
                    
				}
                d.updateStatus("state", "colormode", "HS")
                d.updateStatus("state", "transitiontime", 2)
                d.configure()
                addedBulbs[bulbId] = b
                availableBulbs.remove(bulbId)
			} catch (grails.validation.ValidationException e) {
	            log.debug "${devId} already created"
			}
		}
	}
    
    if (params.remove) {
    	log.debug "Removing ${params.remove}"
		def devId = params.remove
        params.remove = null
		def bulbId = devId.split("BULB")[1]
		try {
        	deleteChildDevice(devId)
            addedBulbs.remove(bulbId)
            availableBulbs[bulbId] = bridge.value.bulbs[bulbId]
		} catch (physicalgraph.exception.NotFoundException e) {
        	log.debug("${devId} already deleted")
            addedBulbs.remove(bulbId)
            availableBulbs[bulbId] = bridge.value.bulbs[bulbId]
		} catch (physicalgraph.exception.ConflictException e) {
        	log.debug("${devId} still in use")
            errorText = "Bulb ${bridge.value.bulbs[bulbId].label} is still in use. Remove from any SmartApps or Dashboards, then try again."
        }     
    }
    
    dynamicPage(name:"chooseBulbs", title: "", install: true) {
    	section("") {
        	href(name: "manageBridge", page: "manageBridge", title: "Back to Bridge", description: "", params: [mac: params.mac])
	}
    	section("Added Bulbs") {
			addedBulbs.sort{it.value.name}.each { 
				def devId = "${params.mac}/BULB${it.key}"
				def name = it.value.label
				href(name:"${devId}", page:"chooseBulbs", description:"", title:"Remove ${name}", params: [mac: params.mac, remove: devId], submitOnChange: true )
			}
		}
        section("Available Bulbs") {
			availableBulbs.sort{it.value.name}.each { 
				def devId = "${params.mac}/BULB${it.key}"
				def name = it.value.label
				href(name:"${devId}", page:"chooseBulbs", description:"", title:"Add ${name}", params: [mac: params.mac, add: it.key], submitOnChange: true )
			}
        }
    }
}

def chooseScenes(params) {
 
    if (params.mac) {
        state.params = params;
    } else {
        params = state.params;
    }

	def bridge = getBridge(params.mac)
	def addedScenes = [:]
    def availableScenes = [:]
 //   def availableSchedules = [:]
    def user = state.username
    
    bridge.value.scenes.each {
		def devId = "${params.mac}/SCENE${it.key}"
        
		def d = getChildDevice(devId) 
        if (d) {
        	addedScenes << it
        } else {
        	availableScenes << it
        }
    }

    
	if (params.add) {
	    log.debug("Adding ${params.add}")
        def sceneId = params.add
		params.add = null
        def s = bridge.value.scenes[sceneId]
        log.debug "adding scene ${s.label}.  Are lights assigned? lights = ${s.lights}"
		log.debug "Does scene ${s.label} have lightStates?  lightStates = ${s.lightStates}"
        
         
		def devId = "${params.mac}/SCENE${sceneId}"
		try { 
			def d = addChildDevice("info_fiend", "Hue B Smart Scene", devId, bridge.value.hub, ["label": s.label, "type": "scene", "lights": s.lights, "lightStates": s.lightStates])
//            d.updateStatus("scene", "lights", s.lights )
//            d.updateStatus("scene", "lightStates", s.lightStates )
            if (d.scheduleId) {
	            d.updateStatus("scene", "scheduleId", s.scheduleId )
            }    
            d.configure()
			addedScenes[sceneId] = s
			availableScenes.remove(sceneId)
		} catch (grails.validation.ValidationException e) {
            	log.debug "${devId} already created"
	    }
	}
    
    if (params.remove) {
    	log.debug "Removing ${params.remove}"
		def devId = params.remove
        params.remove = null
		def sceneId = devId.split("SCENE")[1]
        try {
        	deleteChildDevice(devId)
            addedScenes.remove(sceneId)
            availableScenes[sceneId] = bridge.value.scenes[sceneId]
            
		} catch (physicalgraph.exception.NotFoundException e) {
        	log.debug("${devId} already deleted. 536")
			addedScenes.remove(sceneId)
            availableScenes[sceneId] = bridge.value.scenes[sceneId]
		} catch (physicalgraph.exception.ConflictException e) {
        	log.debug("${devId} still in use. 540")
            errorText = "Scene ${bridge.value.scenes[sceneId].label} is still in use. Remove from any SmartApps or Dashboards, then try again."
        }
    }
    
    dynamicPage(name:"chooseScenes", title: "", install: true) {
		section("") { 
			href(name: "manageBridge", page: "manageBridge", description: "", title: "Back to Bridge", params: [mac: params.mac])
        }
    	section("Added Scenes") {
			addedScenes.sort{it.value.name}.each { 
				def devId = "${params.mac}/SCENE${it.key}"
				def name = it.value.label
				href(name:"${devId}", page:"chooseScenes", description:"", title:"Remove ${name}", params: [mac: params.mac, remove: devId], submitOnChange: true )
			}
		}
        section("Available Scenes") {
			availableScenes.sort{it.value.name}.each { 
				def devId = "${params.mac}/SCENE${it.key}"
				def name = it.value.label
				href(name:"${devId}", page:"chooseScenes", description:"", title:"Add ${name}", params: [mac: params.mac, add: it.key], submitOnChange: true )
			}
        }
    }
}

def chooseGroups(params) {
	state.groupLights = []
    state.selectedGroup = []
	state.selectedBulbs = []
    state.availableGroups = []
	state.availableBulbs = []

    
    if (params.mac) {
        state.params = params;
    } else {
        params = state.params;
    }

    def errorText = ""

	def bridge = getBridge(params.mac)
	def addedGroups = [:]
    def availableGroups = [:]
    def user = state.user
    log.debug "=================== ${state.user} ================"
    bridge.value.groups.each {
		def devId = "${params.mac}/GROUP${it.key}"
		def name = it.value.name
        
		def d = getChildDevice(devId) 
        if (d) {
        	addedGroups << it
        } else {
        	availableGroups << it
        }
    }

	if (params.add) {
	    log.debug("602A Adding ${params.add}")
        def groupId = params.add
        log.debug "ADDING GROUP: params.mac = ${params.mac} / groupId = ${groupId}"
		params.add = null
        def g = bridge.value.groups[groupId]
        log.debug "ADDING GROUP: g / bridge.value.groups[groupId] = ${g}.  Are lights assigned? lights = ${g.lights} 605"
		def devId = "${params.mac}/GROUP${groupId}"
        
        if (g.action.hue) {
			try { 
				def d = addChildDevice("info_fiend", "Hue B Smart Group", devId, bridge.value.hub, ["label": g.label, "type": g.type, "groupType": "Color Group", "allOn": g.all_on, "anyOn": g.any_on, "lights": g.lights])
	    	    log.debug "adding group ${d}."	//  Are lights assigned? lights = ${g.lights}"     
            	["bri", "sat", "hue", "on", "xy", "ct", "colormode", "effect"].each { p ->
                		d.updateStatus("action", p, g.action[p])                    
				}
    	        d.updateStatus("action", "transitiontime", 2)
//        	    d.updateStatus("action", "lights", "${g.lights}")
			//	d.updateStatus("scene", "lightDevId", "{g.groupLightDevIds}")
	            d.configure()
				addedGroups[groupId] = g
				availableGroups.remove(groupId)
			} catch (grails.validation.ValidationException e) {
    	        	log.debug "${devId} already created"
	    	}
		}
       else if (g.action.ct) {
			try { 
				def d = addChildDevice("info_fiend", "Hue B Smart White Ambiance Group", devId, bridge.value.hub, ["label": g.label, "type": g.type, "groupType": "Ambience Group", "allOn": g.all_on, "anyOn": g.any_on, "lights": g.lights])
	    	    log.debug "adding group ${d}."	//  Are lights assigned? lights = ${g.lights}"     
            	["bri", "sat", "on", "ct", "colormode", "effect"].each { p ->
                		d.updateStatus("action", p, g.action[p])                    
				}
    	        d.updateStatus("action", "transitiontime", 2)
//        	    d.updateStatus("action", "lights", "${g.lights}")
			//	d.updateStatus("scene", "lightDevId", "{g.groupLightDevIds}")
	            d.configure()
				addedGroups[groupId] = g
				availableGroups.remove(groupId)
			} catch (grails.validation.ValidationException e) {
    	        	log.debug "${devId} already created"
	    	}
		}
        else {
			try { 
				def d = addChildDevice("info_fiend", "Hue B Smart Lux Group", devId, bridge.value.hub, ["label": g.label, "type": g.type, "groupType": "Lux Group", "allOn": g.all_on, "anyOn": g.any_on, "lights": g.lights])
	    	    log.debug "638A adding group ${d}."  // Are lights assigned? lights = ${g.lights}"     
            	["bri", "on", "effect"].each { p ->
                		d.updateStatus("action", p, g.action[p])                    
				}
    	        d.updateStatus("action", "transitiontime", 2)
  //      	    d.updateStatus("action", "lights", "${g.lights}")
	//			d.updateStatus("scene", "lightDevId", "{g.groupLightDevIds}")
	            d.configure()
				addedGroups[groupId] = g
				availableGroups.remove(groupId)
			} catch (grails.validation.ValidationException e) {
    	        	log.debug "${devId} already created"
	    	}
		}        
    }
    
    if (params.remove) {
    	log.debug "Removing ${params.remove}"
		def devId = params.remove
        params.remove = null
		def groupId = devId.split("GROUP")[1]
		try {
        	deleteChildDevice(devId)
            addedGroups.remove(groupId)
            availableGroups[groupId] = bridge.value.groups[groupId]
		} catch (physicalgraph.exception.NotFoundException e) {
        	log.debug("${devId} already deleted")
            addedGroups.remove(groupId)
            availableGroups[groupId] = bridge.value.groups[groupId]
		} catch (physicalgraph.exception.ConflictException e) {
        	log.debug("${devId} still in use")
            errorText = "Group ${bridge.value.groups[groupId].label} is still in use. Remove from any SmartApps or Dashboards, then try again."
        }
    }

    return dynamicPage(name:"chooseGroups", title: "", install:false, uninstall:false, nextPage: "manageBridge") {
	    section("") { 
        		href(name: "manageBridge", page: "manageBridge", description: "", title: "Back to Bridge", params: [mac: params.mac], submitOnChange: true )
		}
	    section("Hue Groups Added to SmartThings") {
			addedGroups.sort{it.value.name}.each { 
				def devId = "${params.mac}/GROUP${it.key}"
				def name = it.value.label
				href(name:"${devId}", page:"chooseGroups", description:"", title:"Remove ${name}", params: [mac: params.mac, remove: devId], submitOnChange: true )
			}
		}
        section("Available Hue Groups") {
			availableGroups.sort{it.value.name}.each { 
				def devId = "${params.mac}/GROUP${it.key}"
				def name = it.value.label
				href(name:"${devId}", page:"chooseGroups", description:"", title:"Add ${name}", params: [mac: params.mac, add: it.key])
			}
        }
        
	}
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def uninstalled() {
    log.debug "Uninstalling"
    state.installed = false
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    initialize()
}

def initialize() {
    log.debug "Initialize"
    unsubscribe()
    unschedule()
    state.schedules = []
    state.subscribed = false
    state.unlinked_bridges = [:]
    state.bridgeRefreshCount = 0
    state.installed = true
	state.limitation = "None"
    state.scheduleEnabled = [:]
    
	doDeviceSync()
	runEvery5Minutes(doDeviceSync)

	state.linked_bridges.each {
        def d = getChildDevice(it.value.mac)
        subscribe(d, "itemDiscovery", itemDiscoveryHandler)
    }
    subscribe(location, null, locationHandler, [filterEvents:false])
}

def itemDiscoveryHandler(evt) {

	log.trace "evt = ${evt}"
    def bulbs = evt.jsonData[0]
 //   log.debug "bulbs from evt.jsonData[0] = ${bulbs}"
    def scenes = evt.jsonData[1]
//	log.debug "scenes from evt.jsonData[1] = ${scenes}"
    def groups = evt.jsonData[2]
//	log.debug "groups from evt.jsonData[2] = ${groups}"
    def schedules = evt.jsonData[3]
//	log.debug "schedules from evt.jsonData[3] = ${schedules}"
    def mac = evt.jsonData[4]
//	log.debug "mac from evt.jsonData[4] = ${mac}"


	def bridge = getBridge(mac)
    state.bridge = bridge
    def host = bridge.value.networkAddress
    host = "${convertHexToIP(host)}" + ":80"
    state.host = host
    def username = state.user

    
	bridge.value.bulbs = bulbs
    bridge.value.groups = groups
    log.debug "Groups = ${groups}"
   	bridge.value.scenes = scenes
    bridge.value.schedules = schedules
    
	if (state.inItemDiscovery) {
	    state.inItemDiscovery = false
        state.itemDiscoveryComplete = true
        bridge.value.itemsDiscovered = true
	}
    
    /* update existing devices */
	def devices = getChildDevices()
    log.trace "devices = ${devices}"
	devices.each {
    	log.trace "device = ${it}"
    	def devId = it.deviceNetworkId
        
	    if (devId.contains(mac) && devId.contains("/")) {
    		if (it.deviceNetworkId.contains("BULB")) {
	            log.trace "contains BULB / DNI = ${it.deviceNetworkId}: ${it}"
   	            def bulbId = it.deviceNetworkId.split("/")[1] - "BULB"
       	        log.debug "bulbId = ${bulbId}" 
                def bBulb = bridge.value.bulbs[bulbId]
                log.debug "bridge.value.bulbs[bulbId] = ${bBulb}."
                if ( bBulb != null ) {  // If user removes bulb from hue without removing it from smartthings,
                                        // getChildDevices() will still return the scene as part of the array as null, so we need to check for it to prevent crashing.
				    def type = bBulb.type 	// bridge.value.bulbs[bulbId].type
               	    if (type.equalsIgnoreCase("Dimmable light")) {
					    ["reachable", "on", "bri"].each { p -> 
   	                	    it.updateStatus("state", p, bridge.value.bulbs[bulbId].state[p])
					    }
           	        } else if (type.equalsIgnoreCase("Color Temperature Light")) {
					    ["bri", "ct", "reachable", "on"].each { p ->
                       	    it.updateStatus("state", p, bridge.value.bulbs[bulbId].state[p])
    				    }
			        } else {
					    ["reachable", "on", "bri", "hue", "sat", "ct", "xy","effect", "colormode"].each { p -> 
                   		    it.updateStatus("state", p, bridge.value.bulbs[bulbId].state[p])
					    }
   	                }
                }
            }
            if (it.deviceNetworkId.contains("GROUP")) {
   	            def groupId = it.deviceNetworkId.split("/")[1] - "GROUP"
           	    def g = bridge.value.groups[groupId]
				def groupFromBridge = bridge.value.groups[groupId]
                if ( groupFromBridge != null ) {        // If user removes group from hue without removing it from smartthings, 
                    def gLights = groupFromBridge.lights// getChildDevices() will still return the scene as part of the array as null, so we need to check for it to prevent crashing.
                    def test
                    def colormode = bridge.value.groups[groupId]?.action?.colormode
                    if (colormode != null) {
    					["on", "bri", "sat", "ct", "xy", "effect", "hue", "colormode"].each { p -> 
           	            	test = bridge.value.groups[groupId].action[p]
                       	    it.updateStatus("action", p, bridge.value.groups[groupId].action[p])
        	    		}
			    	}else{
				    	 ["bri", "on"].each { p ->
                            it.updateStatus("action", p, bridge.value.groups[groupId].action[p])
                         }
				    }
                }
            }

            if (it.deviceNetworkId.contains("SCENE")) {
            	log.trace "it.deviceNetworkId contains SCENE = ${it.deviceNetworkId}"
				log.trace "contains SCENE / DNI = ${it.deviceNetworkId}"
    	        def sceneId = it.deviceNetworkId.split("/")[1] - "SCENE"
        	    log.debug "sceneId = ${sceneId}"
                def sceneFromBridge = bridge.value.scenes[sceneId]
                log.trace "sceneFromBridge = ${sceneFromBridge}"
                if ( sceneFromBridge != null ) { // If user removes scene from hue without removing it from smartthings,
                    def sceneLights = []         // getChildDevices() will still return the scene as part of the array as null, so we need to check for it to prevent crashing.
                    sceneLights = sceneFromBridge.lights
                    def scenelightStates = sceneFromBridge.lightStates

	                log.trace "bridge.value.scenes[${sceneId}].lights = ${sceneLights}"
				    log.trace "bridge.value.scenes[${sceneId}].lightStates = ${scenelightStates}"

            	    if (bridge.value.scenes[sceneId].lights) {
					    it.updateStatus("scene", "lights", bridge.value.scenes[sceneId].lights)
                    }
                    if (scenelightStates) {
					    it.updateStatus("scene", "lightStates", scenelightStates)
				//	it.updateStatus("scene", "schedule", "off")
                    }
        	    }
		    }
	    }
    }
}

def locationHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId

    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]

    if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:device:basic:1")) {
        /* SSDP response */
        processDiscoveryResponse(parsedEvent)
    } else if (parsedEvent.headers && parsedEvent.body) {
        /* Hue bridge HTTP reply */
        def headerString = parsedEvent.headers.toString()
        if (headerString.contains("xml")) {
            /* description.xml reply, verifying bridge */
            processVerifyResponse(parsedEvent.body)
        } else if (headerString?.contains("json")) {
            def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)
            if (body.success != null && body.success[0] != null && body.success[0].username) {
                /* got username from bridge */
                state.params.linkDone = true
                state.params.username = body.success[0].username
            } else if (body.error && body.error[0] && body.error[0].description) {
                log.debug "error: ${body.error[0].description}"
            } else {
                log.debug "unknown response: ${headerString}"
                log.debug "unknown response: ${body}"
            }
        }
    }
}

def Hubinstall(evt){
    def parsedEvent = parseLanMessage(description)
										
    if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:device:basic:1")) {
        /* SSDP response */
		log.debug "SSDP Response"	
        processDiscoveryResponse(parsedEvent)
    } else if (parsedEvent.headers && parsedEvent.body) {
        /* Hue bridge HTTP reply */
        def headerString = parsedEvent.headers.toString()
        if (headerString.contains("xml")) {
			log.debug "HeaderString: XML"	
            /* description.xml reply, verifying bridge */
            processVerifyResponse(parsedEvent.body)
        } else if (headerString?.contains("json")) {
			log.debug "HeaderString: JSON"	
            def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)
            if (body.success != null && body.success[0] != null && body.success[0].username) {
                log.debug "Got Username From Bridge"	
				/* got username from bridge */
                state.params.linkDone = true
                state.params.username = body.success[0].username
            } else if (body.error && body.error[0] && body.error[0].description) {
                log.debug "error: ${body.error[0].description}"
            } else {
                log.debug "unknown response: ${headerString}"
                log.debug "unknown response: ${body}"
            }
        }
    }
}


/**
 * HUE BRIDGE COMMANDS
 **/
private discoverHueBridges() {
    log.debug("Sending bridge discovery.")
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:basic:1", physicalgraph.device.Protocol.LAN))
}

private verifyHueBridges() {
    def devices = getUnlinkedBridges().findAll { it?.value?.verified != true }
    devices.each {
        def ip = convertHexToIP(it.value.networkAddress)
        def port = convertHexToInt(it.value.deviceAddress)
        verifyHueBridge("${it.value.mac}", (ip + ":" + port))
    }
}

private verifyHueBridge(String deviceNetworkId, String host) {
	log.trace "Verify Hue Bridge $deviceNetworkId"
	sendHubCommand(new physicalgraph.device.HubAction([
			method: "GET",
			path: "/description.xml",
			headers: [
					HOST: host
			]], deviceNetworkId, [callback: "processVerifyResponse"]))
}


/**
 * HUE BRIDGE RESPONSES
 **/
private processDiscoveryResponse(parsedEvent) {
	log.debug("Discovery Response is ${parsedEvent}.")
    log.debug("Discovered bridge ${parsedEvent.mac} (${convertHexToIP(parsedEvent.networkAddress)})")

    def bridge = getUnlinkedBridges().find{it?.key?.contains(parsedEvent.ssdpUSN)} 
    if (!bridge) { bridge = getLinkedBridges().find{it?.key?.contains(parsedEvent.ssdpUSN)} }
    if (bridge) {
        /* have already discovered this bridge */
        log.debug("Previously found bridge discovered")
        /* update IP address */
        if (parsedEvent.networkAddress != bridge.value.networkAddress) {
        	bridge.value.networkAddress = parsedEvent.networkAddress
        	def bridgeDev = getChildDevice(parsedEvent.mac)
            if (bridgeDev) {
            	bridgeDev.sendEvent(name: "networkAddress", value: convertHexToIP(bridge.value.networkAddress))
            }
        }
    } else { 
    
        log.debug("Found new bridge.")
        state.unlinked_bridges << ["${parsedEvent.ssdpUSN}":parsedEvent]
   }
}

private processVerifyResponse(physicalgraph.device.HubResponse hubResponse) {
    log.trace "description.xml response (application/xml)"
	def body = hubResponse.xml
    log.debug("Processing verify response.")
    if (body?.device?.modelName?.text().startsWith("Philips hue bridge")) {
        log.debug(body?.device?.UDN?.text())
        def bridge = getUnlinkedBridges().find({it?.key?.contains(body?.device?.UDN?.text())})
        if (bridge) {
            log.debug("found bridge!")
            bridge.value << [name:body?.device?.friendlyName?.text(), serialNumber:body?.device?.serialNumber?.text(), verified: true, itemsDiscovered: false]
        } else {
            log.error " /description.xml returned a bridge that didn't exist"
        }
    }
}


private sendDeveloperReq(mac) {
	//log.debug("Sending developer request to ${ip} (${mac})")
    def token = app.id
    log.debug "MAC is ${mac}"
    
    def bridge = getBridge(mac)
    def host = bridge.value.networkAddress
    host = "${convertHexToIP(host)}" + ":80"
    log.debug "Host is ${host}"
    
	    sendHubCommand(new physicalgraph.device.HubAction([
			method: "POST",
			path: "/api",
			headers: [
					HOST: host
			],
			body: [devicetype: "$token-0"]], "${selectedHue}", [callback: "usernameHandler"]))
}

void usernameHandler(physicalgraph.device.HubResponse hubResponse) {
		def body = hubResponse.json
        log.debug "Button Pressed - Link Done - Save Username"
		if (body.success != null) {
			if (body.success[0] != null) {
				if (body.success[0].username)
                	
                	state.params.linkDone = true
                	state.params.username = body.success[0].username
					//state.username = body.success[0].username
			}
		} else if (body.error != null) {
			//TODO: handle retries...
			log.error "ERROR: application/json ${body.error}"
		}
	}

/**
 * UTILITY FUNCTIONS
 **/
def getCommandData(id) {
    def ids = id.split("/")
    //def bridge = getBridge(ids[0])
    def bridgeDev = getChildDevice(ids[0])
    def result;
    
    if( id.contains("/") ) {
        result = [ip: "${bridgeDev.currentValue("networkAddress")}:80",
                  username: "${bridgeDev.currentValue("username")}",
                  deviceId: "${ids[1] - "BULB" - "GROUP" - "SCENE" - "SCHEDULE"}", ]
    }
    else {
        result = [ip: "${bridgeDev.currentValue("networkAddress")}",
                  username: "${bridgeDev.currentValue("username")}",
                  deviceId: "${ids[0] - "BULB" - "GROUP" - "SCENE" - "SCHEDULE"}", ]
    }
   
    return result
}


def getCommandHub(id) {
    def ids = id.split("/")
    def devId
    def ipAddr
    def userName
 
    log.debug("In getCommandData()")
    log.debug("id = ${id}")
    
    if( id.contains("/") ) {
       devId = ids[1] - "BULB" - "GROUP" - "SCENE" - "SCHEDULE"
    }
    else {
       devId = ids
    }
    
    if( state.host ) {
       def url = state.host.split(":")
       ipAddr = url[0] + ":80"
       userName = state.user
    }
    def result = [ip: "${ipAddr}",
                  username: "${userName}",
                  deviceId: "${devId}",  ]
    
    log.debug("Out getCommandData = ${result}")
    
    return result
}

def getGLightsDNI(groupId,bridgemac) {
	log.trace "getGLightsDNI( from Group ${groupId} )"
    //def mac = state.mac
    def mac = bridgemac
    //log.debug "${mac}"
    def bridge = getBridge(mac)
  
    def groupLights = bridge.value.groups[groupId].lights
    log.debug "bridge.value.groups[groupId].lights = ${groupLights}"
    
    groupLights = groupLights - "[" - "]"
	def gLights = groupLights 
    log.debug "gLights = ${gLights}"
	
    def gLightDevId
	def gLightDNI 
    def gLightsDNI = []
    
    gLights.each { gl ->
    	gLightDevId = "${mac}/BULB${gl}"
        log.debug "Light devId = ${gLightDevId}"
    	gLightDNI = getChildDevice(gLightDevId)
        gLightsDNI << gLightDNI
    }    
    log.debug "gLightsDNI = ${gLightsDNI}"
    
    return gLightsDNI
}    
        
def getSLightsDNI(sceneId) {
	log.trace "getSLightsDNI( from Scene ${sceneId} )"
    def mac = state.mac
    def bridge = getBridge(mac)
    def sceneLights = bridge.value.scene[sceneId].lights
    log.debug "bridge.value.scene[sceneId].lights = ${sceneLights}"
    
    sceneLights = sceneLights - "[" - "]"
	def sLights = sceneLights 
    log.debug "sLights = ${sLights}"
	
    def sLightDevId
	def sLightDNI 
    def sLightsDNI = []
    
    sLights.each { sl ->
    	sLightDevId = "${mac}/BULB${gl}"
        log.debug "Light devId = ${sLightDevId}"
    	sLightDNI = getChildDevice(sLightDevId)
        sLightsDNI << sLightDNI
    }    
    log.debug "sLightsDNI = ${sLightsDNI}"
    
    return sLightsDNI
} 

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

def scaleLevel(level, fromST = false, max = 254) {
	log.trace "ScaleLevel( ${level}, ${fromST}, ${max} )"
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
    log.trace "scaleLevel returned ${scaled}."
    
}

def parse(desc) {
    log.debug("parse")
}

def doDeviceSync(inItems = null) {
	state.limitation = inItems
	log.debug "Doing Hue Device Sync!  inItems = ${inItems}"
    state.doingSync = true
    try {
		subscribe(location, null, locationHandler, [filterEvents:false])
    } catch (e) {
 	}
	state.linked_bridges.each {
		def bridgeDev = getChildDevice(it.value.mac)
        if (bridgeDev) {
			bridgeDev.discoverItems(inItems)
        }
	}
	discoverHueBridges()
    state.doingSync = false
}
