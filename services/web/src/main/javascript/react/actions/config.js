import * as Types from '../constants/ActionTypes'

import * as config from '../reducers/config'

export const loadConfig = () => ({
  type: Types.CONFIG_LOAD
})

export const loadConfigSuccess = (config) => ({
  type: Types.CONFIG_LOAD_SUCCESS, config
})

export const loadConfigFailure = (error) => ({
  type: Types.CONFIG_LOAD_FAILURE, error
})

export const getConfig = (state) => {
    return config.getConfig(state)
}

export const getConfigStatus = (state) => {
    return config.getConfigStatus(state)
}
