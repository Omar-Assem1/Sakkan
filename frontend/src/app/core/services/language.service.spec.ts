import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/common';
import { TranslocoService } from '@jsverse/transloco';
import { LanguageService } from './language.service';

describe('LanguageService', () => {
  let service: LanguageService;
  let doc: Document;
  let translocoSpy: { setActiveLang: ReturnType<typeof vi.fn>; getActiveLang: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    translocoSpy = {
      setActiveLang: vi.fn(),
      getActiveLang: vi.fn().mockReturnValue('en'),
    };

    TestBed.configureTestingModule({
      providers: [
        LanguageService,
        { provide: TranslocoService, useValue: translocoSpy },
      ],
    });

    doc = TestBed.inject(DOCUMENT);
    doc.documentElement.dir = 'ltr';
    doc.documentElement.lang = 'en';
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should default to English (ltr, lang="en") when localStorage is empty', () => {
    service = TestBed.inject(LanguageService);

    expect(service.currentLang()).toBe('en');
    expect(service.isRtl()).toBe(false);
    expect(doc.documentElement.dir).toBe('ltr');
    expect(doc.documentElement.lang).toBe('en');
    expect(translocoSpy.setActiveLang).toHaveBeenCalledWith('en');
  });

  it('should switch to Arabic and update html dir="rtl", lang="ar", Transloco, and localStorage', () => {
    service = TestBed.inject(LanguageService);
    service.setLanguage('ar');

    expect(service.currentLang()).toBe('ar');
    expect(service.isRtl()).toBe(true);
    expect(doc.documentElement.dir).toBe('rtl');
    expect(doc.documentElement.lang).toBe('ar');
    expect(translocoSpy.setActiveLang).toHaveBeenCalledWith('ar');
    expect(localStorage.getItem('sakan_lang')).toBe('ar');
  });

  it('should toggle between en and ar via toggleLanguage()', () => {
    service = TestBed.inject(LanguageService);

    service.toggleLanguage();
    expect(service.currentLang()).toBe('ar');
    expect(doc.documentElement.dir).toBe('rtl');
    expect(doc.documentElement.lang).toBe('ar');

    service.toggleLanguage();
    expect(service.currentLang()).toBe('en');
    expect(doc.documentElement.dir).toBe('ltr');
    expect(doc.documentElement.lang).toBe('en');
  });

  it('should restore saved language ("ar") from localStorage on initialization', () => {
    localStorage.setItem('sakan_lang', 'ar');
    service = TestBed.inject(LanguageService);

    expect(service.currentLang()).toBe('ar');
    expect(service.isRtl()).toBe(true);
    expect(doc.documentElement.dir).toBe('rtl');
    expect(doc.documentElement.lang).toBe('ar');
    expect(translocoSpy.setActiveLang).toHaveBeenCalledWith('ar');
  });

  it('should fall back safely to "en" if localStorage contains corrupted/unsupported language value', () => {
    localStorage.setItem('sakan_lang', 'corrupted-value');
    service = TestBed.inject(LanguageService);

    expect(service.currentLang()).toBe('en');
    expect(service.isRtl()).toBe(false);
    expect(doc.documentElement.dir).toBe('ltr');
    expect(doc.documentElement.lang).toBe('en');
  });
});
