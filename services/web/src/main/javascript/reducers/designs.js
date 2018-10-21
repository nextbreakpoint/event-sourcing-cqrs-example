import * as Types from '../constants/ActionTypes'

import { combineReducers } from 'redux'

import sorting from './designs/sorting'
import selection from './designs/selection'
import pagination from './designs/pagination'

const initialState = {
    data: undefined,
    timestamp: 0,
    status: {
        message: "Loading designs...",
        error: false
    },
    show_create_design: false,
    show_delete_designs: false
}

function content (state = initialState, action) {
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
        timestamp: action.timestamp
      }
    case Types.DESIGNS_LOAD_FAILURE:
      return {
        ...state,
        status: {
            message: action.error,
            error: true
        },
        data: [],
        timestamp: 0
      }
    case Types.SHOW_CREATE_DESIGN:
      return {
        ...state,
        show_create_design: true
      }
    case Types.HIDE_CREATE_DESIGN:
      return {
        ...state,
        show_create_design: false
      }
    case Types.SHOW_DELETE_DESIGNS:
      return {
        ...state,
        show_delete_designs: true
      }
    case Types.HIDE_DELETE_DESIGNS:
      return {
        ...state,
        show_delete_designs: false
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

export const getTimestamp = (state) => {
    return state.designs.content.timestamp
}

export const getShowCreateDesign = (state) => {
    return state.designs.content.show_create_design
}

export const getShowDeleteDesigns = (state) => {
    return state.designs.content.show_delete_designs
}

export default combineReducers({
    content,
    sorting,
    selection,
    pagination
})
