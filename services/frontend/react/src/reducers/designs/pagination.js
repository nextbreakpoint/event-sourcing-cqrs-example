import * as Types from '../../constants/ActionTypes'

const initialState = {
    pagination: {
        page: 0,
        pageSize: 5
    }
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.DESIGNS_PAGINATION_UPDATE:
      return {
        ...state,
        pagination: action.pagination
      }
    default:
      return state
  }
}

export const getPagination = (state) => {
    return state.designs.pagination.pagination
}

export default reducer
