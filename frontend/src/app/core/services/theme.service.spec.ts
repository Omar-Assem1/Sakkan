import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    document.documentElement.style.colorScheme = '';
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    document.documentElement.style.colorScheme = '';
  });

  it('defaults to light mode when nothing is stored', () => {
    const service = TestBed.inject(ThemeService);
    const doc = TestBed.inject(DOCUMENT);

    expect(service.currentTheme()).toBe('light');
    expect(service.isDark()).toBe(false);
    expect(doc.documentElement.classList.contains('dark')).toBe(false);
  });

  it('toggles between light and dark mode and persists preference', () => {
    const service = TestBed.inject(ThemeService);
    const doc = TestBed.inject(DOCUMENT);

    service.toggleTheme();
    expect(service.currentTheme()).toBe('dark');
    expect(service.isDark()).toBe(true);
    expect(doc.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem('sakan_theme')).toBe('dark');

    service.toggleTheme();
    expect(service.currentTheme()).toBe('light');
    expect(service.isDark()).toBe(false);
    expect(doc.documentElement.classList.contains('dark')).toBe(false);
    expect(localStorage.getItem('sakan_theme')).toBe('light');
  });

  it('restores dark mode from localStorage on initialization', () => {
    localStorage.setItem('sakan_theme', 'dark');
    const service = TestBed.inject(ThemeService);
    const doc = TestBed.inject(DOCUMENT);

    expect(service.currentTheme()).toBe('dark');
    expect(service.isDark()).toBe(true);
    expect(doc.documentElement.classList.contains('dark')).toBe(true);
  });
});
