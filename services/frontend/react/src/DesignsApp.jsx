import React from 'react'
import { createRoot } from 'react-dom/client';

import { Provider } from 'react-redux'
import { createStore } from 'redux'

import reducers from './reducers'

import Config from './components/shared/Config'
import Account from './components/shared/Account'
import Designs from './components/designs/Designs'
import DesignsPage from './components/designs/DesignsPage'

import { ThemeProvider, createTheme } from '@mui/material/styles';

const theme = createTheme();

const store = createStore(reducers)

const root = createRoot(document.getElementById('app'));

root.render(
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
    </Provider>
)
