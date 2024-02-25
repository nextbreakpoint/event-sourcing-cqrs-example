import * as Types from '../../constants/ActionTypes'

const default_script = "fractal {\n\torbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\n\t\tloop [0, 200] (mod2(x) > 40) {\n\t\t\tx = x * x + w;\n\t\t}\n\t}\n\tcolor [#FF000000] {\n\t\tpalette gradient {\n\t\t\t[#FFFFFFFF > #FF000000, 100];\n\t\t\t[#FF000000 > #FFFFFFFF, 100];\n\t\t}\n\t\tinit {\n\t\t\tm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n\t\t}\n\t\trule (n > 0) [1] {\n\t\t\tgradient[m - 1]\n\t\t}\n\t}\n}\n"
const default_metadata = "{\n\t\"translation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":1.0,\n\t\t\"w\":0.0\n\t},\n\t\"rotation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":0.0,\n\t\t\"w\":0.0\n\t},\n\t\"scale\":\n\t{\n\t\t\"x\":1.0,\n\t\t\"y\":1.0,\n\t\t\"z\":1.0,\n\t\t\"w\":1.0\n\t},\n\t\"point\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0\n\t},\n\t\"julia\":false,\n\t\"options\":\n\t{\n\t\t\"showPreview\":false,\n\t\t\"showTraps\":false,\n\t\t\"showOrbit\":false,\n\t\t\"showPoint\":false,\n\t\t\"previewOrigin\":\n\t\t{\n\t\t\t\"x\":0.0,\n\t\t\t\"y\":0.0\n\t\t},\n\t\t\"previewSize\":\n\t\t{\n\t\t\t\"x\":0.25,\n\t\t\t\"y\":0.25\n\t\t}\n\t}\n}"
const default_manifest = "{\"pluginId\":\"Mandelbrot\"}"

const initialState = {
    show_upload_design: false,
    show_create_design: false,
    show_update_design: false,
    show_delete_designs: false,
    show_error_message: false,
    error_message: "",
    selected_design: {
        manifest: default_manifest,
        metadata: default_metadata,
        script: default_script
    }
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
    case Types.SELECTED_DESIGN_CHANGED:
      return {
        ...state,
        selected_design: action.design
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

export const getShowUpdateDesign = (state) => {
    return state.designs.dialog.show_update_design
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

export const getSelectedDesign = (state) => {
    return state.designs.dialog.selected_design
}

export default reducer
