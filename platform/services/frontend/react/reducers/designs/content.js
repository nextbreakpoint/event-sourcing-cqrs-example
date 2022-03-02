import * as Types from '../../constants/ActionTypes'

const initialState = {
    data: undefined,
    revision: "0000000000000000-0000000000000000",
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
        revision: action.revision
      }
    case Types.DESIGNS_LOAD_FAILURE:
      return {
        ...state,
        status: {
            message: action.error,
            error: true
        },
        data: [],
        revision: "0000000000000000-0000000000000000"
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

export const getRevision = (state) => {
    return state.designs.content.revision
}

export default reducer