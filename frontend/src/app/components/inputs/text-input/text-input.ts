import { Component, input, model } from '@angular/core';

@Component({
  selector: 'app-text-input',
  template: `
    <label class="flex flex-col gap-1 text-sm font-medium text-slate-700" [attr.for]="id()">
      {{ label() }}
      <input
        class="w-full rounded-lg border border-blue-200 bg-white px-3 py-2 text-slate-900 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
        [id]="id()"
        [type]="type()"
        [autocomplete]="autocomplete()"
        [value]="value()"
        [attr.maxlength]="maxlength()"
        (input)="onInput($event)"
      />
    </label>
  `,
})
export class TextInput {
  readonly id = input.required<string>();
  readonly label = input.required<string>();
  readonly type = input('text');
  readonly autocomplete = input('off');
  readonly maxlength = input<number | null>(null);
  readonly value = model('');

  protected onInput(event: Event): void {
    this.value.set((event.target as HTMLInputElement).value);
  }
}
