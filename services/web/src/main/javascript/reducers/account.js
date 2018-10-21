import * as Types from '../constants/ActionTypes'

const initialState = {
    data: undefined,
    status: {
        message: "Loading account...",
        error: false
    }
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.ACCOUNT_LOAD:
      return {
        ...state,
        status: {
            message: "Loading account...",
            error: false
        },
        data: undefined
      }
    case Types.ACCOUNT_LOAD_SUCCESS:
      return {
        ...state,
        status: {
            message: "Config loaded",
            error: false
        },
        data: action.account
      }
    case Types.ACCOUNT_LOAD_FAILURE:
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

export const getAccount = (state) => {
    return state.account.data
}

export const getAccountStatus = (state) => {
    return state.account.status
}

export default reducer
