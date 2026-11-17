import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UiButtonComponent } from './ui-button.component';

describe('UiButtonComponent', () => {
  let component: UiButtonComponent;
  let fixture: ComponentFixture<UiButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UiButtonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UiButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and render button element', () => {
    expect(component).toBeTruthy();
    const btn = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(btn).toBeTruthy();
  });

  it('should emit clicked event when enabled and clicked', () => {
    const spy = vi.fn();
    component.clicked.subscribe(spy);

    const btn = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    btn.click();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should not emit clicked event when disabled', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const spy = vi.fn();
    component.clicked.subscribe(spy);

    const btn = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(btn.disabled).toBe(true);
    btn.click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should use logical Tailwind classes (ps-*, pe-*) and zero physical directional classes', () => {
    const btn = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    const classes = btn.className;

    expect(classes).toContain('ps-4');
    expect(classes).toContain('pe-4');
    expect(classes).not.toMatch(/\b(ml-|mr-|pl-|pr-|text-left|text-right|left-|right-|border-l|border-r)/);
  });
});
