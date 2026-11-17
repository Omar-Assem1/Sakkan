import { DOCUMENT, Injectable, computed, inject, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

export type AppLang = 'en' | 'ar';

export const SUPPORTED_LANGS: readonly AppLang[] = ['en', 'ar'];
export const DEFAULT_LANG: AppLang = 'en';
const LANG_KEY = 'sakan_lang';

/**
 * Single source of truth for the UI language. Keeps Transloco, <html dir>,
 * <html lang> and localStorage in sync so RTL flips with logical Tailwind classes.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly document = inject(DOCUMENT);
  private readonly transloco = inject(TranslocoService);

  private readonly lang = signal<AppLang>(this.readSavedLang());

  readonly currentLang = this.lang.asReadonly();
  readonly isRtl = computed(() => this.lang() === 'ar');

  constructor() {
    this.apply(this.lang());
  }

  setLanguage(lang: AppLang): void {
    const safe = isSupported(lang) ? lang : DEFAULT_LANG;
    this.lang.set(safe);
    this.apply(safe);
    try {
      localStorage.setItem(LANG_KEY, safe);
    } catch {
      /* storage unavailable – language still applied for this session */
    }
  }

  toggleLanguage(): void {
    this.setLanguage(this.lang() === 'en' ? 'ar' : 'en');
  }

  private apply(lang: AppLang): void {
    const html = this.document.documentElement;
    html.lang = lang;
    html.dir = lang === 'ar' ? 'rtl' : 'ltr';
    this.transloco.setActiveLang(lang);
  }

  private readSavedLang(): AppLang {
    try {
      const saved = localStorage.getItem(LANG_KEY);
      return isSupported(saved) ? saved : DEFAULT_LANG;
    } catch {
      return DEFAULT_LANG;
    }
  }
}

function isSupported(value: unknown): value is AppLang {
  return typeof value === 'string' && (SUPPORTED_LANGS as readonly string[]).includes(value);
}
