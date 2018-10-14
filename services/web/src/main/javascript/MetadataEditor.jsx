import React from 'react'
import ReactDOM from 'react-dom'
import PropTypes from 'prop-types'

import { Editor, EditorState, ContentState, CompositeDecorator } from 'draft-js'

const KEYWORD_REGEXP = /translation|rotation|scale|point|julia|options/g

function keywordStrategy(contentBlock, callback, contentState) {
  findWithRegex(KEYWORD_REGEXP, contentBlock, callback);
}

function findWithRegex(regex, contentBlock, callback) {
  const text = contentBlock.getText();
  let matchArr, start;
  while ((matchArr = regex.exec(text)) !== null) {
    start = matchArr.index;
    callback(start, start + matchArr[0].length);
  }
}

const KeywordSpan = (props) => {
  return <span {...props} style={styles.keyword}>{props.children}</span>
}

const compositeDecorator = new CompositeDecorator([
  {
    strategy: keywordStrategy,
    component: KeywordSpan
  }
])

let DesignEditor = class DesignEditor extends React.Component {
  constructor(props) {
    super(props)

    this.state = { editorState: EditorState.createWithContent(ContentState.createFromText(props.initialValue), compositeDecorator) }

    this.onChange = (editorState) => {
        this.setState({ editorState })
        if (props.onContentChanged) {
            props.onContentChanged(editorState.getCurrentContent().getPlainText())
        }
    }

    this.setEditor = (editor) => {
      this.editor = editor
    }

    this.focusEditor = () => {
      if (this.editor) {
        this.editor.focus()
      }
    }
  }

  componentDidMount() {
  }

  render() {
    return (
      <div style={styles.editor} onClick={this.focusEditor}>
        <Editor ref={this.setEditor} readOnly={this.props.readOnly} editorState={this.state.editorState} onChange={this.onChange}/>
      </div>
    )
  }
}

const styles = {
  editor: {
    border: '1px solid gray',
    minHeight: '20em'
  },
  keyword: {
    color: 'blue'
  }
}

DesignEditor.propTypes = {
  initialValue: PropTypes.string,
  readOnly: PropTypes.boolean,
  onContentChanged: PropTypes.func
}

export default DesignEditor
