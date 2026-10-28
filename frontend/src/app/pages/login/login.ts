import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { PrimaryButton } from '../../components/buttons/primary-button/primary-button';
import { FormError } from '../../components/feedback/form-error/form-error';
import { TextInput } from '../../components/inputs/text-input/text-input';
import { AuthService } from '../../services/auth.service';

type AuthMode = 'login' | 'register';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'app-login',
  imports: [TextInput, PrimaryButton, FormError],
  templateUrl: './login.html',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly mode = signal<AuthMode>('login');
  protected readonly name = signal('');
  protected readonly email = signal('');
  protected readonly password = signal('');
  protected readonly confirmPassword = signal('');
  protected readonly isLoading = signal(false);
  protected readonly error = signal('');
  protected readonly isRegister = computed(() => this.mode() === 'register');

  protected toggleMode(): void {
    this.mode.set(this.isRegister() ? 'login' : 'register');
    this.confirmPassword.set('');
    this.error.set('');
  }

  protected async submit(event: Event): Promise<void> {
    event.preventDefault();
    if (this.isLoading() || !this.validate()) return;

    this.isLoading.set(true);
    this.error.set('');

    try {
      if (this.isRegister()) {
        await firstValueFrom(
          this.auth.register({
            name: this.name().trim(),
            email: this.email().trim(),
            password: this.password(),
          }),
        );
      } else {
        await firstValueFrom(
          this.auth.login({
            email: this.email().trim(),
            password: this.password(),
          }),
        );
      }

      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
      const target = returnUrl && returnUrl.startsWith('/') && returnUrl !== '/login'
        ? returnUrl
        : '/dashboard';
      await this.router.navigateByUrl(target);
    } catch (error: unknown) {
      this.error.set(this.readError(error));
    } finally {
      this.isLoading.set(false);
    }
  }

  private validate(): boolean {
    const email = this.email().trim();

    if (this.isRegister() && this.name().trim().length < 2) {
      this.error.set('El nombre debe tener al menos 2 caracteres.');
      return false;
    }
    if (!EMAIL_PATTERN.test(email)) {
      this.error.set('Ingresa un correo electronico valido.');
      return false;
    }
    if (this.password().length < 8) {
      this.error.set('La contraseña debe tener al menos 8 caracteres.');
      return false;
    }
    if (this.password().length > 72) {
      this.error.set('La contraseña no puede superar los 72 caracteres.');
      return false;
    }
    if (this.isRegister() && this.password() !== this.confirmPassword()) {
      this.error.set('Las contraseñas no coinciden.');
      return false;
    }

    return true;
  }

  private readError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as { message?: string } | null;
      if (body?.message) return body.message;
      if (error.status === 401) return 'Correo o contraseña incorrectos.';
      if (error.status === 409) return 'El correo ya está registrado.';
      if (error.status === 0) return 'No se pudo conectar con el servidor.';
    }
    return 'Ocurrió un error. Intenta de nuevo.';
  }
}
