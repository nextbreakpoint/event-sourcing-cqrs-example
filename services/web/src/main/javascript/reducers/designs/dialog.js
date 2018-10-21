import * as Types from '../../constants/ActionTypes'

const initialState = {
    show_create_design: false,
    show_delete_designs: false
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
    default:
      return state
  }
}

export const getShowCreateDesign = (state) => {
    return state.designs.dialog.show_create_design
}

export const getShowDeleteDesigns = (state) => {
    return state.designs.dialog.show_delete_designs
}

export default reducer
