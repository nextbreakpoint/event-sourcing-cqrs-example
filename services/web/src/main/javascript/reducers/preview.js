import { SET_DESIGN, SHOW_UPDATE_DESIGN, HIDE_UPDATE_DESIGN } from '../constants/ActionTypes'

const initialState = {
    design: undefined,
    timestamp: 0,
    show_update_design: false
}

function previewReducer (state = initialState, action) {
  switch (action.type) {
    case SHOW_UPDATE_DESIGN:
      return {
        ...state,
        show_update_design: true
      }
    case HIDE_UPDATE_DESIGN:
      return {
        ...state,
        show_update_design: false
      }
    case SET_DESIGN:
      return {
        ...state,
        design: action.design,
        timestamp: action.timestamp
      }
    default:
      return state
  }
}

export default previewReducer
