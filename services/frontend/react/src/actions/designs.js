import * as Types from '../constants/ActionTypes'

import * as content from '../reducers/designs/content'
import * as dialog from '../reducers/designs/dialog'
import * as sorting from '../reducers/designs/sorting'
import * as selection from '../reducers/designs/selection'
import * as pagination from '../reducers/designs/pagination'

const default_script = "fractal {\n\torbit [-2.0 - 2.0i,+2.0 + 2.0i] [x,n] {\n\t\tloop [0, 200] (mod2(x) > 40) {\n\t\t\tx = x * x + w;\n\t\t}\n\t}\n\tcolor [#FF000000] {\n\t\tpalette gradient {\n\t\t\t[#FFFFFFFF > #FF000000, 100];\n\t\t\t[#FF000000 > #FFFFFFFF, 100];\n\t\t}\n\t\tinit {\n\t\t\tm = 100 * (1 + sin(mod(x) * 0.2 / pi));\n\t\t}\n\t\trule (n > 0) [1] {\n\t\t\tgradient[m - 1]\n\t\t}\n\t}\n}\n"
const default_metadata = "{\n\t\"translation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":1.0,\n\t\t\"w\":0.0\n\t},\n\t\"rotation\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0,\n\t\t\"z\":0.0,\n\t\t\"w\":0.0\n\t},\n\t\"scale\":\n\t{\n\t\t\"x\":1.0,\n\t\t\"y\":1.0,\n\t\t\"z\":1.0,\n\t\t\"w\":1.0\n\t},\n\t\"point\":\n\t{\n\t\t\"x\":0.0,\n\t\t\"y\":0.0\n\t},\n\t\"julia\":false,\n\t\"options\":\n\t{\n\t\t\"showPreview\":false,\n\t\t\"showTraps\":false,\n\t\t\"showOrbit\":false,\n\t\t\"showPoint\":false,\n\t\t\"previewOrigin\":\n\t\t{\n\t\t\t\"x\":0.0,\n\t\t\t\"y\":0.0\n\t\t},\n\t\t\"previewSize\":\n\t\t{\n\t\t\t\"x\":0.25,\n\t\t\t\"y\":0.25\n\t\t}\n\t}\n}"
const default_manifest = "{\"pluginId\":\"Mandelbrot\"}"

export const loadDesigns = () => ({
  type: Types.DESIGNS_LOAD
})

export const loadDesignsSuccess = (designs, total, revision) => ({
  type: Types.DESIGNS_LOAD_SUCCESS, designs, total, revision
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

export const showUpdateDesign = () => ({
  type: Types.SHOW_UPDATE_DESIGN
})

export const hideUpdateDesign = () => ({
  type: Types.HIDE_UPDATE_DESIGN
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

export const setDesignsSorting = (sorting) => ({
  type: Types.DESIGNS_SORTING_UPDATE, sorting
})

export const setDesignsSelection = (selection) => ({
  type: Types.DESIGNS_SELECTION_UPDATE, selection
})

export const setDesignsPagination = (pagination) => ({
  type: Types.DESIGNS_PAGINATION_UPDATE, pagination
})

export const setSelectedDesign = (design) => ({
  type: Types.SELECTED_DESIGN_CHANGED, present: true, design: design
})

export const resetSelectedDesign = () => ({
  type: Types.SELECTED_DESIGN_CHANGED, present: false, design: {
        manifest: default_manifest,
        metadata: default_metadata,
        script: default_script
  }
})

export const getTotal = (state) => {
    return content.getTotal(state)
}

export const getDesigns = (state) => {
    return content.getDesigns(state)
}

export const getDesignsStatus = (state) => {
    return content.getDesignsStatus(state)
}

export const getRevision = (state) => {
    return content.getRevision(state)
}

export const getShowUploadDesign = (state) => {
    return dialog.getShowUploadDesign(state)
}

export const getShowCreateDesign = (state) => {
    return dialog.getShowCreateDesign(state)
}

export const getShowUpdateDesign = (state) => {
    return dialog.getShowUpdateDesign(state)
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

export const getSelectedDesign = (state) => {
    return dialog.getSelectedDesign(state)
}

export const getSorting = (state) => {
    return sorting.getSorting(state)
}

export const getSelection = (state) => {
    return selection.getSelection(state)
}

export const getPagination = (state) => {
    return pagination.getPagination(state)
}

