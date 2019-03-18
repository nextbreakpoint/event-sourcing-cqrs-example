import * as Types from '../../constants/ActionTypes'

const initialState = {
    data: []
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.DESIGNS_SELECTION_UPDATE:
      return {
        ...state,
        data: action.selected
      }
    default:
      return state
  }
}

export const getSelected = (state) => {
    return state.designs.selection.data
}

export default reducer
