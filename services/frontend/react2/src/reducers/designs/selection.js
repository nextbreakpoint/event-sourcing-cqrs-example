import * as Types from '../../constants/ActionTypes'

const initialState = {
    selection: []
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.DESIGNS_SELECTION_UPDATE:
      return {
        ...state,
        selection: action.selection
      }
    default:
      return state
  }
}

export const getSelection = (state) => {
    return state.designs.selection.selection
}

export default reducer
