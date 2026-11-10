import React, { useMemo, useState } from 'react';
import { Button } from '../../common/Button';
import { TaskFileDetail } from '../../../types';
import { WrapText, CheckCircle2, AlertCircle, Sparkles } from 'lucide-react';
import { formatSize, getFileExtension } from '../fileUtils';

export interface CodeIdeViewerProps {
  file: TaskFileDetail;
  textContent: string;
}

// Maps extension to human language name
const EXTENSION_NAMES: Record<string, string> = {
  json: 'JSON',
  xml: 'XML',
  yaml: 'YAML',
  yml: 'YAML',
  sql: 'SQL',
  java: 'Java',
  py: 'Python',
  js: 'JavaScript',
  ts: 'TypeScript',
  md: 'Markdown',
  markdown: 'Markdown',
  tex: 'LaTeX Source',
};

// Syntax tokenization for code and markdown
function highlightTokens(line: string, ext: string): React.ReactNode[] {
  if (!line) return ['\u00a0'];

  // Markdown syntax highlighting
  if (ext === 'md' || ext === 'markdown') {
    if (/^#{1,6}\s/.test(line)) {
      return [<span key="h" className="syn-keyword" style={{ fontWeight: 'bold' }}>{line}</span>];
    }
    if (line.trim().startsWith('>')) {
      return [<span key="q" className="syn-comment">{line}</span>];
    }
    if (/^[-*+]\s+/.test(line) || /^\d+\.\s+/.test(line)) {
      return [<span key="l" className="syn-number">{line}</span>];
    }
    if (line.startsWith('```')) {
      return [<span key="cb" className="syn-comment">{line}</span>];
    }
    // Inline markdown formatting tokens
    const mdRegex = /(`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*|\[[^\]]+\]\([^)]+\))/g;
    const parts: React.ReactNode[] = [];
    let last = 0;
    let match: RegExpExecArray | null;
    while ((match = mdRegex.exec(line)) !== null) {
      if (match.index > last) {
        parts.push(line.slice(last, match.index));
      }
      const token = match[0];
      if (token.startsWith('`')) {
        parts.push(<span key={match.index} className="syn-string">{token}</span>);
      } else if (token.startsWith('[')) {
        parts.push(<span key={match.index} className="syn-keyword">{token}</span>);
      } else {
        parts.push(<span key={match.index} className="syn-keyword" style={{ fontWeight: 'bold' }}>{token}</span>);
      }
      last = match.index + token.length;
    }
    if (last < line.length) {
      parts.push(line.slice(last));
    }
    return parts.length > 0 ? parts : [line];
  }

  // Comments for code types
  if ((ext === 'java' || ext === 'js' || ext === 'ts') && line.trim().startsWith('//')) {
    return [<span key="c" className="syn-comment">{line}</span>];
  }
  if ((ext === 'py' || ext === 'yaml' || ext === 'yml') && line.trim().startsWith('#')) {
    return [<span key="c" className="syn-comment">{line}</span>];
  }
  if (ext === 'sql' && line.trim().startsWith('--')) {
    return [<span key="c" className="syn-comment">{line}</span>];
  }
  if (ext === 'xml' && line.trim().startsWith('<!--')) {
    return [<span key="c" className="syn-comment">{line}</span>];
  }

  // Token regex for strings, numbers, keywords
  const regex = /(".*?"|'.*?'|<!--.*?-->|\b(const|let|var|function|return|if|else|for|while|import|export|from|class|public|private|protected|static|void|int|new|package|def|elif|select|from|where|insert|into|values|update|delete|table|primary|key|true|false|null|boolean|string|number)\b|\b\d+(\.\d+)?\b)/gi;
  const parts: React.ReactNode[] = [];
  let lastIdx = 0;
  let match: RegExpExecArray | null;

  while ((match = regex.exec(line)) !== null) {
    if (match.index > lastIdx) {
      parts.push(line.slice(lastIdx, match.index));
    }
    const token = match[0];
    if (token.startsWith('"') || token.startsWith("'")) {
      parts.push(<span key={match.index} className="syn-string">{token}</span>);
    } else if (/^\d/.test(token)) {
      parts.push(<span key={match.index} className="syn-number">{token}</span>);
    } else {
      parts.push(<span key={match.index} className="syn-keyword">{token}</span>);
    }
    lastIdx = match.index + token.length;
  }

  if (lastIdx < line.length) {
    parts.push(line.slice(lastIdx));
  }

  return parts.length > 0 ? parts : [line];
}

