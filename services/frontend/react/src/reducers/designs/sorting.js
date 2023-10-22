import * as Types from '../../constants/ActionTypes'

const initialState = {
    order: 'desc',
    orderBy: 'modified'
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.DESIGNS_SORTING_UPDATE:
      return {
        ...state,
        order: action.order,
        orderBy: action.orderBy
      }
    default:
      return state
  }
}

export const getOrder = (state) => {
    return state.designs.sorting.order
}

export const getOrderBy = (state) => {
    return state.designs.sorting.orderBy
}

export default reducer
