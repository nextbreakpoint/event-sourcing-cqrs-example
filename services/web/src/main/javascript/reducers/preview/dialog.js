import * as Types from '../../constants/ActionTypes'

const initialState = {
    show_update_design: false
}

function reducer (state = initialState, action) {
  switch (action.type) {
    case Types.SHOW_UPDATE_DESIGN:
      return {
        ...state,
        show_update_design: true
      }
    case Types.HIDE_UPDATE_DESIGN:
      return {
        ...state,
        show_update_design: false
      }
    default:
      return state
  }
}

export const getShowUpdateDesign = (state) => {
    return state.preview.dialog.show_update_design
}

export default reducer
