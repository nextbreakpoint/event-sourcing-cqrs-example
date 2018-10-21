import * as Types from '../../constants/ActionTypes'

const initialState = {
    data: undefined,
    timestamp: 0,
    status: {
        message: "Loading design...",
        error: false
    }
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
    default:
      return state
  }
}

export const getDesign = (state) => {
    return state.preview.content.data
}

export const getDesignStatus = (state) => {
    return state.preview.content.status
}

export const getTimestamp = (state) => {
    return state.preview.content.timestamp
}

export default reducer
