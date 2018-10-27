import * as Types from '../constants/ActionTypes'

import * as content from '../reducers/preview/content'
import * as dialog from '../reducers/preview/dialog'

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

export const showErrorMessage = (error) => ({
  type: Types.SHOW_ERROR_MESSAGE, error
})

export const hideErrorMessage = () => ({
  type: Types.HIDE_ERROR_MESSAGE
})

export const getDesign = (state) => {
    return content.getDesign(state)
}

export const getDesignStatus = (state) => {
    return content.getDesignStatus(state)
}

export const getTimestamp = (state) => {
    return content.getTimestamp(state)
}

export const getShowUpdateDesign = (state) => {
    return dialog.getShowUpdateDesign(state)
}

export const getShowErrorMessage = (state) => {
    return dialog.getShowErrorMessage(state)
}

export const getErrorMessage = (state) => {
    return dialog.getErrorMessage(state)
}

