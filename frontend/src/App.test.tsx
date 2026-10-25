import { render, screen, within, waitFor, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import App from './App';
import { setAccessToken } from './services/api';

const jsonResponse = (body: unknown, status = 200) => ({
  ok: status >= 200 && status < 300,
  status,
  text: () => Promise.resolve(JSON.stringify(body)),
  json: () => Promise.resolve(body),
});

describe('App Shell & Phase 1 Screens', () => {
  beforeEach(() => {
    localStorage.clear();
    setAccessToken(null);
    global.fetch = vi.fn().mockImplementation(() => {
      return Promise.resolve(jsonResponse({}));
    });
  });

  it('redirects a signed-out visitor at root to sign in with a notice', async () => {
    render(<App />);

    expect(screen.getByText('Task Manager')).toBeInTheDocument();
    expect(await screen.findByText('Sign in to continue.')).toBeInTheDocument();
    const header = within(screen.getByRole('banner'));
    expect(header.getByRole('link', { name: /^Sign In$/i })).toBeInTheDocument();
    expect(header.getByRole('link', { name: /^Sign Up$/i })).toBeInTheDocument();
    expect(header.queryByRole('link', { name: /^Home$/i })).not.toBeInTheDocument();
  });

  it('shows the signed-out header with only public actions', async () => {
    render(<App />);

    expect(screen.getByText('Task Manager')).toBeInTheDocument();
    const header = within(screen.getByRole('banner'));
    expect(header.getByRole('link', { name: /^Sign In$/i })).toBeInTheDocument();
    expect(header.getByRole('link', { name: /^Sign Up$/i })).toBeInTheDocument();
    expect(header.queryByRole('link', { name: /^Home$/i })).not.toBeInTheDocument();
  });

  it('shows the home page for a signed-in user at root', async () => {
    setAccessToken('test-token');
    global.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/profile')) return Promise.resolve(jsonResponse({ id: 1, firstName: 'Test', lastName: 'User', email: 'test@example.com', phone: '1', role: 'EMPLOYEE', status: 'ACTIVE' }));
      if (url.includes('/api/tasks/pool')) return Promise.resolve(jsonResponse({ content: [], totalElements: 0, totalPages: 1 }));
      if (url.includes('/api/mentions/unread')) return Promise.resolve(jsonResponse({ count: 0 }));
      if (url.includes('/api/notifications')) return Promise.resolve(jsonResponse([]));
      return Promise.resolve(jsonResponse({ status: 'UP', timestamp: '', timezone: 'Africa/Cairo', database: 'CONNECTED', seedManagerConfigured: true }));
    });
    render(<App />);
    expect(await screen.findByRole('heading', { level: 1, name: /Tasks/i })).toBeInTheDocument();
    expect(within(screen.getByRole('banner')).getByRole('link', { name: /^Home$/i })).toBeInTheDocument();
  });

  it('shows the branded loader while the signed-in session is restored', async () => {
    setAccessToken('restore-token');
    let resolveProfile!: (value: unknown) => void;
    const profile = new Promise((resolve) => { resolveProfile = resolve; });
    global.fetch = vi.fn().mockImplementation((url: string) =>
      url.includes('/api/profile')
        ? profile.then((body) => jsonResponse(body))
        : Promise.resolve(jsonResponse({ content: [], totalElements: 0, totalPages: 1 })),
    );
    render(<App />);
    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument();
    await act(async () => resolveProfile({ id: 1, firstName: 'Test', lastName: 'User', role: 'EMPLOYEE', status: 'ACTIVE' }));
  });

  it('shows Try again when the task request fails with no data', async () => {
    setAccessToken('test-token');
    global.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/profile')) return Promise.resolve(jsonResponse({ id: 1, firstName: 'Test', lastName: 'User', role: 'EMPLOYEE', status: 'ACTIVE' }));
      if (url.includes('/api/tasks/pool')) return Promise.reject(new Error('offline'));
      if (url.includes('/api/notifications')) return Promise.resolve(jsonResponse([]));
      return Promise.resolve(jsonResponse({ count: 0 }));
    });
    render(<App />);
    expect(await screen.findByText('Tasks could not be loaded.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
  });

  it('keeps task data visible when a background refresh fails', async () => {
    setAccessToken('test-token');
    let poolCalls = 0;
    global.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/profile')) return Promise.resolve(jsonResponse({ id: 1, firstName: 'Test', lastName: 'User', role: 'EMPLOYEE', status: 'ACTIVE' }));
      if (url.includes('/api/tasks/pool')) {
        poolCalls += 1;
        if (poolCalls > 1) return Promise.reject(new Error('refresh failed'));
        return Promise.resolve(jsonResponse({ content: [{ id: 1, title: 'Visible task', priority: 'LOW', status: 'TODO', deadline: '2026-11-04', progress: 0, members: [], subtasks: [], checkpoints: [] }], totalElements: 1, totalPages: 1 }));
      }
      if (url.includes('/api/notifications')) return Promise.resolve(jsonResponse([]));
      return Promise.resolve(jsonResponse({ count: 0 }));
    });
    render(<App />);
    expect(await screen.findByText('Visible task')).toBeInTheDocument();
    await act(async () => window.dispatchEvent(new Event('focus')));
    await waitFor(() => expect(screen.getByText('Visible task')).toBeInTheDocument());
    expect(await screen.findByText('Tasks could not be refreshed.')).toBeInTheDocument();
  });
});
