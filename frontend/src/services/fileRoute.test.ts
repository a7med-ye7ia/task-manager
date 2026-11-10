import { describe, expect, it } from 'vitest';
import { getTaskFileContentUrl } from './api';

describe('signed file route helper', () => {
  it('creates the content route with an encoded token', () => {
    expect(getTaskFileContentUrl(7, 9, 'a+b/c')).toContain('/api/tasks/7/files/9/content?token=a%2Bb%2Fc');
  });
});
