import * as Types from '../constants/ActionTypes'

import * as content from '../reducers/designs/content'
import * as dialog from '../reducers/designs/dialog'
import * as sorting from '../reducers/designs/sorting'
import * as selection from '../reducers/designs/selection'
import * as pagination from '../reducers/designs/pagination'

export const loadDesigns = () => ({
  type: Types.DESIGNS_LOAD
})

export const loadDesignsSuccess = (designs, revision) => ({
  type: Types.DESIGNS_LOAD_SUCCESS, designs, revision
})

export const loadDesignsFailure = (error) => ({
  type: Types.DESIGNS_LOAD_FAILURE, error
})

export const showCreateDesign = () => ({
  type: Types.SHOW_CREATE_DESIGN
})

export const hideCreateDesign = () => ({
  type: Types.HIDE_CREATE_DESIGN
})

export const showDeleteDesigns = () => ({
  type: Types.SHOW_DELETE_DESIGNS
})

export const hideDeleteDesigns = () => ({
  type: Types.HIDE_DELETE_DESIGNS
})

export const showErrorMessage = (error) => ({
  type: Types.SHOW_ERROR_MESSAGE, error
})

export const hideErrorMessage = () => ({
  type: Types.HIDE_ERROR_MESSAGE
})

export const setDesignsSorting = (order, orderBy) => ({
  type: Types.DESIGNS_SORTING_UPDATE, order, orderBy
})

export const setDesignsSelection = (selected) => ({
  type: Types.DESIGNS_SELECTION_UPDATE, selected
})

export const setDesignsPagination = (page, rowsPerPage) => ({
  type: Types.DESIGNS_PAGINATION_UPDATE, page, rowsPerPage
})

export const getDesigns = (state) => {
    return content.getDesigns(state)
}

export const getDesignsStatus = (state) => {
    return content.getDesignsStatus(state)
}

export const getRevision = (state) => {
    return content.getRevision(state)
}

export const getShowCreateDesign = (state) => {
    return dialog.getShowCreateDesign(state)
}

export const getShowDeleteDesigns = (state) => {
    return dialog.getShowDeleteDesigns(state)
}

export const getShowErrorMessage = (state) => {
    return dialog.getShowErrorMessage(state)
}

export const getErrorMessage = (state) => {
    return dialog.getErrorMessage(state)
}

export const getSelected = (state) => {
    return selection.getSelected(state)
}

export const getOrder = (state) => {
    return sorting.getOrder(state)
}

export const getOrderBy = (state) => {
    return sorting.getOrderBy(state)
}

export const getPage = (state) => {
    return pagination.getPage(state)
}

export const getRowsPerPage = (state) => {
    return pagination.getRowsPerPage(state)
}
