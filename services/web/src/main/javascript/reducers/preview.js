import { SET_DESIGN } from '../constants/ActionTypes'

const initialState = {
    config: {},
    role: "anonymous",
    name: "Guest",
    design: {},
    timestamp: "0"
}

function previewReducer (state = initialState, action) {
  switch (action.type) {
    case SET_DESIGN:
      return {
        ...state,
        design: {}
      }
    default:
      return state
  }
}

export default previewReducer
