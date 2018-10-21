import * as Types from '../constants/ActionTypes'

const initialState = {
    data: undefined,
    timestamp: 0,
    status: {
        message: "Loading design...",
        error: false
    },
    show_update_design: false
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.DESIGN_LOAD:
      return {
        ...state,
        status: {
            message: "Loading design...",
            error: false
        }
      }
    case Types.DESIGN_LOAD_SUCCESS:
      return {
        ...state,
        status: {
            message: "Design loaded",
            error: false
        },
        data: action.design,
        timestamp: action.timestamp
      }
    case Types.DESIGN_LOAD_FAILURE:
      return {
        ...state,
        status: {
            message: action.error,
            error: true
        },
        data: undefined,
        timestamp: 0
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
    default:
      return state
  }
}

export const getDesign = (state) => {
    return state.preview.data
}

export const getDesignStatus = (state) => {
    return state.preview.status
}

export const getTimestamp = (state) => {
    return state.preview.timestamp
}

export const getShowUpdateDesign = (state) => {
    return state.preview.show_update_design
}

export default reducer
