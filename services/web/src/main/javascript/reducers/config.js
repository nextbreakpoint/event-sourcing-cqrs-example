import * as Types from '../constants/ActionTypes'

const initialState = {
    data: undefined,
    status: {
        message: "Loading config...",
        error: false
    }
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.CONFIG_LOAD:
      return {
        ...state,
        status: {
            message: "Loading config...",
            error: false
        },
        data: undefined
      }
    case Types.CONFIG_LOAD_SUCCESS:
      return {
        ...state,
        status: {
            message: "Config loaded",
            error: false
        },
        data: action.config
      }
    case Types.CONFIG_LOAD_FAILURE:
      return {
        ...state,
        status: {
            message: action.error,
            error: true
        },
        data: undefined
      }
    default:
      return state
  }
}

export const getConfig = (state) => {
    return state.config.data
}

export const getConfigStatus = (state) => {
    return state.config.status
}

export default reducer
