import { Component, input } from '@angular/core';

@Component({
  selector: 'app-form-error',
  template: `
    @if (message()) {
      <p class="text-sm text-red-700" role="alert">{{ message() }}</p>
    }
  `,
})
export class FormError {
  readonly message = input('');
}
