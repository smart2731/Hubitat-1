/**
 *  ****************  Snapshot Child  ****************
 *
 *  Design Usage:
 *  Monitor lights, devices and sensors. Easily see their status right on your dashboard.
 *
 *  Copyright 2019 Bryan Turcotte (@bptworld)
 *
 *  This App is free.  If you like and use this app, please be sure to give a shout out on the Hubitat forums to let
 *  people know that it exists!  Thanks.
 *
 *  Remember...I am not a programmer, everything I do takes a lot of time and research!
 *  Donations are never necessary but always appreciated.  Donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://paypal.me/bptworld
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  If modifying this project, please keep the above header intact and add your comments/credits below - Thank you! -  @BPTWorld
 *
 *  App and Driver updates can be found at https://github.com/bptworld/Hubitat/
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  Changes:
 *
 *	V1.0.2 - 03/30/19 - Added ability to select what type of data to report: Full, Only On/Off, Only Open/Closed. Also added count attributes.
 *	V1.0.1 - 03/22/19 - Major update to comply with Hubitat's new dashboard requirements.
 *  V1.0.0 - 03/16/19 - Initial Release
 *
 */

def setVersion() {
	state.version = "v1.0.2"
}

definition(
	name: "Snapshot Child",
	namespace: "BPTWorld",
	author: "Bryan Turcotte",
	description: "Monitor lights, devices and sensors. Easily see their status right on your dashboard.",
	category: "Convenience",
	parent: "BPTWorld:Snapshot",
	iconUrl: "",
	iconX2Url: "",
	iconX3Url: "",
)

preferences {
	page(name: "pageConfig")
}

