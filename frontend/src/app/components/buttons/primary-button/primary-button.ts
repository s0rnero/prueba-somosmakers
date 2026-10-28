import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-primary-button',
  template: `
    <button
      class="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
      [type]="type()"
      [disabled]="disabled() || loading()"
      (click)="clicked.emit()"
    >
      {{ loading() ? 'Procesando...' : label() }}
    </button>
  `,
})
export class PrimaryButton {
  readonly label = input.required<string>();
  readonly type = input<'button' | 'submit'>('submit');
  readonly disabled = input(false);
  readonly loading = input(false);
  readonly clicked = output<void>();
}
