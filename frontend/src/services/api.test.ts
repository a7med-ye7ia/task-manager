import { describe, expect, it, vi } from 'vitest';
import { getText, request } from './api';

describe('file response parsing', () => {
  it('keeps plain text responses as text', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('a,b\n1,2', { headers: { 'content-type': 'text/plain;charset=UTF-8' } })));
    await expect(getText('/api/tasks/1/files/2/text')).resolves.toBe('a,b\n1,2');
  });
  it('parses JSON responses only when the response is JSON', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"ok":true}', { headers: { 'content-type': 'application/json' } })));
    await expect(request<{ ok: boolean }>('/api/example')).resolves.toEqual({ ok: true });
  });
});
