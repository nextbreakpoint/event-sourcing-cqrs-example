import { SET_DESIGN, SHOW_UPDATE_DESIGN, HIDE_UPDATE_DESIGN } from '../constants/ActionTypes'

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
  type: SET_DESIGN,
  design: design,
  timestamp: timestamp
})

export const showUpdateDesign = () => ({
  type: SHOW_UPDATE_DESIGN
})

export const hideUpdateDesign = () => ({
  type: HIDE_UPDATE_DESIGN
})

