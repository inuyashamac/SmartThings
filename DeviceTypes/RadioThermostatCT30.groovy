/**
 *  Radio Thermostat CT30
 *
 *  Copyright 2016 mikempls
 *  Copyright 2013-2016 SmartThings
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
  definition (name: "Radio Thermostat CT30", namespace: "mikempls", author: "mikempls") {
    capability "Thermostat"

    fingerprint profileId: "0104", inClusters: "0000,0001,0003,0009,0201,0202,0402,0800", outClusters: "000A,0800"
  }

  simulator {
    // TODO: define status and reply messages here
  }

  tiles {
    valueTile("temperature", "device.temperature", width: 2, height: 2) {
      state("temperature", label:'${currentValue}°', unit:"F",
        backgroundColors:[
          [value: 31, color: "#153591"],
          [value: 44, color: "#1e9cbb"],
          [value: 59, color: "#90d2a7"],
          [value: 74, color: "#44b621"],
          [value: 84, color: "#f1d801"],
          [value: 95, color: "#d04e00"],
          [value: 96, color: "#bc2323"]
        ]
      )
    }
    standardTile("mode", "device.thermostatMode", inactiveLabel: false, decoration: "flat") {
      state "off", label:'${name}', action:"thermostat.setThermostatMode"
      state "heat", label:'${name}', action:"thermostat.setThermostatMode"
      state "emergencyHeat", label:'${name}', action:"thermostat.setThermostatMode"
    }
    standardTile("fanMode", "device.thermostatFanMode", inactiveLabel: false, decoration: "flat") {
      state "fanAuto", label:'${name}', action:"thermostat.setThermostatFanMode"
      state "fanOn", label:'${name}', action:"thermostat.setThermostatFanMode"
    }
    controlTile("heatSliderControl", "device.heatingSetpoint", "slider", height: 1, width: 2, inactiveLabel: false) {
      state "setHeatingSetpoint", action:"thermostat.setHeatingSetpoint", backgroundColor:"#d04e00"
    }
    valueTile("heatingSetpoint", "device.heatingSetpoint", inactiveLabel: false, decoration: "flat") {
      state "heat", label:'${currentValue}° heat', unit:"F", backgroundColor:"#ffffff"
    }
    standardTile("refresh", "device.temperature", inactiveLabel: false, decoration: "flat") {
      state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
    }
    standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
      state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
    }
    main "temperature"
    details(["temperature", "mode", "fanMode", "heatSliderControl", "heatingSetpoint", "coolSliderControl", "coolingSetpoint", "refresh", "configure"])
  }
}

// parse events into attributes
def parse(String description) {
  log.debug "Parsing '${description}'"
  log.debug "Parse description $description"
  def map = [:]
  if (description?.startsWith("read attr -")) {
    def descMap = parseDescriptionAsMap(description)
    log.debug "Desc Map: $descMap"
    if (descMap.cluster == "0201" && descMap.attrId == "0000") {
      log.debug "TEMP"
      map.name = "temperature"
      map.value = getTemperature(descMap.value)
    } else if (descMap.cluster == "0201" && descMap.attrId == "0011") {
      log.debug "COOLING SETPOINT"
      map.name = "coolingSetpoint"
      map.value = getTemperature(descMap.value)
    } else if (descMap.cluster == "0201" && descMap.attrId == "0012") {
      log.debug "HEATING SETPOINT"
      map.name = "heatingSetpoint"
      map.value = getTemperature(descMap.value)
    } else if (descMap.cluster == "0201" && descMap.attrId == "001c") {
      log.debug "MODE"
      map.name = "thermostatMode"
      map.value = getModeMap()[descMap.value]
    } else if (descMap.cluster == "0202" && descMap.attrId == "0000") {
      log.debug "FAN MODE"
      map.name = "thermostatFanMode"
      map.value = getFanModeMap()[descMap.value]
    }
  }

  def result = null
  if (map) {
    result = createEvent(map)
  }
  log.debug "Parse returned $map"
  return result
}


def parseDescriptionAsMap(description) {
  (description - "read attr - ").split(",").inject([:]) { map, param ->
    def nameAndValue = param.split(":")
    map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
  }
}

def getModeMap() { [
  "00":"off",
  "04":"heat",
]}

def getFanModeMap() { [
  "04":"fanOn",
  "05":"fanAuto"
]}

def refresh()
{
  log.debug "refresh called"
  [
    "st rattr 0x${device.deviceNetworkId} 0xA 0x201 0", "delay 200",
    "st rattr 0x${device.deviceNetworkId} 0xA 0x201 0x11", "delay 200",
      "st rattr 0x${device.deviceNetworkId} 0xA 0x201 0x12", "delay 200",
    "st rattr 0x${device.deviceNetworkId} 0xA 0x201 0x1C", "delay 200",
    "st rattr 0x${device.deviceNetworkId} 0xA 0x202 0"
  ]
}

def getTemperature(value) {
  if (value != null) {
    def celsius = Integer.parseInt(value, 16) / 100
    if (getTemperatureScale() == "C") {
      return celsius
    } else {
      return Math.round(celsiusToFahrenheit(celsius))
    }
  }
}

def setHeatingSetpoint(degrees) {
  if (degrees != null) {
    def temperatureScale = getTemperatureScale()

    def degreesInteger = Math.round(degrees)
    log.debug "setHeatingSetpoint({$degreesInteger} ${temperatureScale})"
    sendEvent("name": "heatingSetpoint", "value": degreesInteger)

    def celsius = (getTemperatureScale() == "C") ? degreesInteger : (fahrenheitToCelsius(degreesInteger) as Double).round(2)
    "st wattr 0x${device.deviceNetworkId} 0xA 0x201 0x12 0x29 {" + hex(celsius * 100) + "}"
  }
}

def modes() {
  ["off", "heat", "cool"]
}

def setThermostatMode() {
  log.debug "switching thermostatMode"
  def currentMode = device.currentState("thermostatMode")?.value
  def modeOrder = modes()
  def index = modeOrder.indexOf(currentMode)
  def next = index >= 0 && index < modeOrder.size() - 1 ? modeOrder[index + 1] : modeOrder[0]
  log.debug "switching mode from $currentMode to $next"
  "$next"()
}

def setThermostatFanMode() {
  log.debug "Switching fan mode"
  def currentFanMode = device.currentState("thermostatFanMode")?.value
  log.debug "switching fan from current mode: $currentFanMode"
  def returnCommand

  switch (currentFanMode) {
    case "fanAuto":
      returnCommand = fanOn()
      break
    case "fanOn":
      returnCommand = fanAuto()
      break
  }
  if(!currentFanMode) { returnCommand = fanAuto() }
  returnCommand
}

def setThermostatMode(String value) {
  log.debug "setThermostatMode({$value})"
  "$value"()
}

def setThermostatFanMode(String value) {
  log.debug "setThermostatFanMode({$value})"
  "$value"()
}

def off() {
  log.debug "off"
  sendEvent("name":"thermostatMode", "value":"off")
  "st wattr 0x${device.deviceNetworkId} 0xA 0x201 0x1C 0x30 {00}"
}

def heat() {
  log.debug "heat"
  sendEvent("name":"thermostatMode", "value":"heat")
  "st wattr 0x${device.deviceNetworkId} 0xA 0x201 0x1C 0x30 {04}"
}

def on() {
  fanOn()
}

def fanOn() {
  log.debug "fanOn"
  sendEvent("name":"thermostatFanMode", "value":"fanOn")
  "st wattr 0x${device.deviceNetworkId} 0xA 0x202 0 0x30 {04}"
}

def auto() {
  fanAuto()
}

def fanAuto() {
  log.debug "fanAuto"
  sendEvent("name":"thermostatFanMode", "value":"fanAuto")
  "st wattr 0x${device.deviceNetworkId} 0xA 0x202 0 0x30 {05}"
}

def configure() {

  log.debug "binding to Thermostat and Fan Control cluster"
  [
    "zdo bind 0x${device.deviceNetworkId} 0x1 0xA 0x201 {${device.zigbeeId}} {}", "delay 200",
    "zdo bind 0x${device.deviceNetworkId} 0x1 0xA 0x202 {${device.zigbeeId}} {}"
  ]
}

private hex(value) {
  new BigInteger(Math.round(value).toString()).toString(16)
}
