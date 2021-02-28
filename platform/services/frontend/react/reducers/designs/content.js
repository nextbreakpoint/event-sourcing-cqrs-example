import * as Types from '../../constants/ActionTypes'

const initialState = {
    data: undefined,
    timestamp: 0,
    status: {
        message: "Loading designs...",
        error: false
    }
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.DESIGNS_LOAD:
      return {
        ...state,
        status: {
            message: "Loading designs...",
            error: false
        }
      }
    case Types.DESIGNS_LOAD_SUCCESS:
      return {
        ...state,
        status: {
            message: "Designs loaded",
            error: false
        },
        data: action.designs,
        timestamp: action.timestamp
      }
    case Types.DESIGNS_LOAD_FAILURE:
      return {
        ...state,
        status: {
            message: action.error,
            error: true
        },
        data: [],
        timestamp: 0
      }
    default:
      return state
  }
}

export const getDesigns = (state) => {
    return state.designs.content.data
}

export const getDesignsStatus = (state) => {
    return state.designs.content.status
}

export const getTimestamp = (state) => {
    return state.designs.content.timestamp
}

export default reducer