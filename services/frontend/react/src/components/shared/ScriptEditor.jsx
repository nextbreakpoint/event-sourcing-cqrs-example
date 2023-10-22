import React from 'react'
import PropTypes from 'prop-types'

import { Editor, EditorState, ContentState, CompositeDecorator } from 'draft-js'

const KEYWORD_REGEXP = /fractal|orbit|color|begin|loop|end|rule|trap|palette|if|else|stop|init/g
const FUNCTION_REGEXP = /re|im|mod2|pha|log|exp|sqrt|mod|abs|ceil|floor|pow|hypot|atan2|min|max|cos|sin|tan|asin|acos|atan|time|square|saw|ramp|pulse/g
const PATHOP_REGEXP = /MOVETO|MOVEREL|LINETO|LINEREL|ARCTO|ARCREL|QUADTO|QUADREL|CURVETO|CURVEREL|CLOSE/g
const PARENT_REGEXP = /\(|\)/g
const BRACE_REGEXP = /\{|\}/g
const OPERATOR_REGEXP = /\*|\+|-|\/|\^|<|>|\||&|=|\#|;|\[|\]/g

function keywordStrategy(contentBlock, callback, contentState) {
  findWithRegex(KEYWORD_REGEXP, contentBlock, callback);
}

function functionStrategy(contentBlock, callback, contentState) {
  findWithRegex(FUNCTION_REGEXP, contentBlock, callback);
}

function pathopStrategy(contentBlock, callback, contentState) {
  findWithRegex(PATHOP_REGEXP, contentBlock, callback);
}

function parentStrategy(contentBlock, callback, contentState) {
  findWithRegex(PARENT_REGEXP, contentBlock, callback);
}

function braceStrategy(contentBlock, callback, contentState) {
  findWithRegex(BRACE_REGEXP, contentBlock, callback);
}

function operatorStrategy(contentBlock, callback, contentState) {
  findWithRegex(OPERATOR_REGEXP, contentBlock, callback);
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
  return <span className="keyword">{props.children}</span>
}

const FunctionSpan = (props) => {
  return <span className="function">{props.children}</span>
}

const PathopSpan = (props) => {
  return <span className="pathop">{props.children}</span>
}

const ParentSpan = (props) => {
  return <span className="parent">{props.children}</span>
}

const BraceSpan = (props) => {
  return <span className="brace">{props.children}</span>
}

const OperatorSpan = (props) => {
  return <span className="operator">{props.children}</span>
}

const compositeDecorator = new CompositeDecorator([
  {
    strategy: keywordStrategy,
    component: KeywordSpan
  },
  {
    strategy: functionStrategy,
    component: FunctionSpan
  },
  {
    strategy: pathopStrategy,
    component: PathopSpan
  },
  {
    strategy: parentStrategy,
    component: ParentSpan
  },
  {
    strategy: braceStrategy,
    component: BraceSpan
  },
  {
    strategy: operatorStrategy,
    component: OperatorSpan
  }
])

let ScriptEditor = class ScriptEditor extends React.Component {
    state = {
        editorState: EditorState.createWithContent(ContentState.createFromText(this.props.initialValue), compositeDecorator)
    }

    onChange = (editorState) => {
        this.setState({ editorState })
        if (this.props.onContentChanged) {
            this.props.onContentChanged(editorState.getCurrentContent().getPlainText())
        }
    }

    setEditor = (editor) => {
      this.editor = editor
    }

    focusEditor = () => {
      if (this.editor) {
        this.editor.focus()
      }
    }

    render() {
        return (
          <div className="editor" onClick={this.focusEditor}>
            <Editor ref={this.setEditor} readOnly={this.props.readOnly} editorState={this.state.editorState} onChange={this.onChange}/>
          </div>
        )
  }
}

ScriptEditor.propTypes = {
  initialValue: PropTypes.string.isRequired,
  readOnly: PropTypes.bool.isRequired,
  onContentChanged: PropTypes.func.isRequired
}

export default ScriptEditor
