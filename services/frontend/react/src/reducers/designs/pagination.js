import * as Types from '../../constants/ActionTypes'

const initialState = {
    page: 0,
    rowsPerPage: 5
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.DESIGNS_PAGINATION_UPDATE:
      return {
        ...state,
        page: action.page,
        rowsPerPage: action.rowsPerPage
      }
    default:
      return state
  }
}

export const getPage = (state) => {
    return state.designs.pagination.page
}

export const getRowsPerPage = (state) => {
    return state.designs.pagination.rowsPerPage
}

export default reducer
