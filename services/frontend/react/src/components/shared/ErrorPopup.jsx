import React from 'react'

import Snackbar from '@mui/material/Snackbar'
import IconButton from '@mui/material/IconButton'
import CloseIcon from '@mui/icons-material/Close'

export default function ErrorPopup({ showErrorMessage, errorMessage, onPopupClose }) {
    const onClose = (event, reason) => {
        if (reason === 'clickaway') {
          return
        }

        onPopupClose()
    }

    return (
        <Snackbar
          anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
          open={showErrorMessage}
          autoHideDuration={6000}
          onClose={onClose}
          message={errorMessage}
          action={[
            <IconButton key="close" aria-label="Close" color="inherit" onClick={onClose}>
              <CloseIcon />
            </IconButton>
          ]}
        />
    )
}
