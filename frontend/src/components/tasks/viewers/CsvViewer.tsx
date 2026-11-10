import React, { useEffect, useMemo, useState } from 'react';
import { TaskFileDetail } from '../../../types';
import { Search } from 'lucide-react';

export interface CsvViewerProps {
  file: TaskFileDetail;
  textContent: string;
}

// Robust fallback CSV parser supporting quotes, escaped quotes, newlines, UTF-8 BOM
export function parseCsvFallback(text: string): string[][] {
  const clean = text.replace(/^\uFEFF/, '');
  const rows: string[][] = [];
  let currentRow: string[] = [];
  let currentCell = '';
  let inQuotes = false;

  for (let i = 0; i < clean.length; i++) {
    const char = clean[i];
    const nextChar = clean[i + 1];

    if (inQuotes) {
      if (char === '"' && nextChar === '"') {
        currentCell += '"';
        i++;
      } else if (char === '"') {
        inQuotes = false;
      } else {
        currentCell += char;
      }
    } else {
      if (char === '"') {
        inQuotes = true;
      } else if (char === ',') {
        currentRow.push(currentCell.trim());
        currentCell = '';
      } else if (char === '\r') {
        if (nextChar === '\n') i++;
        currentRow.push(currentCell.trim());
        if (currentRow.some((c) => c.length > 0)) rows.push(currentRow);
        currentRow = [];
        currentCell = '';
      } else if (char === '\n') {
        currentRow.push(currentCell.trim());
        if (currentRow.some((c) => c.length > 0)) rows.push(currentRow);
        currentRow = [];
        currentCell = '';
      } else {
        currentCell += char;
      }
    }
  }

  if (currentCell.length > 0 || currentRow.length > 0) {
    currentRow.push(currentCell.trim());
    if (currentRow.some((c) => c.length > 0)) rows.push(currentRow);
  }

  return rows;
}

export const CsvViewer: React.FC<CsvViewerProps> = ({ file: _file, textContent }) => {
  const [parsedRows, setParsedRows] = useState<string[][] | null>(null);
  const [searchTerm, setSearchTerm] = useState<string>('');

  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const papaModule: any = await import(
          /* @vite-ignore */ 'https://cdn.jsdelivr.net/npm/papaparse@5.4.1/+esm'
        );
        const Papa = papaModule.default || papaModule;
        const result = Papa.parse(textContent, {
          skipEmptyLines: 'greedy',
          header: false,
          transform: (value: string) => value.replace(/^\uFEFF/, '').trim(),
        });
        if (active && Array.isArray(result.data) && result.data.length > 0) {
          setParsedRows(result.data as string[][]);
          return;
        }
      } catch {
        // Fallback to internal parser
      }
      if (active) {
        setParsedRows(parseCsvFallback(textContent));
      }
    })();

    return () => {
      active = false;
    };
  }, [textContent]);

  const allRows = useMemo(() => parsedRows ?? parseCsvFallback(textContent), [parsedRows, textContent]);
  const headers = useMemo(() => (allRows.length > 0 ? allRows[0] : []), [allRows]);
  const dataRows = useMemo(() => (allRows.length > 1 ? allRows.slice(1) : []), [allRows]);

  const totalDataCount = dataRows.length;
  const isTruncated = totalDataCount > 1000;
  const cappedRows = useMemo(() => dataRows.slice(0, 1000), [dataRows]);

  const filteredRows = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) return cappedRows;
    return cappedRows.filter((row) =>
      row.some((cell) => cell.toLowerCase().includes(term))
    );
  }, [cappedRows, searchTerm]);

  const columnCount = headers.length > 0 ? headers.length : (dataRows[0]?.length || 0);

  return (
    <div className="file-csv-viewer">
      <div className="csv-topbar">
        <div className="csv-search-wrapper">
          <Search size={15} className="csv-search-icon" />
          <input
            type="search"
            className="csv-search-input"
            placeholder="Search rows..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            aria-label="Search CSV rows"
          />
        </div>

        <div className="csv-meta-info">
          <span className="csv-count-badge">
            {searchTerm.trim()
              ? `${filteredRows.length} matching of ${totalDataCount.toLocaleString()} rows`
              : `${totalDataCount.toLocaleString()} rows`}
            {' · '}
            {columnCount} {columnCount === 1 ? 'column' : 'columns'}
          </span>
        </div>
      </div>

      {isTruncated && (
        <div className="csv-truncation-note">
          <span>
            Note: Showing the first 1,000 rows (truncated from {totalDataCount.toLocaleString()} rows). Download the full file to see everything.
          </span>
        </div>
      )}

      <div className="csv-table-scroll-container">
        <table className="csv-real-table">
          <thead>
            <tr>
              <th className="csv-cell-num">#</th>
              {headers.map((h, i) => (
                <th key={i} dir="auto" className="csv-header-cell">
                  {h || `Col ${i + 1}`}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {filteredRows.length > 0 ? (
              filteredRows.map((row, rIndex) => (
                <tr key={rIndex} className="csv-data-row">
                  <td className="csv-cell-num">{rIndex + 1}</td>
                  {row.map((cell, cIndex) => (
                    <td key={cIndex} dir="auto" className="csv-data-cell">
                      {cell}
                    </td>
                  ))}
                  {/* Fill empty cells if row is shorter than header */}
                  {Array.from({ length: Math.max(0, headers.length - row.length) }).map((_, emptyIdx) => (
                    <td key={`empty-${emptyIdx}`} dir="auto" className="csv-data-cell muted">
                      —
                    </td>
                  ))}
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={columnCount + 1} className="csv-empty-search text-center muted">
                  No matching rows found.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
