import * as Types from '../../constants/ActionTypes'

const initialState = {
    data: undefined,
    revision: "0000000000000000-0000000000000000",
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
        revision: action.revision
      }
    case Types.DESIGN_LOAD_FAILURE:
      return {
        ...state,
        status: {
            message: action.error,
            error: true
        },
        data: undefined,
        revision: "0000000000000000-0000000000000000"
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

export const getRevision = (state) => {
    return state.preview.content.revision
}

export default reducer
