import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

export type UiButtonVariant = 'primary' | 'secondary' | 'outline' | 'danger';

const BASE_CLASSES =
  'inline-flex cursor-pointer items-center justify-center gap-2 rounded-lg ps-4 pe-4 py-2 text-sm font-medium ' +
  'transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 ' +
  'disabled:cursor-not-allowed disabled:opacity-50';

const VARIANT_CLASSES: Record<UiButtonVariant, string> = {
  primary:
    'bg-emerald-600 text-white hover:bg-emerald-700 focus-visible:outline-emerald-600 dark:bg-emerald-600 dark:hover:bg-emerald-500',
  secondary:
    'bg-slate-100 text-slate-800 hover:bg-slate-200 focus-visible:outline-slate-400 dark:bg-slate-800 dark:text-slate-100 dark:hover:bg-slate-700',
  outline:
    'border border-slate-300 bg-white text-slate-800 hover:bg-slate-50 focus-visible:outline-slate-400 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800',
  danger:
    'bg-rose-600 text-white hover:bg-rose-700 focus-visible:outline-rose-600 dark:bg-rose-600 dark:hover:bg-rose-500',
};

/** Shared button: logical padding (ps/pe) only, so it mirrors correctly in RTL. Label via content projection. */
@Component({
  selector: 'app-ui-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      [attr.type]="type()"
      [class]="classes()"
      [disabled]="disabled()"
      [attr.aria-label]="ariaLabel() || null"
      (click)="onClick($event)"
    >
      <ng-content />
    </button>
  `,
})
export class UiButtonComponent {
  readonly variant = input<UiButtonVariant>('primary');
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly disabled = input<boolean>(false);
  readonly ariaLabel = input<string>('');

  readonly clicked = output<MouseEvent>();

  protected readonly classes = computed(() => `${BASE_CLASSES} ${VARIANT_CLASSES[this.variant()]}`);

  protected onClick(event: MouseEvent): void {
    if (this.disabled()) {
      return;
    }
    this.clicked.emit(event);
  }
}
