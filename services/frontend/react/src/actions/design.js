import * as Types from '../constants/ActionTypes'

import * as content from '../reducers/design/content'
import * as dialog from '../reducers/design/dialog'

export const loadDesign = () => ({
  type: Types.DESIGN_LOAD
})

export const loadDesignSuccess = (design, revision) => ({
  type: Types.DESIGN_LOAD_SUCCESS, design, revision
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

export const getRevision = (state) => {
    return content.getRevision(state)
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

