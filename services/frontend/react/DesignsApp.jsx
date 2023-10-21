import React from 'react'
import ReactDOM from 'react-dom'

import { Provider } from 'react-redux'
import { createStore } from 'redux'

import reducers from './reducers'

import Config from './components/shared/Config'
import Account from './components/shared/Account'
import Designs from './components/designs/Designs'
import DesignsPage from './components/designs/DesignsPage'

import { ThemeProvider, createMuiTheme } from '@mui/material/styles';

const theme = createMuiTheme();

const store = createStore(reducers)

ReactDOM.render(
    <Provider store={store}>
        <Config>
            <ThemeProvider theme={theme}>
                <Account>
                    <Designs>
                        <DesignsPage/>
                    </Designs>
                </Account>
            </ThemeProvider>
        </Config>
    </Provider>,
    document.querySelector('#app')
)