export const CodeIdeViewer: React.FC<CodeIdeViewerProps> = ({ file, textContent }) => {
  const ext = getFileExtension(file.originalName);
  const languageName = EXTENSION_NAMES[ext] || 'Source Code';
  const isJson = ext === 'json' || file.contentType === 'application/json';

  const [prettyPrint, setPrettyPrint] = useState<boolean>(false);
  const [wrap, setWrap] = useState<boolean>(false);

  // JSON validation & pretty printing
  const jsonMeta = useMemo(() => {
    if (!isJson) return null;
    try {
      const parsed = JSON.parse(textContent);
      return { valid: true, error: null, formatted: JSON.stringify(parsed, null, 2) };
    } catch (err) {
      return {
        valid: false,
        error: err instanceof Error ? err.message : 'Invalid JSON syntax',
        formatted: textContent,
      };
    }
  }, [isJson, textContent]);

  const activeContent = useMemo(() => {
    if (isJson && prettyPrint && jsonMeta?.valid) {
      return jsonMeta.formatted;
    }
    return textContent;
  }, [isJson, prettyPrint, jsonMeta, textContent]);

  const lines = useMemo(() => activeContent.split(/\r?\n/), [activeContent]);

  return (
    <div className="file-ide-viewer">
      <div className="ide-topbar">
        <div className="ide-meta-group">
          <span className="ide-lang-badge">{languageName}</span>
          <span className="ide-info muted">
            {lines.length.toLocaleString()} lines · {formatSize(file.sizeBytes || activeContent.length)}
          </span>

          {/* JSON validity indicator */}
          {isJson && jsonMeta && (
            <span
              className={`ide-validity-badge ${jsonMeta.valid ? 'is-valid' : 'is-invalid'}`}
              title={jsonMeta.valid ? 'Valid JSON syntax' : jsonMeta.error || 'Invalid JSON'}
            >
              {jsonMeta.valid ? (
                <>
                  <CheckCircle2 size={14} />
                  <span>Valid JSON</span>
                </>
              ) : (
                <>
                  <AlertCircle size={14} />
                  <span>Invalid JSON</span>
                </>
              )}
            </span>
          )}
        </div>

        <div className="ide-controls-group">
          {/* JSON Pretty-print toggle */}
          {isJson && jsonMeta?.valid && (
            <Button
              type="button"
              size="sm"
              variant={prettyPrint ? 'primary' : 'outline'}
              icon={<Sparkles size={14} />}
              onClick={() => setPrettyPrint((p) => !p)}
              title={prettyPrint ? 'Show original JSON' : 'Pretty-print formatted JSON'}
              aria-label="Toggle JSON pretty-print"
            >
              {prettyPrint ? 'Original' : 'Pretty-print'}
            </Button>
          )}

          <Button
            type="button"
            size="sm"
            variant="outline"
            icon={<WrapText size={14} />}
            onClick={() => setWrap((w) => !w)}
            title={wrap ? 'No wrap' : 'Wrap lines'}
            aria-label={wrap ? 'Disable line wrap' : 'Enable line wrap'}
          >
            {wrap ? 'No wrap' : 'Wrap lines'}
          </Button>
        </div>
      </div>

      <div className={`ide-editor-container ${wrap ? 'is-wrapped' : 'no-wrap'}`}>
        <div className="ide-code-gutter" aria-hidden="true">
          {lines.map((_, idx) => (
            <div key={idx} className="ide-line-number">
              {idx + 1}
            </div>
          ))}
        </div>
        <div className="ide-code-surface">
          {lines.map((line, idx) => (
            <div key={idx} className="ide-code-line" dir="auto">
              <span className="ide-line-content" dir="auto">
                {highlightTokens(line, ext)}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
