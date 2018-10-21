import * as Types from '../constants/ActionTypes'

import * as config from '../reducers/config'
import * as account from '../reducers/account'
import * as preview from '../reducers/preview'

export const loadConfig = () => ({
  type: Types.CONFIG_LOAD
})

export const loadConfigSuccess = (config) => ({
  type: Types.CONFIG_LOAD_SUCCESS,
  config
})

export const loadConfigFailure = (error) => ({
  type: Types.CONFIG_LOAD_FAILURE,
  error
})

export const loadAccount = () => ({
  type: Types.ACCOUNT_LOAD
})

export const loadAccountSuccess = (account) => ({
  type: Types.ACCOUNT_LOAD_SUCCESS,
  account
})

export const loadAccountFailure = (error) => ({
  type: Types.ACCOUNT_LOAD_FAILURE,
  error
})

export const loadDesign = () => ({
  type: Types.DESIGN_LOAD
})

export const loadDesignSuccess = (design, timestamp) => ({
  type: Types.DESIGN_LOAD_SUCCESS,
  design,
  timestamp
})

export const loadDesignFailure = (error) => ({
  type: Types.DESIGN_LOAD_FAILURE,
  error
})

export const showUpdateDesign = () => ({
  type: Types.SHOW_UPDATE_DESIGN
})

export const hideUpdateDesign = () => ({
  type: Types.HIDE_UPDATE_DESIGN
})

export const getConfig = (state) => {
    return config.getConfig(state)
}

export const getConfigStatus = (state) => {
    return config.getConfigStatus(state)
}

export const getAccount = (state) => {
    return account.getAccount(state)
}

export const getAccountStatus = (state) => {
    return account.getAccountStatus(state)
}

export const getDesign = (state) => {
    return preview.getDesign(state)
}

export const getDesignStatus = (state) => {
    return preview.getDesignStatus(state)
}

export const getTimestamp = (state) => {
    return preview.getTimestamp(state)
}

export const getShowUpdateDesign = (state) => {
    return preview.getShowUpdateDesign(state)
}
