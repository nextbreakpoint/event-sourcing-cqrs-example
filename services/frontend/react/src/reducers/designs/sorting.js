import * as Types from '../../constants/ActionTypes'

const initialState = {
    sorting: [{
        field: 'created',
        sort: 'desc'
    }]
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.DESIGNS_SORTING_UPDATE:
      return {
        ...state,
        sorting: action.sorting
      }
    default:
      return state
  }
}

export const getSorting = (state) => {
    return state.designs.sorting.sorting
}

export default reducer
