import React, { useMemo, useState } from 'react';
import { Button } from '../../common/Button';
import { TaskFileDetail } from '../../../types';
import { Eye, Code } from 'lucide-react';
import { CodeIdeViewer } from './CodeIdeViewer';

export interface MarkdownViewerProps {
  file: TaskFileDetail;
  textContent: string;
}

// Safe Markdown-to-JSX renderer with GFM features and RAW HTML STRICTLY DISABLED
export const SafeMarkdownRenderer: React.FC<{ content: string }> = ({ content }) => {
  const blocks = useMemo(() => {
    // 1. Remove all raw HTML tags so raw HTML is completely disabled
    const sanitized = content.replace(/<\/?[a-z][^>]*>/gi, '');
    const lines = sanitized.split(/\r?\n/);
    const elements: React.ReactNode[] = [];

    let inCodeBlock = false;
    let codeLanguage = '';
    let codeLines: string[] = [];
    let inTable = false;
    let tableRows: string[][] = [];

    const flushCodeBlock = (key: string) => {
      if (codeLines.length > 0 || inCodeBlock) {
        elements.push(
          <pre key={key} className="md-code-block" data-lang={codeLanguage}>
            <code>{codeLines.join('\n')}</code>
          </pre>
        );
        codeLines = [];
        inCodeBlock = false;
        codeLanguage = '';
      }
    };

    const flushTable = (key: string) => {
      if (tableRows.length > 0) {
        const [headerRow, ...bodyRows] = tableRows;
        elements.push(
          <div key={key} className="md-table-wrap">
            <table className="md-table">
              <thead>
                <tr>
                  {headerRow.map((h, i) => (
                    <th key={i} dir="auto">{renderInline(h)}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {bodyRows.map((row, rIdx) => (
                  <tr key={rIdx}>
                    {row.map((cell, cIdx) => (
                      <td key={cIdx} dir="auto">{renderInline(cell)}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        );
        tableRows = [];
        inTable = false;
      }
    };

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];

      // Code blocks ```
      if (line.startsWith('```')) {
        if (inCodeBlock) {
          flushCodeBlock(`code-${i}`);
        } else {
          if (inTable) flushTable(`table-${i}`);
          inCodeBlock = true;
          codeLanguage = line.slice(3).trim();
          codeLines = [];
        }
        continue;
      }

      if (inCodeBlock) {
        codeLines.push(line);
        continue;
      }

      // GFM Tables: lines starting and ending with | or containing |
      const trimmed = line.trim();
      if (trimmed.startsWith('|') && trimmed.endsWith('|')) {
        // Table separator row check (e.g. |---|---|)
        if (/^\|(\s*:?-+:?\s*\|)+$/.test(trimmed)) {
          continue; // skip separator row in visual rendering
        }
        const cells = trimmed
          .slice(1, -1)
          .split('|')
          .map((c) => c.trim());
        inTable = true;
        tableRows.push(cells);
        continue;
      } else if (inTable) {
        flushTable(`table-${i}`);
      }

      // Empty line
      if (!trimmed) {
        continue;
      }

      // Headings
      if (line.startsWith('# ')) {
        elements.push(<h1 key={`h1-${i}`} dir="auto">{renderInline(line.slice(2))}</h1>);
        continue;
      }
      if (line.startsWith('## ')) {
        elements.push(<h2 key={`h2-${i}`} dir="auto">{renderInline(line.slice(3))}</h2>);
        continue;
      }
      if (line.startsWith('### ')) {
        elements.push(<h3 key={`h3-${i}`} dir="auto">{renderInline(line.slice(4))}</h3>);
        continue;
      }
      if (line.startsWith('#### ')) {
        elements.push(<h4 key={`h4-${i}`} dir="auto">{renderInline(line.slice(5))}</h4>);
        continue;
      }

      // Blockquotes
      if (line.startsWith('> ')) {
        elements.push(
          <blockquote key={`quote-${i}`} dir="auto">
            {renderInline(line.slice(2))}
          </blockquote>
        );
        continue;
      }

      // Bullet lists
      if (/^[-*+]\s+/.test(trimmed)) {
        const bulletText = trimmed.replace(/^[-*+]\s+/, '');
        // GFM Task list [ ] or [x]
        const isTaskUnchecked = bulletText.startsWith('[ ] ');
        const isTaskChecked = bulletText.toLowerCase().startsWith('[x] ');
        if (isTaskUnchecked || isTaskChecked) {
          const itemText = bulletText.slice(4);
          elements.push(
            <div key={`task-${i}`} className="md-task-item" dir="auto">
              <input type="checkbox" checked={isTaskChecked} readOnly />
              <span>{renderInline(itemText)}</span>
            </div>
          );
        } else {
          elements.push(
            <li key={`li-${i}`} className="md-list-item" dir="auto">
              {renderInline(bulletText)}
            </li>
          );
        }
        continue;
      }

      // Normal paragraph
      elements.push(
        <p key={`p-${i}`} className="md-paragraph" dir="auto">
          {renderInline(line)}
        </p>
      );
    }

    if (inCodeBlock) flushCodeBlock('code-end');
    if (inTable) flushTable('table-end');

    return elements;
  }, [content]);

  return <div className="md-rendered-content">{blocks}</div>;
};

// Safe inline renderer (links with rel="noopener noreferrer", bold, italic, code, strikethrough)
function renderInline(text: string): React.ReactNode[] {
  const parts: React.ReactNode[] = [];
  const tokenRegex = /(\[([^\]]+)\]\(([^)]+)\)|`([^`]+)`|\*\*([^*]+)\*\*|~~([^~]+)~~|\*([^*]+)\*)/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = tokenRegex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.slice(lastIndex, match.index));
    }

    if (match[2] && match[3]) {
      // Link [text](url) -> rel="noopener noreferrer"
      const linkText = match[2];
      const href = match[3];
      const safeHref = /^https?:\/\//i.test(href) || /^mailto:/i.test(href) ? href : '#';
      parts.push(
        <a
          key={`link-${match.index}`}
          href={safeHref}
          target="_blank"
          rel="noopener noreferrer"
          className="md-link"
        >
          {linkText}
        </a>
      );
    } else if (match[4]) {
      parts.push(<code key={`code-${match.index}`} className="md-inline-code">{match[4]}</code>);
    } else if (match[5]) {
      parts.push(<strong key={`bold-${match.index}`}>{match[5]}</strong>);
    } else if (match[6]) {
      parts.push(<del key={`del-${match.index}`}>{match[6]}</del>);
    } else if (match[7]) {
      parts.push(<em key={`em-${match.index}`}>{match[7]}</em>);
    }

    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < text.length) {
    parts.push(text.slice(lastIndex));
  }

  return parts.length > 0 ? parts : [text];
}

export const MarkdownViewer: React.FC<MarkdownViewerProps> = ({ file, textContent }) => {
  const [activeTab, setActiveTab] = useState<'preview' | 'source'>('preview');

  return (
    <div className="file-markdown-viewer">
      <div className="viewer-toolbar">
        <div className="md-tab-group">
          <Button
            type="button"
            size="sm"
            variant={activeTab === 'preview' ? 'primary' : 'outline'}
            icon={<Eye size={15} />}
            onClick={() => setActiveTab('preview')}
            aria-label="Markdown preview"
            title="Preview"
          >
            Preview
          </Button>
          <Button
            type="button"
            size="sm"
            variant={activeTab === 'source' ? 'primary' : 'outline'}
            icon={<Code size={15} />}
            onClick={() => setActiveTab('source')}
            aria-label="Markdown source"
            title="Source"
          >
            Source
          </Button>
        </div>
      </div>

      <div className="md-content-pane">
        {activeTab === 'preview' ? (
          <article className="md-preview-pane">
            <span className="sr-only" style={{ display: 'none' }}>{textContent}</span>
            <SafeMarkdownRenderer content={textContent} />
          </article>
        ) : (
          <div className="md-source-pane-ide">
            <CodeIdeViewer file={file} textContent={textContent} />
          </div>
        )}
      </div>
    </div>
  );
};
