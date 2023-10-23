import * as Types from '../../constants/ActionTypes'

const initialState = {
    show_upload_design: false,
    show_create_design: false,
    show_delete_designs: false,
    show_error_message: false,
    error_message: "",
    uploaded_design_present: false,
    uploaded_design: {}
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.SHOW_CREATE_DESIGN:
      return {
        ...state,
        show_create_design: true
      }
    case Types.HIDE_CREATE_DESIGN:
      return {
        ...state,
        show_create_design: false
      }
    case Types.SHOW_DELETE_DESIGNS:
      return {
        ...state,
        show_delete_designs: true
      }
    case Types.HIDE_DELETE_DESIGNS:
      return {
        ...state,
        show_delete_designs: false
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
    case Types.UPLOADED_DESIGN_CHANGED:
      return {
        ...state,
        uploaded_design_present: action.present,
        uploaded_design: action.design
      }
    default:
      return state
  }
}

export const getShowUploadDesign = (state) => {
    return state.designs.dialog.show_upload_design
}

export const getShowCreateDesign = (state) => {
    return state.designs.dialog.show_create_design
}

export const getShowDeleteDesigns = (state) => {
    return state.designs.dialog.show_delete_designs
}

export const getShowErrorMessage = (state) => {
    return state.designs.dialog.show_error_message
}

export const getErrorMessage = (state) => {
    return state.designs.dialog.error_message
}

export const isUploadedDesignPresent = (state) => {
    return state.designs.dialog.uploaded_design_present
}

export const getUploadedDesign = (state) => {
    return state.designs.dialog.uploaded_design
}

export default reducer
