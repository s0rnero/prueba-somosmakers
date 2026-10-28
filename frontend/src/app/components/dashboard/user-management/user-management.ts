import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { User, UserRole } from '../../../models/user.model';
import { UserPayload, UserService } from '../../../services/user.service';

@Component({
  selector: 'app-user-management',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './user-management.html',
})
export class UserManagement {
  private readonly userService = inject(UserService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly users = signal<User[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly formOpen = signal(false);
  protected readonly editingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    name: this.formBuilder.nonNullable.control('', [
      Validators.required,
      Validators.minLength(2),
      Validators.maxLength(120),
    ]),
    email: this.formBuilder.nonNullable.control('', [
      Validators.required,
      Validators.email,
      Validators.maxLength(320),
    ]),
    role: this.formBuilder.nonNullable.control<UserRole>('USER'),
    password: this.formBuilder.nonNullable.control('', [Validators.minLength(8), Validators.maxLength(72)]),
  });

  constructor() {
    this.load();
  }

  protected startCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', email: '', role: 'USER', password: '' });
    this.error.set(null);
    this.success.set(null);
    this.formOpen.set(true);
  }

  protected startEdit(user: User): void {
    this.editingId.set(user.id);
    this.form.reset({ name: user.name, email: user.email, role: user.role, password: '' });
    this.error.set(null);
    this.success.set(null);
    this.formOpen.set(true);
  }

  protected cancel(): void {
    this.formOpen.set(false);
    this.editingId.set(null);
    this.form.reset({ name: '', email: '', role: 'USER', password: '' });
    this.error.set(null);
  }

  protected save(): void {
    if (this.saving()) return;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const password = raw.password.trim();
    const editingId = this.editingId();

    if (editingId === null && password.length === 0) {
      this.error.set('La contraseña es obligatoria para crear un usuario.');
      return;
    }

    const payload: UserPayload = {
      name: raw.name.trim(),
      email: raw.email.trim(),
      role: raw.role,
      ...(password ? { password } : {}),
    };

    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);

    const request = editingId === null ? this.userService.create(payload) : this.userService.update(editingId, payload);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.success.set(editingId === null ? 'Usuario creado.' : 'Usuario actualizado.');
        this.formOpen.set(false);
        this.editingId.set(null);
        this.form.reset({ name: '', email: '', role: 'USER', password: '' });
        this.load();
      },
      error: (response: unknown) => this.error.set(this.readError(response)),
    });
  }

  protected remove(user: User): void {
    if (!window.confirm(`¿Eliminar a ${user.name}?`)) return;

    this.error.set(null);
    this.success.set(null);
    this.userService.delete(user.id).subscribe({
      next: () => {
        this.success.set('Usuario eliminado.');
        if (this.editingId() === user.id) this.cancel();
        this.load();
      },
      error: (response: unknown) => this.error.set(this.readError(response)),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.userService
      .list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: users => this.users.set(users),
        error: (response: unknown) => this.error.set(this.readError(response)),
      });
  }

  private readError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as { message?: string } | null;
      if (body?.message) return body.message;
      if (error.status === 409) return 'El correo ya está registrado.';
      if (error.status === 403) return 'No tienes permisos para gestionar usuarios.';
      if (error.status === 0) return 'No se pudo conectar con el servidor.';
    }
    return 'Ocurrió un error. Intenta de nuevo.';
  }
}
