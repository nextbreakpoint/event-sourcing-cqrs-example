import React from 'react'

import Grid from '@mui/material/Grid'

export default function Message({text, error}) {
    return (
        <Grid container justify="center" className={error ? 'message-error' : 'message-normal'}>
            <Grid item xs={12}>{text}</Grid>
        </Grid>
    )
}
