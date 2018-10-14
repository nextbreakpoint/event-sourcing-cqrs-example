import { SET_CONFIG, SET_ACCOUNT, SET_DESIGNS, SHOW_CREATE_DESIGN, HIDE_CREATE_DESIGN, SET_PAGE, SET_ROWS_PER_PAGE } from '../constants/ActionTypes'

export const setConfig = (config) => ({
  type: SET_CONFIG,
  config: config
})

export const setAccount = (account) => ({
  type: SET_ACCOUNT,
  account: account
})

export const setDesigns = (designs, timestamp) => ({
  type: SET_DESIGNS,
  designs: designs,
  timestamp: timestamp
})

export const showCreateDesign = () => ({
  type: SHOW_CREATE_DESIGN
})

export const hideCreateDesign = () => ({
  type: HIDE_CREATE_DESIGN
})

export const setPage = (page) => ({
  type: SET_PAGE,
  page: page
})

export const setRowsPerPage = (rowsPerPage) => ({
  type: SET_ROWS_PER_PAGE,
  rowsPerPage: rowsPerPage
})
