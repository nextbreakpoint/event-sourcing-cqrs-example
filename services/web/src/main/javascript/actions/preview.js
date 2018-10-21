import * as Types from '../constants/ActionTypes'

import * as preview from '../reducers/preview'

export const loadDesign = () => ({
  type: Types.DESIGN_LOAD
})

export const loadDesignSuccess = (design, timestamp) => ({
  type: Types.DESIGN_LOAD_SUCCESS, design, timestamp
})

export const loadDesignFailure = (error) => ({
  type: Types.DESIGN_LOAD_FAILURE, error
})

export const showUpdateDesign = () => ({
  type: Types.SHOW_UPDATE_DESIGN
})

export const hideUpdateDesign = () => ({
  type: Types.HIDE_UPDATE_DESIGN
})

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
