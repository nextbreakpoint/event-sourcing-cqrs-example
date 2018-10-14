import { SET_DESIGN } from '../constants/ActionTypes'

export const setConfig = (config) => ({
  type: SET_CONFIG,
  config: config
})

export const setAccount = (role, name) => ({
  type: SET_ACCOUNT,
  role: role,
  name: name
})

export const setDesign = (design, timestamp) => ({
  type: SET_DESIGNS,
  design: design,
  timestamp: timestamp
})

