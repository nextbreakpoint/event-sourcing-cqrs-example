import { SET_CONFIG, SET_ACCOUNT, SET_DESIGNS, SET_PAGE, SET_ROWS_PER_PAGE, SHOW_CREATE_DESIGN, HIDE_CREATE_DESIGN } from '../constants/ActionTypes'

const initialState = {
    config: undefined,
    account: undefined,
    role: "anonymous",
    name: "Guest",
    designs: [],
    timestamp: 0,
    show_create_design: false,
    order: 'asc',
    orderBy: 'uuid',
    selected: [],
    page: 0,
    rowsPerPage: 5
}

function designsReducer (state = initialState, action) {
  switch (action.type) {
    case SHOW_CREATE_DESIGN:
      return {
        ...state,
        show_create_design: true
      }
    case HIDE_CREATE_DESIGN:
      return {
        ...state,
        show_create_design: false
      }
    case SET_CONFIG:
      return {
        ...state,
        config: action.config
      }
    case SET_ACCOUNT:
      return {
        ...state,
        account: action.account,
        role: action.account.role,
        name: action.account.name
      }
    case SET_DESIGNS:
      return {
        ...state,
        designs: action.designs,
        timestamp: action.timestamp
      }
    case SET_PAGE:
      return {
        ...state,
        page: action.page
      }
    case SET_ROWS_PER_PAGE:
      return {
        ...state,
        rowsPerPage: action.rowsPerPage
      }
    default:
      return state
  }
}

export default designsReducer
