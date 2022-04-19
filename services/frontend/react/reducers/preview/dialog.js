import * as Types from '../../constants/ActionTypes'

const initialState = {
    show_update_design: false,
    show_error_message: false,
    error_message: ""
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.SHOW_UPDATE_DESIGN:
      return {
        ...state,
        show_update_design: true
      }
    case Types.HIDE_UPDATE_DESIGN:
      return {
        ...state,
        show_update_design: false
      }
    case Types.SHOW_ERROR_MESSAGE:
      return {
        ...state,
        show_error_message: true,
        error_message: action.error
      }
    case Types.HIDE_ERROR_MESSAGE:
      return {
        ...state,
        show_error_message: false
      }
    default:
      return state
  }
}

export const getShowUpdateDesign = (state) => {
    return state.preview.dialog.show_update_design
}

export const getShowErrorMessage = (state) => {
    return state.preview.dialog.show_error_message
}

export const getErrorMessage = (state) => {
    return state.preview.dialog.error_message
}

export default reducer
