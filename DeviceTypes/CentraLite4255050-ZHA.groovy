/**
 *  CentraLite Switch (4255050-ZHA, Xfinity Home)
 *  Author: Mike Nicholson
 *  Date: May 25, 2015
 *
 *  Copyright 2015-2016 mikempls 
 *  Copyright 2014-2015 SmartThings 
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

 /* To (re)pair the device, hold the button in while inserting into the mains outlet.
  * Release the button (after ~2s) as soon as the LED is illuminated. */

metadata {
  definition (name: "CentraLite Switch 4255050-ZHA", namespace: "mikempls", author: "mikempls") {
    capability "Actuator"
    capability "Switch"
    capability "Configuration"
    capability "Sensor"

    fingerprint profileId: "0104", inClusters: "0000,0003,0004,0005,0006,0008"
        // 0104 - HA profile 
        
        // 0000 - basic cluster
        // 0003 - identity cluster
        // 0004 - groups cluster
        // 0005 - scenes cluster
        // 0006 - on/off cluster
        // 0008 - level control cluster
  }

  // simulator metadata
  simulator {
    // status messages
    status "on": "on/off: 1"
    status "off": "on/off: 0"

    // reply messages
    reply "zcl on-off on": "on/off: 1"
    reply "zcl on-off off": "on/off: 0"
  }

  // UI tile definitions
  tiles {
    standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
      state "off", label: 'OFF', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
      state "on", label: 'ON', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
    }
    main "switch"
    details(["switch"])
  }
}

// Parse incoming device messages to generate events
def parse(String description) {
  log.debug "Parse description $description"
  def name = null
  def value = null
  if (description?.startsWith("read attr -")) {
    def descMap = parseDescriptionAsMap(description)
    log.debug "Read attr: $description"
    if (descMap.cluster == "0006" && descMap.attrId == "0000") {
      name = "switch"
      value = descMap.value.endsWith("01") ? "on" : "off"
    }
  } else if (description?.startsWith("on/off:")) {
    log.debug "Switch command"
    name = "switch"
    value = description?.endsWith(" 1") ? "on" : "off"
  } else if (description?.startsWith("catchall: 0104 0006")) {
      log.debug "Switch command catchall"
      name = "switch"
      value = description?.endsWith("0000001001") || description?.endsWith("0B 01 0100") ? "on" : "off"
    }
    
  def result = createEvent(name: name, value: value)
  log.debug "Parse returned ${result?.descriptionText}"
  return result
}

def parseDescriptionAsMap(description) {
  (description - "read attr - ").split(",").inject([:]) { map, param ->
    def nameAndValue = param.split(":")
    map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
  }
}

// Commands to device
def on() {
  'zcl on-off on'
}

def off() {
  'zcl on-off off'
}

def refresh() {
"st rattr 0x${device.deviceNetworkId} 1 0006 0000"
}

def configure() {
  [
    "zdo bind 0x${device.deviceNetworkId} 1 1 6 {${device.zigbeeId}} {}", "delay 200",
  ]
}