def pageConfig() {
	dynamicPage(name: "", title: "<h2 style='color:#1A77C9;font-weight: bold'>Snapshot</h2>", install: true, uninstall: true, refreshInterval:0) {	
    display()
		section("Instructions:", hideable: true, hidden: true) {
			paragraph "<b>Notes:</b>"
			paragraph "REAL TIME - Be careful with this option. Too many devices updating in real time <u>WILL</u> slow down and/or crash the hub."	
		}
		section(getFormat("header-green", "${getImage("Blank")}"+"  Type of Trigger")) {
			input "triggerMode", "enum", required: true, title: "Select Trigger Frequency", submitOnChange: true,  options: ["Real Time", "Every X minutes", "On Demand"]
			if(triggerMode == "Real Time") {
				paragraph "<b>Be careful with this option. Too many devices updating in real time <u>WILL</u> slow down and/or crash the hub.</b>"
				input "realTimeSwitch", "capability.switch", title: "App Control Switch", required: true
			}
			if(triggerMode == "Every X minutes") {
				paragraph "<b>Choose how often to take a Snapshot of your selected devices.</b>"
				input "repeatSwitch", "capability.switch", title: "App Control Switch", required: true
				input "timeDelay", "number", title: "Every X Minutes (1 to 60)", required: true, range: '1..60'
			}
			if(triggerMode == "On Demand") {
				paragraph "<b>Only take a snapshot when this switch is turned on OR the Maintenance Reset button is pressed.</b>"
				paragraph "Recommended to create a virtual device with 'Enable auto off' set to '1s'"
				input "onDemandSwitch", "capability.switch", title: "App Control Switch", required: true
			}
		}
		section(getFormat("header-green", "${getImage("Blank")}"+"  Devices to Monitor")) {
			paragraph "Note: Choose a max of 30 devices in each category."
            input "switches", "capability.switch", title: "Switches", multiple: true, required: false, submitOnChange: true
            input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false, submitOnChange: true
        }
		section(getFormat("header-green", "${getImage("Blank")}"+"  Display Options")) {
			paragraph "Choose the amount/type of data to record"
			if(switches) {
				input "switchMode", "enum", required: true, title: "Select Switches Display Type", submitOnChange: true,  options: ["Full", "Only On", "Only Off"]
			}
			if(contacts) {
				input "contactMode", "enum", required: true, title: "Select Contact Display Type", submitOnChange: true,  options: ["Full", "Only Open", "Only Closed"]
			}
		}
		section(getFormat("header-green", "${getImage("Blank")}"+" Dashboard Tile")) {}
		section("Instructions for Dashboard Tile:", hideable: true, hidden: true) {
			paragraph "<b>Want to be able to view your data on a Dashboard? Now you can, simply follow these instructions!</b>"
			paragraph " - Create a new 'Virtual Device'<br> - Name it something catchy like: 'Snapshot Tile'<br> - Use our 'Snapshot Tile' Driver<br> - Then select this new device below"
			paragraph "Now all you have to do is add this device to one of your dashboards to see your counts on a tile!<br>Add a new tile with the following selections"
			paragraph "- Pick a device = Snapshot Tile<br>- Pick a template = attribute<br>- 3rd box = snapshotSwitch1-6 or snapshotContact1-6"
		}
		section() {
			input(name: "snapshotTileDevice", type: "capability.actuator", title: "Vitual Device created to send the data to:", submitOnChange: true, required: false, multiple: false)
		}
		section(getFormat("header-green", "${getImage("Blank")}"+"  Maintenance")) {
			paragraph "When removing devices from app, it will be necessary to reset the maps. After turning on switch, Click the button that will appear. All tables will be cleared and repopulated with the current devices."
            input(name: "maintSwitch", type: "bool", defaultValue: "false", title: "Clear all tables", description: "Clear all tables", submitOnChange: "true")
			if(maintSwitch) input "resetBtn", "button", title: "Click here to reset maps"
        }
		section(getFormat("header-green", "${getImage("Blank")}"+" General")) {label title: "Enter a name for this automation", required: false, submitOnChange: true}
		section() {
			input(name: "enablerSwitch1", type: "capability.switch", title: "Enable/Disable child app with this switch - If Switch is ON then app is disabled, if Switch is OFF then app is active.", required: false, multiple: false)
		}
        section() {
            input(name: "debugMode", type: "bool", defaultValue: "true", title: "Enable Debug Logging", description: "Enable extra logging for debugging.")
		}
		display2()
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	LOGDEBUG("Updated with settings: ${settings}")
	unsubscribe()
	logCheck()
	initialize()
}

def initialize() {
	if(enableSwitch1) subscribe(enableSwitch, "switch", switchEnable)
	if(triggerMode == "On Demand") subscribe(onDemandSwitch, "switch", onDemandSwitchHandler)
	if(triggerMode == "Every X minutes") subscribe(repeatSwitch, "switch", repeatSwitchHandler)
	if(triggerMode == "Real Time") subscribe(realTimeSwitch, "switch", realTimeSwitchHandler)
}

def realTimeSwitchHandler(evt) {
	LOGDEBUG("In realTimeSwitchHandler...")
	state.realTimeSwitchStatus = evt.value
	if(state.realTimeSwitchStatus == "on") {
		LOGDEBUG("In realTimeSwitchHandler - subscribe")
		subscribe(switches, "switch", switchHandler)
		subscribe(contacts, "contact", contactHandler)
		runIn(1, maintHandler)
	} else {
		LOGDEBUG("In realTimeSwitchHandler - unsubscribe")
		unsubscribe(switches)
		unsubscribe(contacts)
	}
}

def repeatSwitchHandler(evt) {
	LOGDEBUG("In repeatSwitchHandler...")
	state.repeatSwitchStatus = repeatSwitch.currentValue("switch")
	state.runDelay = timeDelay * 60
	if(state.repeatSwitchStatus == "on") {
		maintHandler()
	}
	runIn(state.runDelay,repeatSwitchHandler)
}

def onDemandSwitchHandler(evt) {
	LOGDEBUG("In onDemandSwitchHandler...")
	state.onDemandSwitchStatus = evt.value
	if(state.onDemandSwitchStatus == "on") maintHandler()
}

def switchMapHandler() {
	LOGDEBUG("In switchMapHandler...")
	checkMaps()
	state.onSwitchMapS = state.onSwitchMap.sort { a, b -> a.key <=> b.key }
	state.offSwitchMapS = state.offSwitchMap.sort { a, b -> a.key <=> b.key }
	
	state.fSwitchMap1S = "<table width='100%'>"
	state.fSwitchMap2S = "<table width='100%'>"
	state.fSwitchMap3S = "<table width='100%'>"
	state.fSwitchMap4S = "<table width='100%'>"
	state.fSwitchMap5S = "<table width='100%'>"
	state.fSwitchMap6S = "<table width='100%'>"
	state.count = 0
	state.countOn = 0
	state.countOff = 0
	
	if(switchMode == "Full" || switchMode == "Only On") {
		state.onSwitchMapS.each { stuffOn -> 
			state.count = state.count + 1
			state.countOn = state.countOn + 1
			LOGDEBUG("In switchMapHandler - Building Table ON with ${stuffOn.key} count: ${state.count}")
			if((state.count >= 1) && (state.count <= 5)) state.fSwitchMap1S += "<tr><td>${stuffOn.key}</td><td><div style='color: red;'>on</div></td></tr>"
			if((state.count >= 6) && (state.count <= 10)) state.fSwitchMap2S += "<tr><td>${stuffOn.key}</td><td><div style='color: red;'>on</div></td></tr>"
			if((state.count >= 11) && (state.count <= 15)) state.fSwitchMap3S += "<tr><td>${stuffOn.key}</td><td><div style='color: red;'>on</div></td></tr>"
			if((state.count >= 16) && (state.count <= 20)) state.fSwitchMap4S += "<tr><td>${stuffOn.key}</td><td><div style='color: red;'>on</div></td></tr>"
			if((state.count >= 21) && (state.count <= 25)) state.fSwitchMap5S += "<tr><td>${stuffOn.key}</td><td><div style='color: red;'>on</div></td></tr>"
			if((state.count >= 26) && (state.count <= 30)) state.fSwitchMap6S += "<tr><td>${stuffOn.key}</td><td><div style='color: red;'>on</div></td></tr>"
		}
	}
	
	if(switchMode == "Full") {
		if((state.count >= 1) && (state.count <= 5)) { state.fSwitchMap1S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
		if((state.count >= 6) && (state.count <= 10)) { state.fSwitchMap2S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
		if((state.count >= 11) && (state.count <= 15)) { state.fSwitchMap3S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
		if((state.count >= 16) && (state.count <= 20)) { state.fSwitchMap4S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
		if((state.count >= 21) && (state.count <= 25)) { state.fSwitchMap5S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
		if((state.count >= 26) && (state.count <= 30)) { state.fSwitchMap6S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
	}
	
	if(switchMode == "Full" || switchMode == "Only Off") {
		state.offSwitchMapS.each { stuffOff -> 
			state.count = state.count + 1
			state.countOff = state.countOff + 1
			LOGDEBUG("In switchMapHandler - Building Table OFF with ${stuffOff.key} count: ${state.count}")
			if((state.count >= 1) && (state.count <= 5)) state.fSwitchMap1S += "<tr><td>${stuffOff.key}</td><td><div style='color: green;'>off</div></td></tr>"
			if((state.count >= 6) && (state.count <= 10)) state.fSwitchMap2S += "<tr><td>${stuffOff.key}</td><td><div style='color: green;'>off</div></td></tr>"
			if((state.count >= 11) && (state.count <= 15)) state.fSwitchMap3S += "<tr><td>${stuffOff.key}</td><td><div style='color: green;'>off</div></td></tr>"
			if((state.count >= 16) && (state.count <= 20)) state.fSwitchMap4S += "<tr><td>${stuffOff.key}</td><td><div style='color: green;'>off</div></td></tr>"	
			if((state.count >= 21) && (state.count <= 25)) state.fSwitchMap5S += "<tr><td>${stuffOff.key}</td><td><div style='color: green;'>off</div></td></tr>"	
			if((state.count >= 26) && (state.count <= 30)) state.fSwitchMap6S += "<tr><td>${stuffOff.key}</td><td><div style='color: green;'>off</div></td></tr>"	
		}
	}
	
	state.fSwitchMap1S += "</table>"
	state.fSwitchMap2S += "</table>"
	state.fSwitchMap3S += "</table>"
	state.fSwitchMap4S += "</table>"
	state.fSwitchMap5S += "</table>"
	state.fSwitchMap6S += "</table>"
	
	LOGDEBUG("In switchMapHandler - <br>fSwitchMap1S<br>${state.fSwitchMap1S}")
    snapshotTileDevice.sendSnapshotSwitchMap1(state.fSwitchMap1S)
	snapshotTileDevice.sendSnapshotSwitchMap2(state.fSwitchMap2S)
	snapshotTileDevice.sendSnapshotSwitchMap3(state.fSwitchMap3S)
	snapshotTileDevice.sendSnapshotSwitchMap4(state.fSwitchMap4S)
	snapshotTileDevice.sendSnapshotSwitchMap5(state.fSwitchMap5S)
	snapshotTileDevice.sendSnapshotSwitchMap6(state.fSwitchMap6S)
	snapshotTileDevice.sendSnapshotSwitchCountOn(state.countOn)
	snapshotTileDevice.sendSnapshotSwitchCountOff(state.countOff)
}

def contactMapHandler() {
	LOGDEBUG("In contactMapHandler...")
	checkMaps()
	LOGDEBUG("In contactMapHandler - Sorting Maps")
	state.openContactMapS = state.openContactMap.sort { a, b -> a.key <=> b.key }
	state.closedContactMapS = state.closedContactMap.sort { a, b -> a.key <=> b.key }
	
	state.fContactMap1S = "<table width='100%'>"
	state.fContactMap2S = "<table width='100%'>"
	state.fContactMap3S = "<table width='100%'>"
	state.fContactMap4S = "<table width='100%'>"
	state.fContactMap5S = "<table width='100%'>"
	state.fContactMap6S = "<table width='100%'>"
	state.count = 0
	state.countOpen = 0
	state.countClosed = 0
	
	if(contactMode == "Full" || contactMode == "Only Open") {
		state.openContactMapS.each { stuffOpen -> 
			state.count = state.count + 1
			state.countOpen = state.countOpen + 1
			LOGDEBUG("In contactMapHandler - Building Table OPEN with ${stuffOpen.key} count: ${state.count}")
			if((state.count >= 1) && (state.count <= 5)) state.fContactMap1S += "<tr><td>${stuffOpen.key}</td><td><div style='color: red;'>open</div></td></tr>"
			if((state.count >= 6) && (state.count <= 10)) state.fContactMap2S += "<tr><td>${stuffOpen.key}</td><td><div style='color: red;'>open</div></td></tr>"
			if((state.count >= 11) && (state.count <= 15)) state.fContactMap3S += "<tr><td>${stuffOpen.key}</td><td><div style='color: red;'>open</div></td></tr>"
			if((state.count >= 16) && (state.count <= 20)) state.fContactMap4S += "<tr><td>${stuffOpen.key}</td><td><div style='color: red;'>open</div></td></tr>"
			if((state.count >= 21) && (state.count <= 25)) state.fContactMap5S += "<tr><td>${stuffOpen.key}</td><td><div style='color: red;'>open</div></td></tr>"
			if((state.count >= 26) && (state.count <= 30)) state.fContactMap6S += "<tr><td>${stuffOpen.key}</td><td><div style='color: red;'>open</div></td></tr>"
		}
	}
	
	if(contactMode == "Full") {
		if((state.count >= 1) && (state.count <= 5)) { state.fContactMap1S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
		if((state.count >= 6) && (state.count <= 10)) { state.fContactMap2S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
		if((state.count >= 11) && (state.count <= 15)) { state.fContactMap3S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
		if((state.count >= 16) && (state.count <= 20)) { state.fContactMap4S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
		if((state.count >= 21) && (state.count <= 25)) { state.fContactMap5S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
		if((state.count >= 26) && (state.count <= 30)) { state.fContactMap6S += "<tr><td colspan='2'><hr></td></tr>"; state.count = state.count + 1 }
	}
	
	if(contactMode == "Full" || contactMode == "Only Closed") {
		state.closedContactMapS.each { stuffClosed -> 
			state.count = state.count + 1
			state.countClosed = state.countClosed + 1
			LOGDEBUG("In contactMapHandler - Building Table CLOSED with ${stuffClosed.key} count: ${state.count}")
			if((state.count >= 1) && (state.count <= 5)) state.fContactMap1S += "<tr><td>${stuffClosed.key}</td><td><div style='color: green;'>closed</div></td></tr>"
			if((state.count >= 6) && (state.count <= 10)) state.fContactMap2S += "<tr><td>${stuffClosed.key}</td><td><div style='color: green;'>closed</div></td></tr>"
			if((state.count >= 11) && (state.count <= 15)) state.fContactMap3S += "<tr><td>${stuffClosed.key}</td><td><div style='color: green;'>closed</div></td></tr>"
			if((state.count >= 16) && (state.count <= 20)) state.fContactMap4S += "<tr><td>${stuffClosed.key}</td><td><div style='color: green;'>closed</div></td></tr>"
			if((state.count >= 21) && (state.count <= 25)) state.fContactMap5S += "<tr><td>${stuffClosed.key}</td><td><div style='color: green;'>closed</div></td></tr>"
			if((state.count >= 26) && (state.count <= 30)) state.fContactMap6S += "<tr><td>${stuffClosed.key}</td><td><div style='color: green;'>closed</div></td></tr>"
		}
	}
	
	state.fContactMap1S += "</table>"
	state.fContactMap2S += "</table>"
	state.fContactMap3S += "</table>"
	state.fContactMap4S += "</table>"
	state.fContactMap5S += "</table>"
	state.fContactMap6S += "</table>"

	LOGDEBUG("In contactMapHandler - <br>fContactMap1S<br>${state.fContactMap1S}")
   	snapshotTileDevice.sendSnapshotContactMap1(state.fContactMap1S)
	snapshotTileDevice.sendSnapshotContactMap2(state.fContactMap2S)
	snapshotTileDevice.sendSnapshotContactMap3(state.fContactMap3S)
	snapshotTileDevice.sendSnapshotContactMap4(state.fContactMap4S)
	snapshotTileDevice.sendSnapshotContactMap5(state.fContactMap5S)
	snapshotTileDevice.sendSnapshotContactMap6(state.fContactMap6S)
	snapshotTileDevice.sendSnapshotContactCountOpen(state.countOpen)
	snapshotTileDevice.sendSnapshotContactCountClosed(state.countClosed)
}

def switchHandler(evt){
	def switchName = evt.displayName
	def switchStatus = evt.value
	LOGDEBUG("In switchHandler...${switchName} - ${switchStatus}")
	if(switchStatus == "on") {
		state.offSwitchMap.remove(switchName)
		state.onSwitchMap.put(switchName, switchStatus)
		LOGDEBUG("In switchHandler - ON<br>${state.onSwitchMap}")
	}
	if(switchStatus == "off") {
		state.onSwitchMap.remove(switchName)
		state.offSwitchMap.put(switchName, switchStatus)
		LOGDEBUG("In switchHandler - OFF<br>${state.offSwitchMap}")
	}
	switchMapHandler()
}

def contactHandler(evt){
	def contactName = evt.displayName
	def contactStatus = evt.value
	LOGDEBUG("In contactHandler...${contactName}: ${contactStatus}")
	if(contactStatus == "open") {
		state.closedContactMap.remove(contactName)
		state.openContactMap.put(contactName, contactStatus)
		LOGDEBUG("In contactHandler - OPEN<br>${state.openContactMap}")
	}
	if(contactStatus == "closed") {
		state.openContactMap.remove(contactName)
		state.closedContactMap.put(contactName, contactStatus)
		LOGDEBUG("In contactHandler - CLOSED<br>${state.closedContactMap}")
	}
	contactMapHandler()
}

def checkMaps() {
	LOGDEBUG("In checkMaps...") 
	if(state.offSwitchMap == null) {
		state.offSwitchMap = [:]
	}
	if(state.onSwitchMap == null) {
		state.onSwitchMap = [:]
	}
	if(state.closedContactMap == null) {
		state.closedContactMap = [:]
	}
	if(state.openContactMap == null) {
		state.openContactMap = [:]
	}
	LOGDEBUG("In checkMaps - Finished")
}

def maintHandler(evt){
	LOGDEBUG("In maintHandler...")
	state.offSwitchMap = [:]
	state.onSwitchMap = [:]
	state.closedContactMap = [:]
	state.openContactMap = [:] 
	LOGDEBUG("In maintHandler...Tables have been cleared!")
	LOGDEBUG("In maintHandler...Repopulating tables")
	switches.each { device ->
		def switchName = device.displayName
		def switchStatus = device.currentValue('switch')
		LOGDEBUG("In maintHandler - Working on ${switchName} - ${switchStatus}")
		if(switchStatus == "on") state.onSwitchMap.put(switchName, switchStatus)
		if(switchStatus == "off") state.offSwitchMap.put(switchName, switchStatus)
	}
	switchMapHandler()
	contacts.each { device ->
		def contactName = device.displayName
		def contactStatus = device.currentValue('contact')
		LOGDEBUG("In maintHandler - Working on ${contactName} - ${contactStatus}")
		if(contactStatus == "open") state.openContactMap.put(contactName, contactStatus)
		if(contactStatus == "closed") state.closedContactMap.put(contactName, contactStatus)
	}
	contactMapHandler()
}

def appButtonHandler(btn){  // *****************************
	// section(){input "resetBtn", "button", title: "Click here to reset maps"}
    runIn(1, maintHandler)
}  

// Normal Stuff

def pauseOrNot(){							// Modified from @Cobra Code
	LOGDEBUG("In pauseOrNot...")
    state.pauseNow = pause1
        if(state.pauseNow == true){
            state.pauseApp = true
            if(app.label){
            if(app.label.contains('red')){
                log.warn "Paused"}
            else{app.updateLabel(app.label + ("<font color = 'red'> (Paused) </font>" ))
              LOGDEBUG("App Paused - state.pauseApp = $state.pauseApp ")   
            }
            }
        }
     if(state.pauseNow == false){
         state.pauseApp = false
         if(app.label){
     if(app.label.contains('red')){ app.updateLabel(app.label.minus("<font color = 'red'> (Paused) </font>" ))
     	LOGDEBUG("App Released - state.pauseApp = $state.pauseApp ")                          
        }
     }
  }    
}

def logCheck(){								// Modified from @Cobra Code
	state.checkLog = debugMode
	if(state.checkLog == true){
		log.info "${app.label} - All Logging Enabled"
	}
	else if(state.checkLog == false){
		log.info "${app.label} - Further Logging Disabled"
	}
}

def LOGDEBUG(txt){							// Modified from @Cobra Code
    try {
		if (settings.debugMode) { log.debug("${app.label} - ${txt}") }
    } catch(ex) {
    	log.error("${app.label} - LOGDEBUG unable to output requested data!")
    }
}

def getImage(type) {						// Modified from @Stephack Code
    def loc = "<img src=https://raw.githubusercontent.com/bptworld/Hubitat/master/resources/images/"
    if(type == "Blank") return "${loc}blank.png height=40 width=5}>"
}

def getFormat(type, myText=""){				// Modified from @Stephack Code
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<div style='color:blue;font-weight: bold'>${myText}</div>"
}

def display() {
	section() {
		paragraph getFormat("line")
		input "pause1", "bool", title: "Pause This App", required: true, submitOnChange: true, defaultValue: false
	}
}

def display2(){
	setVersion()
	section() {
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center'>Snapshot - @BPTWorld<br><a href='https://github.com/bptworld/Hubitat' target='_blank'>Find more apps on my Github, just click here!</a><br>Get app update notifications and more with <a href='https://github.com/bptworld/Hubitat/tree/master/Apps/App%20Watchdog' target='_blank'>App Watchdog</a><br>${state.version}</div>"
	}       
} 